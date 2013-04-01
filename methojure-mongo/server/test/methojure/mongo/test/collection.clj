(ns methojure.mongo.test.collection
  (:use [clojure.test])
  (:require [monger.core :as mg]
            [monger.db :as mdb]
            [methojure.mongo.api :as mc]
            [methojure.mongo.collection :as mcoll]))

(defn with-mongo [f]
  (mg/connect!)
  (mg/set-db! (mg/get-db "methojure_mongo_test"))
  (f)
  (mdb/drop-db)
  (mg/disconnect!))

(use-fixtures :once with-mongo)

(deftest mongo-basic
  (let [c (mcoll/create-collection "mongo-basic")]
    ;; clean all before testing
    (mc/remove c {})
    (mc/insert c {:type "kitten" :name "fluffy"})
    (mc/insert c {:type "kitten" :name "snookums"})
    (mc/insert c {:type "cryptographer" :name "alice"})
    (mc/insert c {:type "cryptographer" :name "bob"})
    (mc/insert c {:type "cryptographer" :name "cara"})
    (is (= 5 (-> c (mc/find) (mc/count))))
    (is (= 2 (-> c (mc/find {:type "kitten"}) (mc/count))))
    (is (= 3 (-> c (mc/find {:type "cryptographer"}) (mc/count))))
    (is (= 2 (count (-> c (mc/find {:type "kitten"}) (mc/fetch)))))
    (is (= 3 (count (-> c (mc/find {:type "cryptographer"}) (mc/fetch)))))

    (mc/remove c {:name "cara"})
    (is (= 4 (-> c (mc/find) (mc/count))))
    (is (= 2 (-> c (mc/find {:type "kitten"}) (mc/count))))
    (is (= 2 (-> c (mc/find {:type "cryptographer"}) (mc/count))))
    (is (= 2 (count (-> c (mc/find {:type "kitten"}) (mc/fetch)))))
    (is (= 2 (count (-> c (mc/find {:type "cryptographer"}) (mc/fetch)))))

    (mc/update c {:name "snookums"} {:$set {:type "cryptographer"}})
    (is (= 4 (-> c (mc/find) (mc/count))))
    (is (= 1 (-> c (mc/find {:type "kitten"}) (mc/count))))
    (is (= 3 (-> c (mc/find {:type "cryptographer"}) (mc/count))))
    (is (= 1 (count (-> c (mc/find {:type "kitten"}) (mc/fetch)))))
    (is (= 3 (count (-> c (mc/find {:type "cryptographer"}) (mc/fetch)))))

    (mc/remove c nil)
    (mc/remove c false)
    (is (= 4 (-> c (mc/find) (mc/count))))

    (mc/remove c {:_id nil})
    (mc/remove c {:_id false})
    (is (= 4 (-> c (mc/find) (mc/count))))

    (mc/remove c {})
    (is (= 0 (-> c (mc/find) (mc/count))))

    (mc/insert c {:_id 1 :name "strawberry" :tags ["fruit" "red" "squishy"]})
    (mc/insert c {:_id 2 :name "apple" :tags ["fruit" "red" "hard"]})
    (mc/insert c {:_id 3 :name "rose" :tags ["flower" "red" "squishy"]})
    (is (= 1 (-> c (mc/find {:tags "flower"}) (mc/count))))
    (is (= 2 (-> c (mc/find {:tags "fruit"}) (mc/count))))
    (is (= 3 (-> c (mc/find {:tags "red"}) (mc/count))))
    (is (= 1 (count (-> c (mc/find {:tags "flower"}) (mc/fetch)))))
    (is (= 2 (count (-> c (mc/find {:tags "fruit"}) (mc/fetch)))))
    (is (= 3 (count (-> c (mc/find {:tags "red"}) (mc/fetch)))))

    (is (= "strawberry" (-> c (mc/find-one 1) (:name))))
    (is (= "apple" (-> c (mc/find-one 2) (:name))))
    (is (= "rose" (-> c (mc/find-one 3) (:name))))
    (is (= nil (-> c (mc/find-one 4))))
    (is (= nil (-> c (mc/find-one "abc"))))
    (is (= nil (-> c (mc/find-one nil))))

    (are [id param cnt] (= cnt (-> c (mc/find id param) (mc/count)))
         1 nil 1
         4 nil 0
         "abc" nil 0
         nil nil 0
         ;;1 {:skip 1} 0
         ;;{:_id 1} {:skip 1} 0
         {} {:skip 1} 2
         {} {:skip 2} 1
         {} {:limit 2} 2
         {} {:limit 1} 1
         {} {:skip 1 :limit 1} 1
         {:tags "fruit"} {:skip 1} 1
         {:tags "fruit"} {:limit 1} 1
         {:tags "fruit"} {:skip 1 :limit 1} 1
         ;;1 {:sort [["_id" "desc"]] :skip 1} 0
         ;;{:_id 1} {:sort [["_id" "desc"]] :skip 1} 0
         {} {:sort [[:_id :desc]] :skip 1} 2
         {} {:sort [[:_id :desc]] :skip 2} 1
         {} {:sort [[:_id :desc]] :limit 2} 2
         {} {:sort [[:_id :desc]] :limit 1} 1
         {} {:sort [[:_id :desc]] :limit 1 :skip 1} 1
         {:tags "fruit"} {:sort [[:_id :desc]] :skip 1} 1
         {:tags "fruit"} {:sort [[:_id :desc]] :limit 1} 1
         {:tags "fruit"} {:sort [[:_id :desc]] :skip 1 :limit 1} 1)
    (is (= 1 (-> c (mc/find 1) (mc/count))))
    (is (= 0 (-> c (mc/find 4) (mc/count))))
    (is (= 0 (-> c (mc/find "abc") (mc/count))))

    (mc/insert c {:foo {:bar "baz"}})
    (are [query cnt] (= cnt (-> c (mc/find query) (mc/count)))
         {:foo {:bam "baz"}} 0
         {:foo {:bar "baz"}} 1)))

(deftest sorted-query
  (let [c (mcoll/create-collection "mongo-sorted-query")]
    (mc/insert c {:type "kitten" :name "fluffy"})
    (mc/insert c {:type "kitten" :name "snookums"})
    (mc/insert c {:type "cryptographer" :name "snake"})
    (mc/insert c {:type "cryptographer" :name "alice"})
    (mc/insert c {:type "cryptographer" :name "bob"})
    (mc/insert c {:type "cryptographer" :name "cara"})

    (are [query options res] (= res (vec (map :name (-> c
                                                        (mc/find query options)
                                                        (mc/fetch)))))
         {:type "kitten"} {:sort [[:name 1]]} ["fluffy" "snookums"]
         {:type "kitten"} {:sort [[:name :asc]]} ["fluffy" "snookums"]
         {:type "kitten"} {:sort {:name 1}} ["fluffy" "snookums"]
         {:type "kitten"} {:sort {:name :asc}} ["fluffy" "snookums"]

         {:type "kitten"} {:sort [[:name -1]]} ["snookums" "fluffy"]
         {:type "kitten"} {:sort [[:name :desc]]} ["snookums" "fluffy"]
         {:type "kitten"} {:sort {:name -1}} ["snookums" "fluffy"]
         {:type "kitten"} {:sort {:name :desc}} ["snookums" "fluffy"]

         {} {:sort [[:type 1] [:name 1]]}
         ,["alice" "bob" "cara" "snake" "fluffy" "snookums"]
         {} {:sort [[:type :asc] [:name :asc]]}
         ,["alice" "bob" "cara" "snake" "fluffy" "snookums"]
         {} {:sort [[:type 1] [:name :asc]]}
         ,["alice" "bob" "cara" "snake" "fluffy" "snookums"]

         {} {:sort [[:name 1] [:type 1]]}
         ,["alice" "bob" "cara" "fluffy" "snake" "snookums"]
         {} {:sort [[:name :asc] [:type :asc]]}
         ,["alice" "bob" "cara" "fluffy" "snake" "snookums"]
         {} {:sort [[:name :asc] [:type 1]]}
         ,["alice" "bob" "cara" "fluffy" "snake" "snookums"]

         {} {:sort [[:type -1] [:name 1]]}
         ,["fluffy" "snookums" "alice" "bob" "cara" "snake"]
         {} {:sort [[:type :desc] [:name 1]]}
         ,["fluffy" "snookums" "alice" "bob" "cara" "snake"]
         {} {:sort [[:type :desc] [:name :asc]]}
         ,["fluffy" "snookums" "alice" "bob" "cara" "snake"]

         {} {:sort [[:type -1] [:name -1]]}
         ,["snookums" "fluffy" "snake" "cara" "bob" "alice"]
         {} {:sort [[:type :desc] [:name :desc]]}
         ,["snookums" "fluffy" "snake" "cara" "bob" "alice"]
         )))

(deftest observe-changes
  (let [c (mcoll/create-collection "mongo-observe-changes")
        cursor (mc/find c {} {:sort {:a 1}})
        changes (atom '())]
    (add-watch cursor :test-changes (fn [x] (swap! changes conj x)))

    (mc/insert c {:_id 1 :a 1})
    (is (= {:+ [[-1 {:_id 1 :a 1}]]
            :- []}
           (first @changes)))

    (mc/update c {:a 1} {:$set {:a 2}})
    (is (= {:+ [[-1 {:_id 1 :a 2}]]
            :- [0]}
           (first @changes)))

    (mc/insert c {:_id 2 :a 10})
    (is (= {:+ [[0 {:_id 2 :a 10}]]
            :- []}
           (first @changes)))

    (mc/update c {} {:$inc {:a 1}} {:multi true})
    (is (= {:+ [[-1 {:a 3, :_id 1} {:a 11, :_id 2}]]
            :- [0 1]}
           (first @changes)))

    (mc/update c {:a 11} {:a 1})
    (is (= {:+ [[-1 {:a 1, :_id 2}]]
            :- [1]}
           (first @changes)))

    (mc/remove c {:a 2})
    (is (= {:+ []
            :- []}
           (first @changes)))

    (mc/remove c {:a 3})
    (is (= {:+ []
            :- [1]}
           (first @changes)))
    ))
