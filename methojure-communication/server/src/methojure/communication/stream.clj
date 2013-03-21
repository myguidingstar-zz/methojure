(ns methojure.communication.stream
  (:use [methojure.sockjs.session :only [SockjsConnection on-open on-close
                                         on-message send!]])
  (:require [methojure.sockjs.core :as sockjs]
            [clojure.edn :as edn]))

(def open-sockets (atom []))

(defn handle-response [session response]
  (cond
   (string? response) (send! session {:type :msg :content response})
   (and (map? response) (= (:type response) :msg)) (send! session response)
   (and (map? response) (= (:type response) :open)) (send! session response)
   (and (map? response) (= (:type response) :close)) (send! session response)
   (nil? response) session
   :else (send! session {:type :msg :content (pr-str response)})))

(defrecord CommunicationConnection [middleware]
  SockjsConnection
  (on-open [this session]
    (swap! open-sockets conj session)
    (send! session {:type :msg :content "welcome"})
    (handle-response
     session
     (middleware (assoc session :type :on-open))))
  (on-message [this session msg]
    (handle-response session
                     (middleware (-> session
                                     (assoc :type :on-message)
                                     (assoc :message msg)))))
  (on-close [this session]
    (handle-response session
                     (middleware (assoc session :type :on-close)))
    (swap! open-sockets #(remove (partial = session) %))))

(defn all-sockets
  "return all open sockets. Can be used for broadcasting."
  []
  @open-sockets)

(defn methojure-handler [& {:keys [route middleware]
                            :as options}]
  (sockjs/sockjs-handler
   (or route (or route "/sockjs"))
   (->CommunicationConnection (or middleware (fn [] nil)))
   (dissoc options :route :middleware)))
