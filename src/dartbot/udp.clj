(ns dartbot.udp
  (:import [java.net DatagramSocket Inet4Address DatagramPacket MulticastSocket NetworkInterface])
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

(defn get-ip []
  (flatten (for [n (enumeration-seq (java.net.NetworkInterface/getNetworkInterfaces)) :when (not-empty (enumeration-seq (.getInetAddresses n)))]
    (for [a (enumeration-seq (.getInetAddresses n)) :when (instance? Inet4Address a)]
      (str (.getHostAddress a))
      ))))

(defn broadcast-ip []
  (for [ip (get-ip)]
    (when (not= ip "127.0.0.1")
      (broadcast ip))))
