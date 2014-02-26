(ns dartbot.udp
  (:use [clojure.java.shell :only [sh]]))

(defn broadcast [msg]
  (future (sh "socat" "-" "UDP-DATAGRAM:255.255.255.255:5000,broadcast" :in (str msg "\n"))
    (println "BROADCAST:" msg)))