(ns methojure.communication.stream
  (:require [methojure.sockjs.core :as sockjs]
            [crate.binding :as subatom]))

;; timeouts
(def connection-timeout 10000)
(def heatbeat-timeout 60000)

;; connection ids
(def *id* (atom 0))
(defn next-id [] (swap! *id* inc))

;; existing connections (also closed/unused connections)
(def *connections* (atom {}))

(defn- create-connection
  "creates a new connection"
  [url]
  (let [id (next-id)
        data {::id id  ;; the connection id
              :url url ;; the connection url
              :socket nil ;; the sockjs socket
              :ready-state :connecting ;; :connecting, :connected, :waiting
              :connection-timer nil ;; called after initialization
              :retry-timer nil ;; retries to open the connection later on
              :retry-count 0 ;; amount of retries
              :force-disconnect false ;; if conn was disconnected by client
              }]
    (swap! *connections* assoc id data)
    (subatom/subatom *connections* id)))

(defn ->ready-state
  "return the connection state of the connection"
  [conn]
  (:ready-state @conn))

(defn- ready-state!
  "set a new ready state to the connection"
  [conn value]
  (subatom/sub-swap! conn :ready-state value))

(defn clear-timer [conn k]
  (when (k @conn)
    (js/clearTimeout (k @conn)))
  (subatom/sub-swap! conn dissoc k))

(defn- fire-listener [conn event-name & args]
  (doseq [f (get-in @conn [:listeners event-name] [])]
    (apply f conn args))
  conn)

(defn- register-listener [conn event-name f]
  (subatom/sub-swap! conn #(update-in % [:listeners event-name] conj f)))

(defn- connect
  "set ups all required connection information."
  [conn socket data]
  (clear-timer conn :connection-timer)
  (subatom/sub-swap! conn #(-> %
                               (assoc :ready-state :connected)
                               (assoc :retry-count 0)))
  (fire-listener conn :reset)
  conn)

(declare initialize-connection)

(defn- retry
  "retries to reopen the connection only if the user has not abort the
   connection by itsself."
  [conn]
  (if-not (:force-disconnect @conn)
    (do
      (subatom/sub-swap! conn #(-> %
                                   (update-in [:retry-count] inc)
                                   (assoc :ready-state :connecting)))
      (initialize-connection conn))
    conn))

(defn- retry-later
  "retries to open the connection to a later point."
  [conn]
  (clear-timer conn :retry-timer)
  (subatom/sub-swap!
   conn
   #(-> %
        (assoc :ready-state :waiting)
        (assoc :retry-timer (js/setTimeout
                             (fn [] (retry conn))
                             ;; TODO: exp timeout
                             (* 1000 (:retry-count @conn)))))))

(defn- lost-connection
  "we we lost the connection remove the socket and retry to open the
   connection again."
  [conn]
  (clear-timer conn :connection-timer)
  (clear-timer conn :heatbeat-timer)
  (sockjs/close! (:socket @conn))
  (subatom/sub-swap! conn #(assoc % :socket nil))
  (retry-later conn))

(defn- start-connection-timer
  "retries to open the connection again if it does not work the first time."
  [conn]
  (clear-timer conn :connection-timer)
  (subatom/sub-swap!
   conn assoc
   :connection-timer
   (js/setTimeout (fn [] (lost-connection conn)) connection-timeout)))

(defn- start-heatbeat-timer [conn]
  (when-not (:force-disconnect @conn)
    (clear-timer conn :heatbeat-timer)
    (subatom/sub-swap! conn #(assoc % :heatbeat-timer
                                    (js/setTimeout
                                     (fn []
                                       (lost-connection conn))
                                     heatbeat-timeout)))))

(defn- on-message
  "create the sockjs on-message handler"
  [conn socket]
  (fn [data]
    (start-heatbeat-timer conn)
    (condp = (->ready-state conn)
      :connecting (connect conn socket (.-data data))
      :connected (fire-listener conn :message (.-data data))
      (.log js/console "invalid state" (->ready-state conn)))))

(defn- on-close
  "create the sockjs on-close handler"
  [conn socket]
  (fn [] (lost-connection conn)))

(defn- initialize-connection
  "initializes a new sockjs connection."
  [conn]
  (let [socket (sockjs/create-sockjs-handler (:url @conn))]
    ;; register sockjs listener
    (sockjs/on-message socket (on-message conn socket))
    (sockjs/on-close socket (on-close conn socket))
    (sockjs/on-heatbeat socket (fn [] (start-heatbeat-timer conn)))
    ;; retry to open a connection if it does not work the first time.
    (start-connection-timer conn)
    ;; return the connection
    (subatom/sub-swap! conn assoc :socket socket)
    conn))

;; here starts the public API

(defn create-stream
  "creates a new connection stream. returns the connection."
  [url]
  (initialize-connection (create-connection url)))

(defn connected?
  "returns true if we succesfully connected to the server"
  [conn]
  (= (->ready-state conn) :connected))

(defn send!
  "sends a message to the server. if there is no connection messages
   will be dropped.
   TODO: Should we cache these meassage or let the user handle the case?"
  [conn msg]
  (when (connected? conn)
    (sockjs/send! (:socket @conn) msg)))

(defn close!
  "Closes a connection."
  [conn]
  (clear-timer conn :connection-timer)
  (clear-timer conn :retry-timer)
  (clear-timer conn :heatbeat-timer)
  (when (connected? conn)
    (sockjs/close! (:socket @conn)))
  (swap! conn #(-> %
                   (assoc :ready-state :closed)
                   (assoc :force-disconnect :true))))

(defn reconnect!
  "Reconnect to the server."
  [conn & {:keys [force]}]
  (if (and (connected? conn) force)
    (lost-connection conn)
    (do
      (when (= (->ready-state @conn) :connecting)
        (lost-connection conn))
      (clear-timer conn :retry-timer)
      (subatom/sub-swap! conn #(update-in % [:retry-count] dec))
      (retry conn))))

(defn on
  "registers a listeners. Currently available events:
   * :message is fired whenever a new message appears
   * :reset is fired whenever we are reconnected to the server"
  [conn trigger f]
  (when (contains? #{:message :reset} trigger)
    (register-listener conn trigger f)))

(defn status
  "returns the current status of the connection."
  [conn]
  (->ready-state conn))
