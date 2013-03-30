^:clj (ns methojure.mongo.test.minimongo.selector
        (:use clojure.test)
        (:require [methojure.mongo.minimongo.selector :as m]))

^:cljs (ns methojure.mongo.test.minimongo.selector
         (:require-macros
          [methojure.test.macros :refer [is deftest are]])
         (:require [methojure.test.core :as j]
                   [methojure.mongo.minimongo.selector :as m]))

;; Tests are adopted from the meteor.js minimongo package.

(deftest empty-selector
  (are [x y] (true? (m/match x y))
       {} {}
       {} {:a 12}))

(deftest scalar
  (are [x y match?] (= match? (m/match x y))
       1 {:_id 1 :a "foo"} true
       1 {:_id 2 :a "foo"} false
       "a" {:_id "a" :a "foo"} true
       "a" {:_id "b" :a "foo"} false))

(deftest safety
  (are [x y] (= false (m/match x y))
       nil {}
       nil {:_id "foo"}
       false {:_id "foo"}
       {:_id nil} {:_id "foo"}
       {:_id false} {:_id "foo"}))

(deftest one-or-more-keys
  (are [x y match?] (= match? (m/match x y))
       {:a 12} {} false
       {:a 12} {:a 12} true
       {:a 12} {:a 12 :b 13} true
       {:a 12 :b 13} {:a 12 :b 13} true
       {:a 12 :b 13} {:a 12 :b 13 :c 14} true
       {:a 12 :b 13 :c 14} {:a 12 :b 13} false
       {:a 12 :b 13} {:b 13 :c 14} false
       {:a 12} {:a [12]} true
       {:a 12} {:a [11 12 13]} true
       {:a 12} {:a [11 13]} false
       {:a 12 :b 13} {:a [11 12 13] :b [13 14 15]} true
       {:a 12 :b 13} {:a [11 12 13] :b [14 15]} false))

(deftest arrays
  (are [x y match?] (= match? (m/match x y))
       {:a [1 2]} {:a [1 2]} true
       {:a [1 2]} {:a [[1 2]]} true
       {:a [1 2]} {:a [[3 4] [1 2]]} true
       {:a [1 2]} {:a [[3 4]]} false
       {:a [1 2]} {:a [[[3 4]]]} false))

(deftest literal-documents
  (are [x y match?] (= match? (m/match x y))
       {:a {:b 12}} {:a {:b 12}} true
       {:a {:b 12 :c 13}} {:a {:b 12}} false
       {:a {:b 12}} {:a {:b 12 :c 13}} false
       {:a {:b 12 :c 13}} {:a {:b 12 :c 13}} true
       ;;{:a {:b 12 :c 13}} {:a {:c 13 :b 12}} false
       {:a {}} {:a {:b 12}} false
       {:a {:b 12}} {:a {}} false
       {:a {:b 12 :c [13 true false 2.2 "a" nil {:d 14}]}}
       ,{:a {:b 12 :c [13 true false 2.2 "a" nil {:d 14}]}} true
       {:a {:b 12}} {:a {:b 12} :k 99} true
       {:a {:b 12}} {:a [{:b 12}]} true
       {:a {:b 12}} {:a [[{:b 12}]]} false
       {:a {:b 12}} {:a [{:b 11} {:b 12} {:b 13}]} true
       {:a {:b 12}} {:a [{:b 11} {:b 12 :c 20} {:b 13}]} false
       {:a {:b 12 :c 20}} {:a [{:b 11} {:b 12} {:c 20}]} false
       {:a {:b 12 :c 20}} {:a [{:b 11} {:b 12 :c 20} {:b 13}]} true))

(deftest nil-test
  (are [x y match?] (= match? (m/match x y))
       {:a nil} {:a nil} true
       {:a nil} {:b 12} true
       {:a nil} {:a 12} false
       {:a nil} {:a [1 2 nil 3]} true
       {:a nil} {:a [1 2 {} 3]} false))

(deftest lt-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$lt 10}} {:a 9} true
       {:a {:$lt 10}} {:a 10} false
       {:a {:$lt 10}} {:a 11} false
       {:a {:$lt 10}} {:a [11 9 12]} true
       {:a {:$lt 10}} {:a [11 12]} false
       {:a {:$lt "null"}} {:a nil} true
       {:a {:$lt {:x [2 3 4]}}} {:a {:x [1 3 4]}} true
       {:a {:$lt {:x [2 3 4]}}} {:a {:x [2 3 4]}} false))

(deftest gt-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$gt 10}} {:a 11} true
       {:a {:$gt 10}} {:a 10} false
       {:a {:$gt 10}} {:a 9} false
       {:a {:$gt {:x [2 3 4]}}} {:a {:x [3 3 4]}} true
       {:a {:$gt {:x [2 3 4]}}} {:a {:x [1 3 4]}} false
       {:a {:$gt {:x [2 3 4]}}} {:a {:x [2 3 4]}} false))

(deftest lte-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$lte 10}} {:a 9} true
       {:a {:$lte 10}} {:a 10} true
       {:a {:$lte 10}} {:a 11} false
       {:a {:$lte {:x [2 3 4]}}} {:a {:x [2 3 4]}} true))

(deftest gte-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$gte 10}} {:a 11} true
       {:a {:$gte 10}} {:a 10} true
       {:a {:$gte 10}} {:a 9} false
       {:a {:$gte {:x [2 3 4]}}} {:a {:x [2 3 4]}} true))

(deftest composing-ops
  (are [x y match?] (= match? (m/match x y))
       {:a {:$lt 11 :$gt 9}} {:a 8} false
       {:a {:$lt 11 :$gt 9}} {:a 9} false
       {:a {:$lt 11 :$gt 9}} {:a 10} true
       {:a {:$lt 11 :$gt 9}} {:a 11} false
       {:a {:$lt 11 :$gt 9}} {:a 12} false
       {:a {:$lt 11 :$gt 9}} {:a [8 9 10 11 12]} true
       {:a {:$lt 11 :$gt 9}} {:a [8 9 11 12]} true))

(deftest all-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$all [1 2]}} {:a [1 2]} true
       {:a {:$all [1 2 3]}} {:a [1 2]} false
       {:a {:$all [1 2]}} {:a [3 2 1]} true
       {:a {:$all [1 "x"]}} {:a [3 "x" 1]} true
       {:a {:$all ["2"]}} {:a 2} false
       {:a {:$all [2]}} {:a "2"} false
       {:a {:$all [[1 2] [1 3]]}} {:a [[1 3] [1 2] [1 4]]} true
       {:a {:$all [[1 2] [1 3]]}} {:a [[1 4] [1 2] [1 4]]} false
       {:a {:$all [2 2]}} {:a [2]} true
       {:a {:$all [2 3]}} {:a [2 2]} false
       {:a {:$all [1 2]}} {:a [[1 1]]} false
       {:a {:$all [1 2]}} {} false
       {:a {:$all [1 2]}} {:a {:foo "bar"}} false))

(deftest exist-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$exists true}} {:a 12} true
       {:a {:$exists true}} {:b 12} false
       {:a {:$exists false}} {:a 12} false
       {:a {:$exists false}} {:b 12} true
       {:a {:$exists true}} {:a []} true
       {:a {:$exists true}} {:b []} false
       {:a {:$exists false}} {:a []} false
       {:a {:$exists false}} {:b []} true
       {:a {:$exists true}} {:a [1]} true
       {:a {:$exists true}} {:b [1]} false
       {:a {:$exists false}} {:a [1]} false
       {:a {:$exists false}} {:b [1]} true))

(deftest mod-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$mod [10 1]}} {:a 11} true
       {:a {:$mod [10 1]}} {:a 12} false
       {:a {:$mod [10 1]}} {:a [10 11 12]} true
       {:a {:$mod [10 1]}} {:a [10 12]} false))

(deftest ne-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$ne 1}} {:a 2} true
       {:a {:$ne 2}} {:a 2} false
       {:a {:$ne [1]}} {:a [2]} true
       {:a {:$ne [1 2]}} {:a [1 2]} false
       {:a {:$ne 1}} {:a [1 2]} false
       {:a {:$ne 2}} {:a [1 2]} false
       {:a {:$ne 3}} {:a [1 2]} true
       {:a {:$ne {:x 1}}} {:a {:x 1}} false
       {:a {:$ne {:x 1}}} {:a {:x 2}} true
       {:a {:$ne {:x 1}}} {:a {:x 1 :y 2}} true))

(deftest in-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$in [1 2 3]}} {:a 2} true
       {:a {:$in [1 2 3]}} {:a 4} false
       {:a {:$in [[1] [2] [3]]}} {:a [2]} true
       {:a {:$in [[1] [2] [3]]}} {:a [4]} false
       {:a {:$in [{:b 1} {:b 2} {:b 3}]}} {:a {:b 2}} true
       {:a {:$in [{:b 1} {:b 2} {:b 3}]}} {:a {:b 4}} false
       {:a {:$in [1 2 3]}} {:a [2]} true
       {:a {:$in [{:x 1} {:x 2} {:x 3}]}} {:a [{:x 2}]} true
       {:a {:$in [1 2 3]}} {:a [4 2]} true
       {:a {:$in [1 2 3]}} {:a [4]} false))

(deftest nin-test
  (are [x y match?] (= match? (m/match x y))
       {:a {:$nin [1 2 3]}} {:a 2} false
       {:a {:$nin [1 2 3]}} {:a 4} true
       {:a {:$nin [[1] [2] [3]]}} {:a [2]} false
       {:a {:$nin [[1] [2] [3]]}} {:a [4]} true
       {:a {:$nin [{:b 1} {:b 2} {:b 3}]}} {:a {:b 2}} false
       {:a {:$nin [{:b 1} {:b 2} {:b 3}]}} {:a {:b 4}} true
       {:a {:$nin [1 2 3]}} {:a [2]} false
       {:a {:$nin [{:x 1} {:x 2} {:x 3}]}} {:a [{:x 2}]} false
       {:a {:$nin [1 2 3]}} {:a [4 2]} false
       {:a {:$nin [1 2 3]}} {:a [4]} true))

(deftest size-test
  (are [x y match?] (= match? (m/match {:a {:$size x}} {:a y}))
       0 [] true
       1 [2] true
       2 [2 2] true
       0 [2] false
       1 [] false
       1 [2 2] false
       0 "2" false
       1 "2" false
       2 "2" false
       2 [[2 2]] false))

(deftest type-test
  (are [x y match?] (= match? (m/match {:a {:$type x}} {:a y}))
       1 1.1 true
       1 1 true
       1 "1" false
       1 [1] true ;; runs in mongo
       1 ["1" 1] true
       1 ["1" []] false
       2 "1" true
       2 1 false
       2 [1] false
       2 ["1" 1] true
       2 ["1" []] true
       3 {} true
       3 {:b 2} true
       3 [] false
       3 [1] false
       3 nil false
       3 ["1" 1] false
       4 [] false
       4 [1] false
       4 ["1" 1] false
       4 ["1" []] true
       ;; TODO: Binary
       ;; TODO: Object ID
       8 true true
       8 false true
       8 "true" false
       8 0 false
       8 nil false
       8 "" false
       8 {} false
       ;; TODO: Date
       10 nil true
       10 false false
       10 "" false
       10 0 false
       10 {} false
       11 "x" false
       11 {} false))

(deftest regex-test
  (are [x y match?] (= match? (m/match {:a x} {:a y}))
       {:$regex "a"} ["foo" "bar"] true
       {:$regex ","} ["foo" "bar"] false
       {:$regex "a"} ["foo" "bar"] true
       {:$regex ","} ["foo" "bar"] false
       {:$regex "/a/"} "cat" true
       {:$regex "/a/"} "cut" false
       {:$regex "/a/"} "CAT" false
       {:$regex "/a/i"} "CAT" true
       {:$regex "/a/" :$options "i"} "CAT" true
       {:$regex "/a/i" :$options "i"} "CAT" true
       {:$regex "/a/i" :$options ""} "CAT" false
       {:$regex "a"} "cat" true
       {:$regex "a"} "cut" false
       {:$regex "a"} "CAT" false
       {:$regex "a" :$options "i"} "CAT" true
       {:$options "i"} 12 true
       {:$regex "a"} ["dog" "cat"] true
       {:$regex "a"} ["dog" "puppy"] false
       ))

(deftest not-test
  (are [x y match?] (= match? (m/match {:x {:$not x}} {:x y}))
       {:$gt 7} 6 true
       {:$gt 7} 8 false
       {:$lt 10 :$gt 7} 11 true
       {:$lt 10 :$gt 7} 9 false
       {:$lt 10 :$gt 7} 6 true
       {:$gt 7} [2 3 4] true
       {:$gt 7} [2 3 4 10] false
       {:$regex "/a/"} "dog" true
       {:$regex "/a/"} "cat" false
       {:$regex "/a/"} ["dog" "puppy"] true
       {:$regex "/a/"} ["kitten" "cat"] false))

(deftest dotted-keypath
  (are [x y match?] (= match? (m/match x y))
       {:a.b 1} {:a {:b 1}} true
       {:a.b 1} {:a {:b 2}} false
       {:a.b [1 2 3]} {:a {:b [1 2 3]}} true
       {:a.b [1 2 3]} {:a {:b [4]}} false
       {:a.b {:$regex "a"}} {:a {:b "cat"}} true
       {:a.b {:$regex "a"}} {:a {:b "dog"}} false
       {:a.b.c nil} {} true
       {:a.b.c nil} {:a 1} true
       {:a.b.c nil} {:a {:b 4}} true
       {:a.b 1} {:x 2} false
       {:a.b.c 1} {:a {:x 2}} false
       {:a.b.c 1} {:a {:b {:x 2}}} false
       {:a.b.c 1} {:a {:b 1}} false
       {:a.b.c 1} {:a {:b 0}} false
       {:a.b {:c 1}} {:a {:b {:c 1}}} true
       {:a.b {:c 1}} {:a {:b {:c 2}}} false
       {:a.b {:c 1}} {:a {:b 2}} false
       {:a.b {:c 1 :d 2}} {:a {:b {:c 1 :d 2}}} true
       {:a.b {:c 1 :d 2}} {:a {:b {:c 1 :d 1}}} false
       {:a.b {:c 1 :d 2}} {:a {:b {:d 2}}} false
       {:a.b {:$in [1 2 3]}} {:a {:b [2]}} true
       {:a.b {:$in [{:x 1} {:x 2} {:x 3}]}} {:a {:b [{:x 2}]}} true
       {:a.b {:$in [1 2 3]}} {:a {:b [4 2]}} true
       {:a.b {:$in [1 2 3]}} {:a {:b [4]}} false
       ))

(deftest dotted-array-keypath
  (are [x y match?] (= match? (m/match x y))
       {:dogs.0.name "Fido"} {:dogs [{:name "Fido"} {:name "Rex"}]} true
       {:dogs.1.name "Rex"} {:dogs [{:name "Fido"} {:name "Rex"}]} true
       {:dogs.1.name "Fido"} {:dogs [{:name "Fido"} {:name "Rex"}]} false
       {:room.1b "bla"} {:room {:1b "bla"}} true
       {:dogs.name "Fido"} {:dogs [{:name "Fido"} {:name "Rex"}]} true
       {:dogs.name "Rex"} {:dogs [{:name "Fido"} {:name "Rex"}]} true
       {:animals.dogs.name "Fido"} {:animals
                                    [{:dogs [{:name "Rover"}]}
                                     {}
                                     {:dogs [{:name "Fido"}
                                             {:name "Rex"}]}]} true
       {:animals.dogs.name "Fido"} {:animals
                                    [{:dogs {:name "Rex"}}
                                     {:dogs {:name "Fido"}}]} true
       {:animals.dogs.name "Fido"} {:animals
                                    [{:dogs [{:name "Rover"}]}
                                     {}
                                     {:dogs [{:name ["Fido"]}
                                             {:name "Rex"}]}]} true
       {:dogs.name "Fido"} {:dogs []} false
       ))

(deftest or-test
  (are [x y match?] (= match? (m/match x y))
       {:$or [{:a 1}]} {:a 1} true
       {:$or [{:b 2}]} {:a 1} false
       {:$or [{:a 1} {:b 2}]} {:a 1} true
       {:$or [{:c 3} {:d 4}]} {:a 1} false
       {:$or [{:a 1} {:b 2}]} {:a [1 2 3]} true
       {:$or [{:a 1} {:b 2}]} {:c [1 2 3]} false
       {:$or [{:a 1} {:b 2}]} {:a [2 3 4]} false
       {:$or [{:a 1} {:a 2}]} {:a 1} true
       {:$or [{:a 1} {:a 2}] :b 2} {:a 1 :b 2} true
       {:$or [{:a 2} {:a 3}] :b 2} {:a 1 :b 2} false
       {:$or [{:a 1} {:a 2}] :b 3} {:a 1 :b 2} false))

(deftest or-with-lt-lte-gt-gte
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$lte 1}} {:a 2}] {:a 1} true
       [{:a {:$lt 1}} {:a 2}] {:a 1} false
       [{:a {:$gte 1}} {:a 2}] {:a 1} true
       [{:a {:$gt 1}} {:a 2}] {:a 1} false
       [{:b {:$gt 1}} {:b {:$lt 3}}] {:b 2} true
       [{:b {:$lt 1}} {:b {:$gt 3}}] {:b 2} false))

(deftest or-with-in
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$in [1 2 3]}}] {:a 1} true
       [{:a {:$in [4 5 6]}}] {:a 1} false
       [{:a {:$in [1 2 3]}} {:b 2}] {:a 1} true
       [{:a {:$in [1 2 3]}} {:b 2}] {:b 2} true
       [{:a {:$in [1 2 3]}} {:b 2}] {:c 3} false
       [{:a {:$in [1 2 3]}} {:b {:$in [1 2 3]}}] {:b 2} true
       [{:a {:$in [1 2 3]}} {:b {:$in [4 5 6]}}] {:b 2} false
       ))

(deftest or-with-nin
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$nin [1 2 3]}}] {:a 1} false
       [{:a {:$nin [4 5 6]}}] {:a 1} true
       [{:a {:$nin [1 2 3]}} {:b 2}] {:a 1} false
       [{:a {:$nin [1 2 3]}} {:b 2}] {:b 2} true
       [{:a {:$nin [1 2 3]}} {:b 2}] {:c 3} true
       [{:a {:$nin [1 2 3]}} {:b {:$nin [1 2 3]}}] {:b 2} true
       [{:a {:$nin [1 2 3]}} {:b {:$nin [1 2 3]}}] {:a 1 :b 2} false
       [{:a {:$nin [1 2 3]}} {:b {:$nin [4 5 6]}}] {:b 2} true
       ))

(deftest or-with-dot
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a.b 1} {:a.b 2}] {:a {:b 1}} true
       [{:a.b 1} {:a.c 1}] {:a {:b 1}} true
       [{:a.b 2} {:a.c 1}] {:a {:b 1}} false
       ))

(deftest or-with-nested
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:b 1 :c 2}} {:a {:b 2 :c 1}}] {:a {:b 1 :c 2}} true
       [{:a {:b 1 :c 3}} {:a {:b 2 :c 1}}] {:a {:b 1 :c 2}} false
       ))

(deftest or-with-regex
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$regex "a"}}] {:a "cat"} true
       [{:a {:$regex "o"}}] {:a "cat"} false
       [{:a {:$regex "a"}} {:a {:$regex "o"}}] {:a "cat"} true
       [{:a {:$regex "i"}} {:a {:$regex "o"}}] {:a "cat"} false
       [{:a {:$regex "i"}} {:b {:$regex "o"}}] {:a "cat" :b "dog"} true
       ))

(deftest or-with-ne
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$ne 1}}] {} true
       [{:a {:$ne 1}}] {:a 1} false
       [{:a {:$ne 1}}] {:a 2} true
       [{:a {:$ne 1}}] {:b 1} true
       [{:a {:$ne 1}} {:a {:$ne 2}}] {:a 1} true
       [{:a {:$ne 1}} {:b {:$ne 1}}] {:a 1} true
       [{:a {:$ne 1}} {:b {:$ne 2}}] {:a 1 :b 2} false
       ))

(deftest or-with-not
  (are [x y match?] (= match? (m/match {:$or x} y))
       [{:a {:$not {:$mod [10 1]}}}] {} true
       [{:a {:$not {:$mod [10 1]}}}] {:a 1} false
       [{:a {:$not {:$mod [10 1]}}}] {:a 2} true
       [{:a {:$not {:$mod [10 1]}}} {:a {:$not {:$mod [10 2]}}}] {:a 1} true
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 1} false
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 2} true
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 3} true
       ))

(deftest nor-test
  (are [x y match?] (= match? (m/match x y))
       {:$nor [{:a 1}]} {:a 1} false
       {:$nor [{:b 2}]} {:a 1} true
       {:$nor [{:a 1} {:b 2}]} {:a 1} false
       {:$nor [{:c 3} {:d 4}]} {:a 1} true
       {:$nor [{:a 1} {:b 2}]} {:a [1 2 3]} false
       {:$nor [{:a 1} {:b 2}]} {:c [1 2 3]} true
       {:$nor [{:a 1} {:b 2}]} {:a [2 3 4]} true
       {:$nor [{:a 1} {:a 2}]} {:a 1} false))

(deftest nor-with-lt-lte-gt-gte
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$lte 1}} {:a 2}] {:a 1} false
       [{:a {:$lt 1}} {:a 2}] {:a 1} true
       [{:a {:$gte 1}} {:a 2}] {:a 1} false
       [{:a {:$gt 1}} {:a 2}] {:a 1} true
       [{:b {:$gt 1}} {:b {:$lt 3}}] {:b 2} false
       [{:b {:$lt 1}} {:b {:$gt 3}}] {:b 2} true))

(deftest nor-with-in
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$in [1 2 3]}}] {:a 1} false
       [{:a {:$in [4 5 6]}}] {:a 1} true
       [{:a {:$in [1 2 3]}} {:b 2}] {:a 1} false
       [{:a {:$in [1 2 3]}} {:b 2}] {:b 2} false
       [{:a {:$in [1 2 3]}} {:b 2}] {:c 3} true
       [{:a {:$in [1 2 3]}} {:b {:$in [1 2 3]}}] {:b 2} false
       [{:a {:$in [1 2 3]}} {:b {:$in [4 5 6]}}] {:b 2} true
       ))

(deftest nor-with-nin
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$nin [1 2 3]}}] {:a 1} true
       [{:a {:$nin [4 5 6]}}] {:a 1} false
       [{:a {:$nin [1 2 3]}} {:b 2}] {:a 1} true
       [{:a {:$nin [1 2 3]}} {:b 2}] {:b 2} false
       [{:a {:$nin [1 2 3]}} {:b 2}] {:c 3} false
       [{:a {:$nin [1 2 3]}} {:b {:$nin [1 2 3]}}] {:b 2} false
       [{:a {:$nin [1 2 3]}} {:b {:$nin [1 2 3]}}] {:a 1 :b 2} true
       [{:a {:$nin [1 2 3]}} {:b {:$nin [4 5 6]}}] {:b 2} false
       ))

(deftest nor-with-dot
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a.b 1} {:a.b 2}] {:a {:b 1}} false
       [{:a.b 1} {:a.c 1}] {:a {:b 1}} false
       [{:a.b 2} {:a.c 1}] {:a {:b 1}} true
       ))

(deftest nor-with-nested
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:b 1 :c 2}} {:a {:b 2 :c 1}}] {:a {:b 1 :c 2}} false
       [{:a {:b 1 :c 3}} {:a {:b 2 :c 1}}] {:a {:b 1 :c 2}} true
       ))

(deftest nor-with-regex
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$regex "a"}}] {:a "cat"} false
       [{:a {:$regex "o"}}] {:a "cat"} true
       [{:a {:$regex "a"}} {:a {:$regex "o"}}] {:a "cat"} false
       [{:a {:$regex "i"}} {:a {:$regex "o"}}] {:a "cat"} true
       [{:a {:$regex "i"}} {:b {:$regex "o"}}] {:a "cat" :b "dog"} false
       ))

(deftest nor-with-ne
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$ne 1}}] {} false
       [{:a {:$ne 1}}] {:a 1} true
       [{:a {:$ne 1}}] {:a 2} false
       [{:a {:$ne 1}}] {:b 1} false
       [{:a {:$ne 1}} {:a {:$ne 2}}] {:a 1} false
       [{:a {:$ne 1}} {:b {:$ne 1}}] {:a 1} false
       [{:a {:$ne 1}} {:b {:$ne 2}}] {:a 1 :b 2} true
       ))

(deftest nor-with-not
  (are [x y match?] (= match? (m/match {:$nor x} y))
       [{:a {:$not {:$mod [10 1]}}}] {} false
       [{:a {:$not {:$mod [10 1]}}}] {:a 1} true
       [{:a {:$not {:$mod [10 1]}}}] {:a 2} false
       [{:a {:$not {:$mod [10 1]}}} {:a {:$not {:$mod [10 2]}}}] {:a 1} false
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 1} true
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 2} false
       [{:a {:$not {:$mod [10 1]}}} {:a {:$mod [10 2]}}] {:a 3} false
       ))

(deftest and-test
  (are [x y match?] (= match? (m/match x y))
       {:$and [{:a 1}]} {:a 1} true
       {:$and [{:a 1} {:a 2}]} {:a 1} false
       {:$and [{:a 1} {:b 2}]} {:a 1} false
       {:$and [{:a 1} {:b 2}]} {:a 1 :b 2} true
       {:$and [{:a 1} {:b 1}]} {:a 1 :b 2} false
       {:$and [{:a 1} {:b 2}] :c 3} {:a 1 :b 2 :c 3} true
       {:$and [{:a 1} {:b 2}] :c 4} {:a 1 :b 2 :c 3} false
       ))

(deftest and-with-regex
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$regex "/a/"}}] {:a "cat"} true
       [{:a {:$regex "/a/i"}}] {:a "CAT"} true
       [{:a {:$regex "/o/"}}] {:a "cat"} false
       [{:a {:$regex "/a/"}} {:a {:$regex "/o/"}}] {:a "cat"} false
       [{:a {:$regex "/a/"}} {:b {:$regex "/o/"}}] {:a "cat" :b "dog"} true
       [{:a {:$regex "/a/"}} {:b {:$regex "/a/"}}] {:a "cat" :b "dog"} false
       ))

(deftest and-with-dot-and-nested
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a.b 1}] {:a {:b 1}} true
       [{:a {:b 1}}] {:a {:b 1}} true
       [{:a.b 2}] {:a {:b 1}} false
       [{:a.c 1}] {:a {:b 1}} false
       [{:a.b 1} {:a.b 2}] {:a {:b 1}} false
       [{:a.b 1} {:a {:b 2}}] {:a {:b 1}} false
       [{:a.b 1} {:c.d 2}] {:a {:b 1} :c {:d 2}} true
       [{:a.b 1} {:c.d 1}] {:a {:b 1} :c {:d 2}} false
       [{:a.b 1} {:c {:d 2}}] {:a {:b 1} :c {:d 2}} true
       [{:a.b 1} {:c {:d 1}}] {:a {:b 1} :c {:d 2}} false
       [{:a.b 2} {:c {:d 2}}] {:a {:b 1} :c {:d 2}} false
       [{:a {:b 1}} {:c {:d 2}}] {:a {:b 1} :c {:d 2}} true
       [{:a {:b 2}} {:c {:d 2}}] {:a {:b 1} :c {:d 2}} false
       ))

(deftest and-with-in
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$in []}}] {} false
       [{:a {:$in [1 2 3]}}] {:a 1} true
       [{:a {:$in [4 5 6]}}] {:a 1} false
       [{:a {:$in [1 2 3]}} {:a {:$in [4 5 6]}}] {:a 1} false
       [{:a {:$in [1 2 3]}} {:b {:$in [1 2 3]}}] {:a 1 :b 4} false
       [{:a {:$in [1 2 3]}} {:b {:$in [4 5 6]}}] {:a 1 :b 4} true
       ))

(deftest and-with-nin
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$nin []}}] {} true
       [{:a {:$nin [1 2 3]}}] {:a 1} false
       [{:a {:$nin [4 5 6]}}] {:a 1} true
       [{:a {:$nin [1 2 3]}} {:a {:$in [4 5 6]}}] {:a 1} false
       [{:a {:$nin [1 2 3]}} {:b {:$in [1 2 3]}}] {:a 1 :b 4} false
       [{:a {:$nin [1 2 3]}} {:b {:$in [4 5 6]}}] {:a 1 :b 4} false
       ))

(deftest and-with-lt-lte-gt-gte
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$lt 2}}] {:a 1} true
       [{:a {:$lt 1}}] {:a 1} false
       [{:a {:$lte 1}}] {:a 1} true
       [{:a {:$gt 0}}] {:a 1} true
       [{:a {:$gt 1}}] {:a 1} false
       [{:a {:$gte 1}}] {:a 1} true
       [{:a {:$gt 0}} {:a {:$lt 2}}] {:a 1} true
       [{:a {:$gt 1}} {:a {:$lt 2}}] {:a 1} false
       [{:a {:$gt 0}} {:a {:$lt 1}}] {:a 1} false
       [{:a {:$gte 1}} {:a {:$lte 1}}] {:a 1} true
       [{:a {:$gte 2}} {:a {:$lte 0}}] {:a 1} false
       ))

(deftest and-with-ne
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$ne 1}}] {} true
       [{:a {:$ne 1}}] {:a 1} false
       [{:a {:$ne 1}}] {:a 2} true
       [{:a {:$ne 1}} {:a {:$ne 2}}] {:a 2} false
       [{:a {:$ne 1}} {:a {:$ne 3}}] {:a 2} true
       ))

(deftest and-with-not
  (are [x y match?] (= match? (m/match {:$and x} y))
       [{:a {:$not {:$gt 2}}}] {:a 1} true
       [{:a {:$not {:$lt 2}}}] {:a 1} false
       [{:a {:$not {:$lt 0}}} {:a {:$not {:$gt 2}}}] {:a 1} true
       [{:a {:$not {:$lt 2}}} {:a {:$not {:$gt 0}}}] {:a 1} false
       ))

(deftest elemMatch-test
  (are [x y match?] (= match? (m/match x y))
       {:dogs {:$elemMatch {:name {:$regex "/e/"}}}}
       ,{:dogs [{:name "Fido"} {:name "Rex"}]} true
       {:dogs {:$elemMatch {:name {:$regex "/a/"}}}}
       ,{:dogs [{:name "Fido"} {:name "Rex"}]} false
       {:dogs {:$elemMatch {:age {:$gt 4}}}}
       ,{:dogs [{:name "Fido" :age 5} {:name "Rex" :age 3}]} true
       {:dogs {:$elemMatch {:age {:$gt 4} :name "Fido"}}}
       ,{:dogs [{:name "Fido" :age 5} {:name "Rex" :age 3}]} true
       {:dogs {:$elemMatch {:age {:$gt 5} :name "Fido"}}}
       ,{:dogs [{:name "Fido" :age 5} {:name "Rex" :age 3}]} false
       {:dogs {:$elemMatch {:age {:$gt 4} :name {:$regex "/i/"}}}}
       ,{:dogs [{:name "Fido" :age 5} {:name "Rex" :age 3}]} true
       {:dogs {:$elemMatch {:age 5 :name {:$regex "/e/"}}}}
       ,{:dogs [{:name "Fido" :age 5} {:name "Rex" :age 3}]} false
       ))
