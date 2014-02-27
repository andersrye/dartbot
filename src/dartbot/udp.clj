(ns dartbot.udp
  (:import [java.net DatagramSocket InetAddress DatagramPacket MulticastSocket]))

(def socket (atom nil))

(defn start
  ([] (start 5000))
  ([port] (reset! socket (MulticastSocket. port))))

(defn listen []
  (when @socket
    (let [packet (DatagramPacket. (byte-array 1024) 1024)]
      (.receive @socket packet)
      (String.  (.getData packet) 0 (.getLength packet)))))

(defn broadcast [msg]
  (when @socket
    (let [message (DatagramPacket. (byte-array (map (comp byte int) msg)) (count msg) (InetAddress/getByName "255.255.255.255") 5000)]
      (.send @socket message 1000000)
      )))

