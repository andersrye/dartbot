(ns dartbot.udp
  (:import [java.net DatagramSocket InetAddress DatagramPacket MulticastSocket])
  (:use [clojure.java.shell :only [sh]]))

(def socket (atom nil))
(def default-port 5000)

(defn broadcast [msg]
  (when @socket
    (let [message (DatagramPacket. (byte-array (map (comp byte int) msg)) (count msg) (InetAddress/getByName "255.255.255.255") default-port)]
      (.send @socket message 1000000)
      )))

(defn listen []
  (when @socket
    (let [packet (DatagramPacket. (byte-array 1024) 1024)]
      (.receive @socket packet)
      (String.  (.getData packet) 0 (.getLength packet)))))

(defn udp-listener [handler]
  (loop [msg (listen)]
    (do (handler msg) (recur (listen)))))

(defn start
  ([handler] (start handler default-port))
  ([handler port]
    (reset! socket (MulticastSocket. port))
    (future (udp-listener handler))))

(defn broadcast-ip []
  (broadcast (.getHostAddress (InetAddress/getLocalHost)))
  ;(sh )
  )
