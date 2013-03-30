(ns methojure.mongo.minimongo.selector
  (:require [clojure.string :as cstr]))

(defn any? [f x] (not (not (some f x))))

(defn some-if-seq [x f] (if (sequential? x) (any? f x) (f x)))

(defn some-if-seq-plus [x f] (if (f x) true (and (sequential? x) (any? f x))))

(defn mongo-cmp
  "compares to values as mongo db would do."
  [a b]
  (cond
   (nil? a) (if (nil? b) 0 -1)
   (nil? b) 1
   ;; TODO: ObjectID
   ;; TODO: Date
   (map? a) (mongo-cmp (apply concat (seq a)) (apply concat (seq b)))
   (sequential? a) (->> (map vector a b)
                        (map #(mongo-cmp (first %) (second %)))
                        (drop-while #(= % 0))
                        ((fn [x] (if (empty? x)
                                   (cond
                                    (= (count a) (count b)) 0
                                    (> (count a) (count b)) 1
                                    (< (count a) (count b)) -1)
                                   (first x)))))
   :else (compare a b)))

(defn ^:clj boolean? [v] (= (str (type v)) "class java.lang.Boolean"))
(defn ^:cljs boolean? [v] (= (type v) js/Boolean))

(defn ^:clj pattern? [v] (= (str (type v)) "class java.util.regex.Pattern"))
(defn ^:cljs pattern? [v] (= (type v) js/RegExp))

(defn ^:clj parse-int [v] (Integer/valueOf v))
(defn ^:cljs parse-int [v] (js/parseInt v))

(defn mongo-type
  "returns the mongo db type of the give value"
  [v]
  (cond
   (number? v) 1
   (string? v) 2
   (boolean? v) 8
   (sequential? v) 4
   (nil? v) 10
   (pattern? v) 11
   (fn? v) 13
   ;; TODO: Date
   ;; TODO: Binary
   ;; TODO: ObjectId
   :else 3))

(defn mongo-pattern
  "converts a javascript regex to a java pattern.
   e.g (mongo-pattern \"/a/\" \"i\") => \"(?i)a\""
  [s options]
  (if-not (= \/ (first s))
    (let [options (if options (str "(?" options ")") "")]
      (re-pattern (str options (cstr/replace s "\\" "\\\\"))))
    (let [idx (.lastIndexOf s "/")
          regex (subs s 1 idx)
          options (or options (subs s (inc idx)))
          options (if (> (count options) 0)
                    (str "(?" options ")")
                    "")]
      (re-pattern (str options (cstr/replace regex "\\" "\\\\"))))))

(declare compile-doc-selector)
(declare compile-value-selector)

(defn $and
  "The MongoDB $and operator."
  [selector]
  (let [fns (map compile-doc-selector selector)]
    (fn [doc] (every? #(% doc) fns))))

(defn $or
  "The MongoDB $or operator."
  [selector]
  (let [fns (map compile-doc-selector selector)]
    (fn [doc] (any? #(% doc) fns))))

(defn $nor
  "The MongoDB $nor operator."
  [selector]
  (let [fns (map compile-doc-selector selector)]
    (fn [doc] (every? #(not (% doc)) fns))))

(defn $where
  "The MongoDB $where operator."
  [selector]
  (let [sel (if (fn? selector) selector (fn [doc] selector))]
    (fn [doc] (sel doc))))

(defn $in
  "The MongoDB $in selector"
  [operand]
  (fn [value]
    (some-if-seq-plus value (fn [x] (any? #(= % x) operand)))))

(defn $all
  "The MongoDB $all selector"
  [operand]
  (fn [value]
    (if-not (sequential? value)
      false
      (every? (fn [op] (any? #(= op %) value)) operand))))

(defn $lt
  "The MongoDB $lt selector"
  [operand]
  (fn [value] (some-if-seq value #(< (mongo-cmp % operand) 0))))

(defn $lte
  "The MongoDB $lte selector"
  [operand]
  (fn [value] (some-if-seq value #(<= (mongo-cmp % operand) 0))))

(defn $gt
  "The MongoDB $gt selector"
  [operand]
  (fn [value] (some-if-seq value #(> (mongo-cmp % operand) 0))))

(defn $gte
  "The MongoDB $gte selector"
  [operand]
  (fn [value] (some-if-seq value #(>= (mongo-cmp % operand) 0))))

(defn $ne
  "The MongoDB $ne selector"
  [operand]
  (fn [value] (not (some-if-seq-plus value #(= % operand)))))

(defn $nin
  "The MongoDB $nin selector"
  [operand]
  (let [f ($in operand)]
    (fn [value] (if (nil? value) true (not (f value))))))

(defn $exists
  "The MongoDB $exists selector"
  [operand]
  (fn [value] (= (not (nil? value)) operand)))

(defn $mod
  "The MongoDB $mod selector"
  [[div rem]]
  (fn [value]
    (if (nil? value) false (some-if-seq value #(= (mod % div) rem)))))

(defn $size
  "The MongoDB $size selector"
  [operand]
  (fn [value] (and (sequential? value) (= operand (count value)))))

(defn $type
  "The MongoDB $type selector"
  [operand]
  (fn [value] (some-if-seq value #(= (mongo-type %) operand))))

(defn $regex
  "The MongoDB $regex selector"
  [operand options]
  (fn [value]
    (if (nil? value)
      false
      (some-if-seq value
                   #(not (nil? (re-find (mongo-pattern operand options) %)))))))

(defn $options
  "The MongoDB $options param.
   This is not really needed but in case if appears simple return a true fn."
  [operand] (fn [value] true))

(defn $elemMatch
  "The MongoDB $elemMatch selector"
  [operand]
  (let [matcher (compile-doc-selector operand)]
    (fn [value]
      (if-not (sequential? value)
        false
        (any? #(matcher %) value)))))

(defn $not
  "The MongoDB $not selector"
  [operand]
  (let [matcher (compile-value-selector operand)]
    (fn [value] (not (matcher value)))))

(def logical-ops {:$and $and :$or $or :$nor $nor :$where $where})

(def value-ops {:$in $in :$all $all :$lt $lt :$lte $lte :$gt $gt :$gte $gte
                :$ne $ne :$nin $nin :$exists $exists :$mod $mod :$size $size
                :$type $type :$regex $regex :$options $options
                :$elemMatch $elemMatch :$not $not})

(defn id? [selector]
  (or (string? selector) (number? selector)))

(defn make-lookup-fn [k]
  (let [k (name k)
        idx (.indexOf k ".")
        first-lookup (if (>= idx 0) (subs k 0 idx) k)
        first-lookup (if-not (nil? (re-matches #"\d+" first-lookup))
                       (parse-int first-lookup)
                       (keyword first-lookup))
        rest-k (subs k (inc idx))
        rest-lookup (when (>= idx 0) (make-lookup-fn rest-k))
        next-is-numeric? (not (nil? (re-find #"^\d+" rest-k)))]
    (fn [doc]
      (cond
       (nil? doc) [nil]
       (not (or (map? doc) (sequential? doc))) [nil]
       (nil? rest-lookup) [(get-in doc [first-lookup])]
       (and (sequential? (get-in doc [first-lookup]))
            (empty? (get-in doc [first-lookup]))) [nil]
       :else (let [f (get-in doc [first-lookup])
                   f (if (or (not (sequential? f)) next-is-numeric?) [f] f)]
               (apply concat [] (map rest-lookup f)))))))

(defn has-operators [value-sel]
  (->> (keys value-sel)
       (map #(subs (name %) 0 1))
       ((fn [xs] (and (> (count xs) 0) (every? #(= % "$") xs))))))

(defn compile-value-selector [value-sel]
  (cond
   (nil? value-sel) (fn [v] (some-if-seq v nil?))
   (sequential? value-sel) (fn [v] (if (sequential? v)
                                     (some-if-seq-plus v #(= % value-sel))
                                     false))
   (and (map? value-sel) (has-operators value-sel))
   ,(fn [v]
      (every?
       #(% v)
       (map (fn [[k v]]
              (if (= k :$regex)
                ((k value-ops) v (:$options value-sel))
                ((k value-ops) v)))
            value-sel)))
   :else (fn [v] (some-if-seq v #(= % value-sel)))
   ))

(defn- compile-doc-selector [selector]
  (fn [doc]
    (->> selector
         (map (fn [[k v]]
                (if (= "$" (subs (name k) 0 1))
                  ((k logical-ops) v)
                  (let [lookup-fn (make-lookup-fn k)
                        selector-fn (compile-value-selector v)]
                    (fn [doc] (any? selector-fn (lookup-fn doc)))))))
         (every? #(% doc)))))

(defn compile-selector [selector]
  (cond
   (fn? selector) selector
   (nil? selector) (fn [doc] false)
   (id? selector) (fn [doc] (= selector (:_id doc)))
   (and (map? selector)
        (contains? selector :_id)
        (not (:_id selector))) (fn [doc] false)
   (map? selector) (compile-doc-selector selector)
   :else (fn [doc] false) ;; TODO: throw exception
   ))

(defn match [selector doc]
  ((compile-selector selector) doc))

(defn reduce-sort [branch-values find-min]
  (let [branch-values (map #(if (sequential? %) % [%]) branch-values)]
    (reduce
     (fn [reduced v]
       (let [c (mongo-cmp reduced v)]
         (if (or (and find-min (> c 0))
                 (and (not find-min) (< c 0)))
           v
           reduced)))
     (first branch-values)
     (rest branch-values))))

(defn cmp-parts [a b]
  (fn [{:keys [lookup ascending]}]
    (let [a-value (reduce-sort (lookup a) ascending)
          b-value (reduce-sort (lookup b) ascending)
          c (mongo-cmp a-value b-value)]
      (if ascending c (- c)))))

(defn compile-sort [spec]
  (let [parts (map (fn [x]
                     (cond
                      (sequential? x) (let [[k v] x]
                                        {:lookup (make-lookup-fn k)
                                         :ascending (cond
                                                     (number? v) (> v 0)
                                                     (= v :desc) false
                                                     (= v :asc) true
                                                     :else true)})
                      :else {:lookup (make-lookup-fn x)
                             :ascending true}))
                   spec)]
    (if (empty? parts)
      (fn [a b] 0)
      (fn [a b]
        (or (->> parts
                 (map (cmp-parts a b))
                 (remove zero?)
                 (first))
            0)))))
