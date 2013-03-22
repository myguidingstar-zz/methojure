(ns methojure.communication.core
  (:use [methojure.sockjs.session :only [send! closed?]])
  (:require [clojure.edn :as edn]))

;; EDN format

(defn wrap-edn [handler]
  (fn [session]
    (if (contains? session :message)
      (handler (update-in session [:message] edn/read-string))
      (handler session))))

;; Actions

(def registered-actions (atom {}))

(defmacro defaction [fn-name params* & body]
  `(do
     (defn ~fn-name ~params* ~@body)
     (swap! registered-actions assoc
            ~(keyword (str *ns* "/" fn-name)) ~fn-name)))

(defn error [code reason & [details]]
  {:type :error
   :code code
   :reason reason
   :details details})

(defn handle-action [session]
  (let [{:keys [name args id]} (:message session)]
    (try
      (if-let [f (@registered-actions name)]
        {:type :action
         :id id
         :result (apply f session args)}
        (error 404 (str "action not found: " (str name))))
      (catch Exception e
        (.printStackTrace e)
        (error 500 (str "action " (str name) "has thrown an exception")
               (.getMessage e))))))

(defn wrap-action [handler]
  (fn [session]
    (if (= :action (get-in session [:message :type]))
      (handle-action session)
      (handler session))))

;; PubSub

(defn publishable? [x]
  (and (instance? clojure.lang.IDeref x) (instance? clojure.lang.IRef x)))

(def channels (atom {}))
(def subscriptions (atom {}))

(defmacro defchannel [fn-name params* & body]
  `(do
     (defn ~fn-name ~params*
       {:post [publishable?]}
       ~@body)
     (swap! channels assoc
            ~(keyword (str *ns* "/" fn-name)) ~fn-name)))

(defn- unsubscribe! [ch-name id sub-id]
  (if-let [ch (get-in subscriptions [ch-name id sub-id])]
    (do
      (ch)
      (swap! subscriptions assoc-in [ch-name id sub-id] nil)
      true)
    false))

(defn- publish [session ch-name]
  (fn [[sid sub-id] ref old new]
    (if (closed? session)
      ;; remove subscription if session is closed.
      (unsubscribe! ch-name sid sub-id)
      ;; send message to client.
      (let [msg {:type :publish
                 :topic ch-name
                 :id sub-id
                 :old old
                 :new new}]
        (send! session {:type :msg :content (pr-str msg)})))))

(defaction subscribe [{:keys [id] :as session} ch-name sub-id & args]
  (if-let [ch-fn (@channels ch-name)]
    (let [ch (apply ch-fn session args)
          unsub-fn (fn [] (remove-watch ch [id sub-id]))]
      (swap! subscriptions assoc-in [ch-name id sub-id] unsub-fn)
      (add-watch ch [id sub-id] (publish session ch-name))
      {:type :subscribed
       :id sub-id
       :value @ch})
    (error 404 (str "topic not found: " name))))

(defaction unsubscribe [{:keys [id] :as session} name sub-id]
  (when-not (unsubscribe! name id sub-id)
    (error 404 (str "this subscription does not exist: " name sub-id))))

(def empty-handler (fn [session] nil))

(defn wrap-communication [handler]
  (-> handler
      (wrap-action)
      (wrap-edn)))
