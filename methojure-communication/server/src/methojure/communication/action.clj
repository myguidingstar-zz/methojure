(ns methojure.communication.action)

(def registered-actions (atom {}))

(defmacro defaction [fn-name params* & body]
  `(do
     (defn ~fn-name ~params* ~@body)
     (swap! registered-actions assoc ~(keyword fn-name) ~fn-name)))

(defn error [code reason & [details]]
  {:type :error
   :code code
   :reason reason
   :details details})

(defn handle-action [session]
  (let [{:keys [action args]} (:message session)]
    (if-not (action-exits? action)
      (error 404 (str "action not found: " action))
      (try
        (if-let [f (action-name @registered-actions)]
          (apply f session args)
          (error 404 (str "action not found: " (str action))))
        (catch Exception e
          (error 500 (str "action " (str action) "has thrown an exception")
                 (.getMessage e)))))))

(defn wrap-action [handler]
  (fn [session]
    (if (= :action (get-in session [:message :type]))
      (handle-action session)
      (handler session))))
