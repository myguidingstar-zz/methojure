(ns methojure.jasmine.test.core
  (:require-macros
   [methojure.test.macros :refer [is deftest wait-for-async done
                                  before-each after-each set-up tear-down]])
  (:require [methojure.test.core :as j]))

;; define some counter
(def value1 (atom 0))
(def value2 (atom 0))
(def value3 (atom 0))

;; register listener
(set-up
 (swap! value1 inc))

(before-each
 (swap! value2 inc))

(after-each
 (swap! value3 inc))

(tear-down
 (reset! value1 0)
 (reset! value2 0)
 (reset! value3 0))

;; define tests

(deftest my-first-test
  (is (= 1 @value1))
  (is (= 1 @value2))
  (is (= 0 @value3)))

(deftest my-second-test
  (is (= 1 @value1))
  (is (= 2 @value2))
  (is (= 1 @value3)))

(deftest my-third-test
  (is (= 1 @value1))
  (is (= 3 @value2))
  (is (= 2 @value3)))

(deftest my-async-test
  (js/setTimeout (fn [] (done)) 100)
  (wait-for-async "Timeout must finish"))
