(ns methojure.communication.test.core
  (:require-macros
   [methojure.test.macros :refer [is deftest wait-for-async done
                                  before-each after-each set-up tear-down]])
  (:require [methojure.test.core :as j]
            [methojure.communication.stream :as stream]
            [methojure.communication.core :as comm]))

;; initialize communication
(comm/methojure-stream
 "/sockjs"
 comm/default-middleware)

;; define method names

(def hello-world-name
  :methojure.communication.test.test-server/hello-world)

(def inc-counter-name
  :methojure.communication.test.test-server/inc-counter)

(def counter-name
  :methojure.communication.test.test-server/counter)

;; start the tests.

(deftest call-hello-world-action
  (let [handler (fn [res]
                  (is (= res "hello, jens"))
                  (done))]
    (comm/call-action handler hello-world-name "jens")
    (wait-for-async "wait for action respond")))

(deftest call-hello-world-action-two-times
  (let [counter (atom 0)
        handler-1 (fn [res]
                    (swap! counter inc)
                    (is (= res "hello, iris")))
        handler-2 (fn [res]
                    (swap! counter inc)
                    (is (= res "hello, jens")))]
    (comm/call-action handler-1 hello-world-name "iris")
    (comm/call-action handler-2 hello-world-name "jens")
    (js/setTimeout (fn [] (is (= @counter 2)) (done)) 200)
    (wait-for-async "wait for timeout")))

(deftest subscribe-to-counter
  (comm/subscribe
   counter-name
   (fn [res] ;; on subscribe
     (comm/call-action (fn [res] nil) inc-counter-name)
     (wait-for-async "wait for publish message"))
   (fn [old new] ;; on publish
     (is (= (inc old) new))
     (done)))
  (wait-for-async "wait for inc call response"))
