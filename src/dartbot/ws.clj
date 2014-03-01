(ns dartbot.ws
  (:use [compojure.core :only (defroutes GET)]
        ring.util.response
        ring.middleware.cors
        org.httpkit.server
        [clojure.java.shell :only [sh]
        ;clojure.pprint
        ]
        )
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]
            [org.httpkit.client :as http]
            ))

(def clients (atom {}))
(def response-handler (atom (fn [data sid] "hello")))

(defn new-client [con]
  {:conn con
   :game "none" ;all, gid123, none
   :update "none" ;all, diff, messages, none
   })

(defn ws [req]
  (let [sid (str "sid" (System/currentTimeMillis))]
  (with-channel req con
    (swap! clients assoc sid (new-client con))
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc sid)
                    (println con " disconnected. status: " status)))
    (on-receive con (fn [data]
                      (send! con (@response-handler sid data)))))))

(defn update-client [sid params]
  (swap! clients assoc sid (merge (get @clients sid) params))
  (prn @clients)
  )

(defn send-update [type data]
  (doseq [[_ client] @clients]
    (when (= (:update client) type)
      (send! (:conn client) (generate-string data)))))
;
;(defn ws-send [string]
;  (doseq [client @clients]
;    (send! (:conn (val client)) string
;      false))
;  )
;
;(defn ws-send-data [data]
;  (ws-send (generate-string data))
;  data)

(defn ws-generate-response [data]
  ;(prn data)
  (generate-string data))

(defroutes routes
  (GET "/dartbot" [] ws))

(def application (-> (handler/site routes)
                   reload/wrap-reload
                   (wrap-cors
                     :access-control-allow-origin #".+")))

(defn start [r-handler]
  (reset! response-handler r-handler)
  (run-server application {:port 8080 :join? false}))

;(defn pp [s] (clojure.pprint/pprint s))
