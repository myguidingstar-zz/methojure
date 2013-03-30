(ns methojure.mongo.minimongo.modifier
  (:require [clojure.string :as cstr]
            [methojure.mongo.minimongo.selector :as selector]))

(defn- fill-up-sequence [map key]
  (if (and (sequential? map) (number? key) (> key (count map)))
    (apply vector (concat map (take (- key (count map)) (repeat nil))))
    map))

(defn ^:clj my-assoc
  "Extends the basic assoc function to add empty values to sequences.
           (my-assoc [] 0 10) => [10]
           (my-assoc [] 3 10) => [nil nil nil 10]"
  ([map key val]
     (. clojure.lang.RT (assoc (fill-up-sequence map key) key val)))
  ([map key val & kvs]
     (let [ret (assoc map key val)]
       (if kvs
         (recur ret (first kvs) (second kvs) (nnext kvs))
         ret))))

(defn ^:cljs my-assoc
  "Extends the basic assoc function to add empty values to sequences.
            (my-assoc [] 0 10) => [10]
            (my-assoc [] 3 10) => [nil nil nil 10]"
  ([map key val]
     (-assoc (fill-up-sequence map key) key val))
  ([map key val & kvs]
     (let [ret (assoc map key val)]
       (if kvs
         (recur ret (first kvs) (second kvs) (nnext kvs))
         ret))))

(defn ^:clj with-my-assoc [f] (with-redefs [assoc my-assoc] (f)))
(defn ^:cljs with-my-assoc [f] (binding [assoc my-assoc] (f)))


(defn make-access-seq
  "converts a dotted path (like a.b.1.d) to a clojure
   sequence that can be use in assoc-in and update-in calls."
  [doc path]
  (let [keyword-or-empty-str #(if (= "" (.trim %)) % (keyword %))
        keyparts (->> (cstr/split (name path) #"[.]")
                      (map keyword-or-empty-str)
                      (apply vector))]
    (map-indexed (fn [i k]
                   (let [v (get-in doc (subvec keyparts 0 i))]
                     (cond
                      (= (name k) ":") ""
                      (and (not (nil? v))
                           (sequential? v)
                           (not (nil? (re-matches #"\d+" (name k)))))
                      ,(selector/parse-int (name k))
                      :else k)))
                 keyparts)))

(defn $set
  "The MongoDB $set modifier"
  [doc keypath arg]
  (assoc-in doc keypath arg))

(defn $inc
  "The MongoDB $inc modifier"
  [doc keypath arg]
  (update-in doc keypath #(+ (or % 0) arg)))

(defn $unset
  "The MongoDB $unset modifier"
  [doc keypath arg]
  (let [begin (drop-last keypath)
        v (get-in doc begin)
        k (last keypath)]
    (cond
     (nil? v) doc
     (and (sequential? v) (number? k)) (assoc-in doc keypath nil)
     (sequential? v) doc
     (empty? begin) (dissoc doc k)
     :else (update-in doc begin dissoc k))))

(defn $push
  "The MongoDB $push modifier"
  [doc keypath arg]
  (update-in doc keypath conj arg))

(defn $pushAll
  "The MongoDB $pushAll modifier"
  [doc keypath arg]
  (update-in doc keypath concat arg))

(defn $addToSet
  "The MongoDB $addToSet modifier"
  [doc keypath arg]
  (let [v (cond
           (and (map? arg) (contains? arg :$each)) (:$each arg)
           (sequential? arg) arg
           :else [arg])]
    (update-in doc keypath #(distinct (concat % v)))))

(defn $pop
  "The MongoDB $pop modifier"
  [doc keypath arg]
  (let [v (get-in doc keypath)]
    (cond
     (nil? v) doc
     (and (number? arg) (neg? arg)) (update-in doc keypath rest)
     :else (update-in doc keypath drop-last))))

(defn $pull
  "The MongoDB $pull modifier"
  [doc keypath arg]
  (cond
   (nil? (get-in doc keypath)) doc
   (map? arg) (let [sel (selector/compile-selector arg)]
                (update-in doc keypath
                           #(remove sel %)))
   :else (update-in doc keypath #(remove (fn [x] (= x arg)) %))))

(defn $pullAll
  "The MongoDB $pullAll modifier"
  [doc keypath arg]
  (cond
   (nil? (get-in doc keypath)) doc
   :else (update-in doc keypath #(remove (fn [x] (contains? (set arg) x)) %))))

(defn $rename
  "The MongoDB $rename modifier"
  [doc keypath arg]
  (if (nil? (get-in doc keypath))
    doc
    (let [p (make-access-seq doc arg)
          v (get-in doc keypath)
          doc (if (= (count keypath) 1)
                (dissoc doc (first keypath))
                (update-in doc (drop-last keypath)
                           dissoc (last keypath)))]
      (assoc-in doc p v))))

(def modifiers {:$set $set :$inc $inc :$unset $unset :$push $push
                :$pushAll $pushAll :$addToSet $addToSet :$pop $pop :$pull $pull
                :$pullAll $pullAll :$rename $rename})

(defn modify
  "modifies a document by a given MongoDB modifier."
  [doc modifier]
  (let [modifier? (->> modifier
                       (map (fn [[k v]] (subs (name k) 0 1)))
                       (filter #(= "$" %))
                       (empty?)
                       (not))]
    (if-not modifier?
      (let [new-doc (if (contains? modifier :_id)
                      (assoc modifier :_id (:_id modifier))
                      modifier)]
        new-doc)
      (with-my-assoc
        (fn []
          (reduce
           (fn [doc [op v]]
             (let [mod-fn (op modifiers)]
               (reduce (fn [doc [path arg]]
                         (mod-fn doc (make-access-seq doc path) arg))
                       doc v)))
           doc
           modifier))))))
