(ns methojure.sockjs.test.core
  (:require [methojure.sockjs.core :as s]
            [methojure.test.core :as t])
  (:require-macros
   [methojure.test.macros :refer [deftest is done wait-for-async]]))

(deftest on-open-listener
  (let [handler (-> (s/create-sockjs-handler "http://localhost:9876/echo")
                    (s/on-open (fn [] (done)))
                    (s/on-error (fn [] (done))))]
    (wait-for-async "wait for the on open handler")))

(deftest on-message-listener
  (let [handler (-> (s/create-sockjs-handler "http://localhost:9876/echo")
                    (s/on-message (fn [msg]
                                    (is (= (.-data msg) "hello"))
                                    (done))))]
    (s/on-open handler (fn [] (s/send! handler "hello")))
    (wait-for-async "wait for the hello msg to come back")))

(deftest on-close-listener
  
  (let [handler (-> (s/create-sockjs-handler "http://localhost:9876/close")
                    (s/on-close (fn [] (done))))]
    ;;(s/on-open handler (fn [] (s/send! handler "hello")))
    (wait-for-async "wait for the on close handler")))
