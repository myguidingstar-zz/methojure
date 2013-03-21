(ns methojure.communication.test.core
  (:require-macros
   [methojure.test.macros :refer [is deftest wait-for-async done
                                  before-each after-each set-up tear-down]])
  (:require [methojure.test.core :as j]
            [methojure.communication.stream :as stream]
            [methojure.communication.core :as comm]))

(deftest call-hello-world-action
  (let [handler (fn [res]
                  (is (= res "hello, jens"))
                  (done))]
    (comm/call-action handler :hello-world "jens")
    (wait-for-async "wait for action respond")))

(deftest call-hello-world-action-two-times
  (let [counter (atom 0)
        handler-1 (fn [res]
                    (swap! counter inc)
                    (is (= res "hello, iris")))
        handler-2 (fn [res]
                    (swap! counter inc)
                    (is (= res "hello, jens")))]
    (comm/call-action handler-1 :hello-world "iris")
    (comm/call-action handler-2 :hello-world "jens")
    (js/setTimeout (fn [] (is (= @counter 2)) (done)) 200)
    (wait-for-async "wait for timeout")))

(deftest subscribe-to-counter
  (js/setTimeout (fn [res]
                   (comm/call-action (fn [res] nil) :inc-counter)
                   (wait-for-async "wait for publish message"))
                 50)
  (comm/subscribe :counter (fn [old new]
                             (is (= (inc old) new))
                             (done)))
  (wait-for-async "wait for inc call response"))
