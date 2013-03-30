(ns methojure.mongo.minimongo.collection
  (:refer-clojure :exclude [find remove count])
  (:require [methojure.mongo.minimongo.selector :as ms]
            [methojure.mongo.minimongo.modifier :as mm]
            [methojure.mongo.minimongo.diff :as md]))

(defprotocol IMongoCursor
  (fetch [this])
  (count [this]))

(defprotocol IMongoCollection
  (find [this] [this selector] [this selector options])
  (find-one [this] [this selector] [this selector options])
  (insert [this doc])
  (remove [this selector])
  (update [this selector mod] [this selector mod options]))

(defn- query [selector docs options]
  (let [a (filter selector (vals @docs))
        b (if (number? (:skip options))
            (drop (:skip options) a)
            a)
        c (if (number? (:limit options))
            (take (:limit options) b)
            b)
        d (if (contains? options :sort)
            (sort (ms/compile-sort (:sort options)) c)
            c)]
    d))

(deftype ^:clj LocalCursor [selector docs options watcher]
        IMongoCursor
        (fetch [this] (query selector docs options))
        (count [this] (clojure.core/count (fetch this)))

        clojure.lang.IDeref
        (deref [this] (fetch this))
        
        clojure.lang.IRef
        (setValidator [this vf] nil)
        (getValidator [this] nil)
        (getWatches [a] @watcher)
        (addWatch [this k f] (do (swap! watcher assoc k f) this))
        (removeWatch [this k] (do (swap! watcher dissoc k) this)))

(defrecord ^:cljs LocalCursor [selector docs options watcher]
         IMongoCursor
         (fetch [this] (query selector docs options))
         (count [this] (clojure.core/count (fetch this)))

         IDeref
         (deref [this] (fetch this))
         
         IWatchable
         (-notify-watches [this old new] this)
         (-add-watch [this k f] (do (swap! watcher assoc k f) this))
         (-remove-watch [this k] (do (swap! watcher dissoc k) this)))

(defn with-query-notification [queries f]
  (let [data (map #(hash-map :query-id %) (keys @queries))
        data (doall
              (map #(assoc % :before (fetch (@queries (:query-id %)))) data))
        res (f) ;; <-- side effect
        data (doall
              (map #(assoc % :after (fetch (@queries (:query-id %)))) data))]
    (doseq [entry data
            :let [diff (md/diff (:before entry) (:after entry))]]
      (doseq [[_ f] @(.-watcher (@queries (:query-id entry)))]
        (f diff)))
    res))

(deftype LocalCollection [docs queries next-qid]
  IMongoCollection
  (find [this] (find this {}))
  (find [this selector] (find this selector {}))
  (find [this selector options]
    (let [cursor (->LocalCursor (ms/compile-selector selector)
                                docs options (atom {}))
          id (swap! next-qid inc)]
      (when-not (= false (:reactive options))
        (swap! queries assoc id cursor))
      cursor))

  (find-one [this] (find-one this {}))
  (find-one [this selector] (find-one this selector {}))
  (find-one [this selector options]
    (-> (find this selector (assoc options :reactive false)) (fetch) (first)))

  (insert [this doc]
    (let [doc (if-not (contains? doc :_id)
                (assoc doc :_id (swap! next-qid inc))
                doc)]
      (with-query-notification queries
        (fn [] (swap! docs assoc (:_id doc) doc)))))

  (remove [this selector]
    (let [selector-fn (ms/compile-selector selector)]
      (with-query-notification queries
        (fn []
          (swap! docs (fn [m] (->> m
                                   (filter #(not (selector-fn (second %))))
                                   (into {}))))))))

  (update [this selector mod] (update this selector mod {}))
  (update [this selector mod {:keys [multi] :or {multi false} :as options}]
    ;; TODO: implement multi option
    (let [selector-fn (ms/compile-selector selector)]
      (with-query-notification queries
        (fn []
          (swap! docs (fn [m]
                        (->> m
                             (map (fn [[id doc]]
                                    (if (selector-fn doc)
                                      [id (-> doc
                                              (mm/modify mod)
                                              (assoc :_id id))]
                                      [id doc])))
                             (into {})))))))))


(defn ^:clj local-collection []
  (->LocalCollection (atom {}) (atom {}) (atom 1)))

(defn ^:cljs local-collection []
  (LocalCollection. (atom {}) (atom {}) (atom 1)))
