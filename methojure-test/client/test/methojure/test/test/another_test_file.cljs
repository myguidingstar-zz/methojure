(ns methojure.jasmine.test.another-test-file
  (:require-macros
   [methojure.test.macros :refer (is deftest wait-for-async done)])
  (:require [methojure.test.core :as j]))

(deftest my-first-test
  (is (= "c" "c")))

(deftest my-second-test
  (is (= "d" "d")))

(deftest async-test
  (js/setTimeout (fn [] (done)) 100)
  (wait-for-async "Timeout must finish before it ends."))
