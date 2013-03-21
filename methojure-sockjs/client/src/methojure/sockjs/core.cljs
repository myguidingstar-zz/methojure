(ns methojure.sockjs.core)

(defn create-sockjs-handler [url & [whitelist options]]
  (js/SockJS. url (clj->js whitelist) (clj->js options)))

(defn on-open [handler f]
  (set! (.-onopen handler) f)
  handler)

(defn on-message [handler f]
  (set! (.-onmessage handler) f)
  handler)

(defn on-close [handler f]
  (set! (.-onclose handler) f)
  handler)

(defn on-error [handler f]
  (set! (.-onerror handler) f)
  handler)

(defn on-heatbeat [handler f]
  (set! (.-onheatbeat handler) f)
  handler)

(defn send! [handler msg]
  (.send handler msg))

(defn close! [handler & [code reason]]
  (.close handler code reason))
