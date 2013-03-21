(ns methojure.communication.test.test-server
  (:use methojure.communication.core
        [compojure.core :only [defroutes GET POST DELETE ANY]]
        [compojure.handler :only [site]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.reload :only [wrap-reload]]
        org.httpkit.server)
  (:require [methojure.communication.stream :as stream]
            [compojure.route :as route]))

;; Simple actions

(defaction hello-world [session name]
  (str "hello, " name))

;; PubSub

(def cnt (atom 0))

(defchannel counter [session]
  cnt)

(defaction inc-counter [session]
  (swap! cnt inc))

;; Initialize everything

(defroutes all-routes
  (GET "/" [] "hello world")
  (stream/methojure-handler
   :middleware (-> empty-handler
                   (wrap-communication)))
  (route/not-found "page not found"))

(defn start-server []
  (run-server
   (-> all-routes
       (wrap-params)
       (wrap-reload)
       site)
   {:port 8081}))

(defn -main []
  (start-server)
  (println "Communication test server started."))
