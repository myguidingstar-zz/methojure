(ns methojure.communication.core
  (:require [methojure.communication.stream :as stream]
            [cljs.reader :as reader]))

(def callbacks (atom {}))

(defn- register-callback [id f]
  (swap! callbacks assoc id f))

;; store messages 
(def msg-buffer (atom []))

(defn- on-action-message [session msg]
  (let [f (@callbacks (:id msg))]
    (when f
      (f (:result msg)))
    (swap! callbacks dissoc (:id msg))))

(defn- on-publish-message [session msg]
  (when-let [f (@callbacks (:id msg))]
    (f (:old msg) (:new msg))))

(defn- on-error-message [session msg]
  (.warn js/console (:reason msg) (:details msg)))

(defn- on-message [session msg]
  (let [msg (reader/read-string msg)]
    (condp = (:type msg)
      :action (on-action-message session msg)
      :publish (on-publish-message session msg)
      :error (on-error-message session msg)
      nil)))

(declare send-msg)

(defn- on-reset []
  (when-not (empty? @msg-buffer)
    (doseq [m @msg-buffer]
      (send-msg m))
    (reset! msg-buffer [])))

(defn initial-stream []
  (let [s (stream/create-stream "/sockjs")]
    (stream/on s :reset on-reset)
    (stream/on s :message on-message)
    s))

(def ^:dynamic *default-stream* (initial-stream))

(def id (atom 0))
(defn- next-id [] (swap! id inc))

(defn send-msg [msg]
  (stream/send! *default-stream* (pr-str msg)))

(defn call-action [f name & args]
  (let [id (next-id)
        msg {:type :action
             :id id
             :name name
             :args args}]
    (register-callback id f)
    (if-not (stream/connected? *default-stream*)
      (swap! msg-buffer conj msg)
      (send-msg msg))))


;; PubSub

(defn subscribe [topic f]
  (let [id (next-id)
        sub-handler (fn [res]
                      ;; TODO: check if it was successfully and
                      ;; call with initial value
                      )]
    (register-callback id f)
    (call-action sub-handler :subscribe topic id)))
