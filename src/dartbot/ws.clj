(ns dartbot.ws
  (:use [compojure.core :only (defroutes GET)]
        ring.util.response
        ring.middleware.cors
        org.httpkit.server)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]
            [clojure.data.json :as json]
            ))

(def clients (atom {}))
(def response-handler (atom (fn [data] "hello")))

(defn ws
  [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))
    (on-receive con (fn [data]
                          (send! con (@response-handler data) )))))

(defn ws-send [string]
  (doseq [client @clients]
    (send! (key client)string
      false))
  )

(defn ws-send-data [data]
  (ws-send (json/write-str data)))

(defn ws-generate-response [data]
  (json/write-str data))

(defroutes routes
  (GET "/dartbot" [] ws))

(def application (-> (handler/site routes)
                   reload/wrap-reload
                   (wrap-cors
                     :access-control-allow-origin #".+")))

(defn start [r-handler]
  (reset! response-handler r-handler)
  (run-server application {:port 8080 :join? false}))
