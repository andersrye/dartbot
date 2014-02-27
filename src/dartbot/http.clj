(ns dartbot.http
  (:require [cheshire.core :refer :all]
            [org.httpkit.client :as http]))

(defn upload-game [gid game]
  (let [options {:headers {"Accept" "application/json"
                          "Content-Type" "application/json"}
                :body (generate-string game)}]
  @(http/post (str "http://dart-mongo.herokuapp.com/dart/" gid) options)))
