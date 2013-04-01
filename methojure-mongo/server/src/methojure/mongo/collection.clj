(ns methojure.mongo.collection
  (:refer-clojure :exclude [find remove count])
  (:use [methojure.mongo.api]
        [methojure.mongo.minimongo.collection :only [with-query-notification]])
  (:require [monger.collection :as mc]
            [monger.query :as mq]))

(deftype Cursor [coll-name selector options watcher]
  IMongoCursor
  (fetch [this]
    (if (nil? selector)
      []
      (mq/with-collection coll-name
        (merge options {:query selector}))))
  (count [this]
    (clojure.core/count (fetch this)))

  clojure.lang.IDeref
  (deref [this] (fetch this))
  
  clojure.lang.IRef
  (setValidator [this vf] nil)
  (getValidator [this] nil)
  (getWatches [a] @watcher)
  (addWatch [this k f] (do (swap! watcher assoc k f) this))
  (removeWatch [this k] (do (swap! watcher dissoc k) this)))

(defn fix-sort [selector]
  (update-in selector [:sort]
             (fn [xs] (->> xs
                           (map (fn [[k v]]
                                  [(keyword k) (condp = v
                                                 :desc -1
                                                 :asc 1
                                                 "desc" -1
                                                 "asc" 1
                                                 v)]))
                           (apply concat)
                           (apply array-map)
                           ))))

(comment
  (array-map :score -1 :name 1)
  (sorted-map :type 1 :name 1)
  (fix-sort {:sort [[:type 1] [:name 1]]})
  )

(defn fix-id [selector]
  (if (or (string? selector) (number? selector))
    {:_id selector}
    selector))

(deftype Collection [name queries next-qid opts]
  IMongoCollection
  (find [this] (find this {}))
  (find [this selector] (find this selector {}))
  (find [this selector options]
    (let [selector (fix-id selector)
          options (fix-sort options)
          cursor (Cursor. name selector options (atom {}))
          id (swap! next-qid inc)]
      (when-not (= false (:reactive options))
        (swap! queries assoc id cursor))
      cursor))
  
  (find-one [this] (find-one this {}))
  (find-one [this selector] (find-one this selector {}))
  (find-one [this selector options]
    (when-not (nil? selector)
      (let [selector (fix-id selector)
            options (fix-sort options)]
        (mc/find-one-as-map name selector))))
  
  (insert [this doc]
    (with-query-notification queries
      (fn [] (mc/insert-and-return name doc))))
  
  (remove [this selector]
    (with-query-notification queries
      (fn [] (when (map? selector)
               (mc/remove name selector)))))
  
  (update [this selector mod] (update this selector mod {}))
  (update [this selector mod options]
    (with-query-notification queries
      (fn [] (apply mc/update name selector mod
                    (apply concat (vec options)))))))

(defn create-collection
  ([name] (create-collection name {}))
  ([name options]
     (->Collection name (atom {}) (atom 0) options)))
