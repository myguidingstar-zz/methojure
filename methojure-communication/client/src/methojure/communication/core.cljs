(ns methojure.communication.core
  (:require [methojure.communication.stream :as stream]
            [cljs.reader :as reader]))

;; id generation
(def id (atom 0))
(defn- next-id [] (swap! id inc))

;; Middleware
(def ^:dynamic *default-stream* nil)

(defn methojure-stream [url middleware]
  (let [conn (stream/initialize-connection (stream/create-connection url))]
    (set! *default-stream* conn)
    (stream/on conn :reset (fn [] (middleware {:event :reset})))
    (stream/on conn :message
               (fn [session msg]
                 (middleware (merge @session
                                    {:event :message :message msg}))))))

;; EDN reader

(defn wrap-edn
  "parse each message as edn"
  [handler]
  (fn [event]
    (handler (if (= :message (:event event))
               (update-in event [:message] reader/read-string)
               event))))

(defn wrap-log-errors
  "logs each error to the console."
  [handler]
  (fn [event]
    (when (and (= (:event event) :message)
               (= :error (-> event :message :type)))
      (let [{:keys [code reason details]} (:message event)]
        (.warn js/console code reason details)))
    (handler event)))

;; Action middleware

(def callbacks (atom {}))

(def msg-buffer (atom []))

(defn- on-action-message [session msg]
  (let [f (@callbacks (:id msg))]
    (when f
      (f (:result msg)))
    (swap! callbacks dissoc (:id msg))))

(declare send-msg)

(defn- on-action-reset []
  (when-not (empty? @msg-buffer)
    (doseq [m @msg-buffer]
      (send-msg m))
    (reset! msg-buffer [])))

(defn wrap-action [handler]
  (fn [event]
    (condp = (:event event)
      :reset (on-action-reset)
      :message (when (= :action (-> event :message :type))
                 (on-action-message event (:message event))))
    (handler event)))

;; Action API

(defn send-msg [msg]
  (stream/send! *default-stream* (pr-str msg)))

(defn call-action [f name & args]
  (let [id (next-id)
        msg {:type :action
             :id id
             :name name
             :args args}]
    (swap! callbacks assoc id f)
    (if-not (stream/connected? *default-stream*)
      (swap! msg-buffer conj msg)
      (send-msg msg))))


;; PubSub

(def subscription-callbacks (atom {}))

(defn- on-publish-message
  "call the publish callback"
  [session msg]
  (when (= :publish (:type msg))
    (when-let [[_ f] (get-in @subscription-callbacks [(:topic msg) (:id msg)])]
      (f (:old msg) (:new msg)))))

(defn- on-publish-reset
  "resubscribe to all channels when we reconnect. "
  []
  (doseq [[topic topic-subs] @subscription-callbacks]
    (doseq [[id [on-sub _]] topic-subs]
      (call-action on-sub :methojure.communication.core/subscribe topic id))))

(defn wrap-pubsub [handler]
  (fn [event]
    (condp = (:event event)
      :reset (on-publish-reset)
      :message (on-publish-message event (:message event)))
    (handler event)))

;; PubSub API

(defn subscribe [topic on-subscribe on-publish]
  (let [id (next-id)]
    (swap! subscription-callbacks assoc-in [topic id] [on-subscribe on-publish])
    (call-action on-subscribe
                 :methojure.communication.core/subscribe topic id)
    id))

(defn unsubscribe [topic id on-success]
  (swap! subscription-callbacks assoc-in [topic id] nil)
  (call-action on-success :methojure.communication.core/unsubscribe topic id))


;; default middleware

(def empty-handler (fn [event] nil))

(defn wrap-communication [handler]
  (-> handler
      (wrap-pubsub)
      (wrap-action)
      (wrap-edn)))

(def default-middleware
  (-> empty-handler
      (wrap-communication)))
