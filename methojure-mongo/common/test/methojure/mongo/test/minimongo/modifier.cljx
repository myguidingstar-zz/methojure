^:clj (ns methojure.mongo.test.minimongo.modifier
        (:use clojure.test)
        (:require [methojure.mongo.minimongo.modifier :as m]))

^:cljs (ns methojure.mongo.test.minimongo.modifier
         (:require-macros
          [methojure.test.macros :refer [is deftest are]])
         (:require [methojure.test.core :as j]
                   [methojure.mongo.minimongo.modifier :as m]))

(deftest doc-replacement
  (are [x y z] (= z (m/modify x y))
       {} {} {}
       {:a 12} {} {}
       {:a 12} {:a 13} {:a 13}
       {:a 12 :b 99} {:a 13} {:a 13}))

(deftest modifty-keys
  (are [x y z] (= z (m/modify x y))
       {} {:$set {:a 12}} {:a 12}
       {} {:$set {:a.b 12}} {:a {:b 12}}
       {} {:$set {:a.b.c 12}} {:a {:b {:c 12}}}
       {:a {:d 99}} {:$set {:a.b.c 12}} {:a {:b {:c 12} :d 99}}
       {} {:$set {:a.b.3.c 12}} {:a {:b {:3 {:c 12}}}}
       {:a {:b []}} {:$set {:a.b.3.c 12}} {:a {:b [nil nil nil {:c 12}]}}
       {:a [nil nil nil]} {:$set {:a.3.b 12}} {:a [nil nil nil {:b 12}]}
       {:a {}} {:$set {:a.3 12}} {:a {:3 12}}
       {} {:$set {"" 12}} {"" 12}
       {} {:$set {". " 12}} {"" {" " 12}}
       {} {:$inc {"... " 12}} {"" {"" {"" {" " 12}}}}
       {} {:$set {"a..b" 12}} {:a {"" {:b 12}}}
       {:a [1 2 3]} {:$set {:a.01 99}} {:a [1 99 3]}
       {:a [1 {:a 98} 3]} {:$set {:a.01.b 99}} {:a [1 {:a 98 :b 99} 3]}
       {} {:$set {:2.a.b 12}} {:2 {:a {:b 12}}}
       {:x []} {:$set {:x.2..a 99}} {:x [nil nil {"" {:a 99}}]}
       {:x [nil nil]} {:$set {:x.2.a 1}} {:x [nil nil {:a 1}]}))

(deftest inc-test
  (are [x y z] (= z (m/modify x {:$inc y}))
       {:a 1 :b 2} {:a 10} {:a 11 :b 2}
       {:a 1 :b 2} {:c 10} {:a 1 :b 2 :c 10}
       {:a [1 2]} {:a.1 10} {:a [1 12]}
       {:a [1 2]} {:a.2 10} {:a [1 2 10]}
       {:a [1 2]} {:a.3 10} {:a [1 2 nil 10]}
       {:a {:b 2}} {:a.b 10} {:a {:b 12}}
       {:a {:b 2}} {:a.c 10} {:a {:b 2 :c 10}}))

(deftest $set-test
  (are [x y z] (= z (m/modify x {:$set y}))
       {:a 1 :b 2} {:a 10} {:a 10 :b 2}
       {:a 1 :b 2} {:c 10} {:a 1 :b 2 :c 10}
       {:a 1 :b 2} {:a {:c 10}} {:a {:c 10} :b 2}
       {:a [1 2] :b 2} {:a [3 4]} {:a [3 4] :b 2}
       {:a [1 2 3] :b 2} {:a.1 [3 4]} {:a [1 [3 4] 3] :b 2}
       {:a [1] :b 2} {:a.1 9} {:a [1 9] :b 2}
       {:a [1] :b 2} {:a.2 9} {:a [1 nil 9] :b 2}
       {:a {:b 1}} {:a.c 9} {:a {:b 1 :c 9}}))

(deftest unset-test
  (are [x y z] (= z (m/modify x {:$unset y}))
       {} {:a 1} {}
       {:a 1} {:a 1} {}
       {:a 1 :b 2} {:a 1} {:b 2}
       {:a 1 :b 2} {:a 0} {:b 2}
       {:a 1 :b 2} {:a false} {:b 2}
       {:a 1 :b 2} {:a nil} {:b 2}
       {:a 1 :b 2} {:a [1]} {:b 2}
       {:a 1 :b 2} {:a {}} {:b 2}
       {:a {:b 2 :c 3}} {:a.b 1} {:a {:c 3}}
       {:a [1 2 3]} {:a.1 1} {:a [1 nil 3]}
       {:a [1 2 3]} {:a.2 1} {:a [1 2 nil]}
       {:a [1 2 3]} {:a.x 1} {:a [1 2 3]}
       {:a {:b 1}} {:a.b.c.d 1} {:a {:b 1}}
       {:a {:b 1}} {:a.x.c.d 1} {:a {:b 1}}
       {:a {:b {:c 1}}} {:a.b.c 1} {:a {:b {}}}))

(deftest push-test
  (are [x y z] (= z (m/modify x {:$push y}))
       {} {:a 1} {:a [1]}
       {:a []} {:a 1} {:a [1]}
       {:a [1]} {:a 2} {:a [1 2]}
       {:a [1]} {:a [2]} {:a [1 [2]]}
       {:a []} {:a.1 99} {:a [nil [99]]}
       {:a {}} {:a.x 99} {:a {:x [99]}}))

(deftest pushAll-test
  (are [x y z] (= z (m/modify x {:$pushAll y}))
       {} {:a [1]} {:a [1]}
       {:a []} {:a [1]} {:a [1]}
       {} {:a [1 2]} {:a [1 2]}
       {:a []} {:a [1 2]} {:a [1 2]}
       {:a [1]} {:a [2 3]} {:a [1 2 3]}
       {} {:a []} {:a []}
       {:a []} {:a []} {:a []}
       {:a [1]} {:a []} {:a [1]}
       {:a []} {:a.1 [99]} {:a [nil [99]]}
       {:a []} {:a.1 []} {:a [nil []]}
       {:a {}} {:a.x [99]} {:a {:x [99]}}
       {:a {}} {:a.x []} {:a {:x []}}))

(deftest addToSet-test
  (are [x y z] (= z (m/modify x {:$addToSet y}))
       {} {:a 1} {:a [1]}
       {:a []} {:a 1} {:a [1]}
       {:a [1]} {:a 2} {:a [1 2]}
       {:a [1 2]} {:a 1} {:a [1 2]}
       {:a [1 2]} {:a 2} {:a [1 2]}
       {:a [1 2]} {:a 3} {:a [1 2 3]}
       {:a [1]} {:a [2]} {:a [1 2]}
       {} {:a {:x 1}} {:a [{:x 1}]}
       {:a [{:x 1}]} {:a {:x 1}} {:a [{:x 1}]}
       {:a [{:x 1}]} {:a {:x 2}} {:a [{:x 1} {:x 2}]}
       {:a [{:x 1 :y 2}]} {:a {:x 1 :y 2}} {:a [{:x 1 :y 2}]}
       {:a [{:x 1 :y 2}]} {:a {:x 2 :y 2}} {:a [{:x 1 :y 2} {:y 2 :x 2}]}
       {:a [1 2]} {:a {:$each [3 1 4]}} {:a [1 2 3 4]}
       {:a [1 2]} {:a {:$each [3 1 4] :b 12}} {:a [1 2 3 4]}
       ;;{:a [1 2]} {:a {:b 12 :$each [3 1 4]}} {:a [1 2 {:b 12 :$each [3 1 4]}]}
       {:a []} {:a.1 99} {:a [nil [99]]}
       {:a {}} {:a.x 99} {:a {:x [99]}}))

(deftest pop-test
  (are [x y z] (= z (m/modify x {:$pop y}))
       {} {:a 1} {}
       {} {:a -1} {}
       {:a []} {:a 1} {:a []}
       {:a []} {:a -1} {:a []}
       {:a [1 2 3]} {:a 1} {:a [1 2]}
       {:a [1 2 3]} {:a 10} {:a [1 2]}
       {:a [1 2 3]} {:a 0.001} {:a [1 2]}
       {:a [1 2 3]} {:a 0} {:a [1 2]}
       {:a [1 2 3]} {:a "stuff"} {:a [1 2]}
       {:a [1 2 3]} {:a -1} {:a [2 3]}
       {:a [1 2 3]} {:a -10} {:a [2 3]}
       {:a [1 2 3]} {:a -0.001} {:a [2 3]}
       {:a []} {:a.1 1} {:a []}
       {:a [1 [2 3] 4]} {:a.1 1} {:a [1 [2] 4]}
       {:a {}} {:a.x 1} {:a {}}
       {:a {:x [2 3]}} {:a.x 1} {:a {:x [2]}}))

(deftest pull-test
  (are [x y z] (= z (m/modify x {:$pull y}))
       {} {:a 1} {}
       {} {:a.x 1} {}
       {:a {}} {:a.x 1} {:a {}}
       {:a [2 1 2]} {:a 1} {:a [2 2]}
       {:a [2 1 2]} {:a 2} {:a [1]}
       {:a [2 1 2]} {:a 3} {:a [2 1 2]}
       {:a []} {:a 3} {:a []}
       {:a [[2] [2 1] [3]]} {:a [2 1]} {:a [[2] [3]]}
       {:a [{:b 1 :c 2} {:b 2 :c 2}]} {:a {:b 1}} {:a [{:b 2 :c 2}]}
       {:a [{:b 1 :c 2} {:b 2 :c 2}]} {:a {:c 2}} {:a []}
       ;; {:a [1 2 3 4]} {:$gt 2} {:a [1 2]}
       ))

(deftest pullAll-test
  (are [x y z] (= z (m/modify x {:$pullAll y}))
       {} {:a [1]} {}
       {:a [1 2 3]} {:a []} {:a [1 2 3]}
       {:a [1 2 3]} {:a [2]} {:a [1 3]}
       {:a [1 2 3]} {:a [2 1]} {:a [3]}
       {:a [1 2 3]} {:a [1 2]} {:a [3]}
       {} {:a.b.c [2]} {}
       {:x [{:a 1} {:a 1 :b 2}]} {:x [{:a 1}]} {:x [{:a 1 :b 2}]}))

(deftest rename-test
  (are [x y z] (= z (m/modify x {:$rename y}))
       {} {:a :b} {}
       {:a [12]} {:a :b} {:b [12]}
       {:a {:b 12}} {:a :c} {:c {:b 12}}
       {:a {:b 12}} {:a.b :a.c} {:a {:c 12}}
       {:a {:b 12}} {:a.b :x} {:a {} :x 12}
       {:a {:b 12}} {:a.b :q.r} {:a {} :q {:r 12}}
       {:a {:b 12}} {:a.b :q.2.r} {:a {} :q {:2 {:r 12}}}
       {:a {:b 12} :q {}} {:a.b :q.2.r} {:a {} :q {:2 {:r 12}}}
       {:a 12 :b 12} {:a :b} {:b 12}))
