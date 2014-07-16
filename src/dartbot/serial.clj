(ns dartbot.serial
  (:import (j.extensions.comm SerialComm)))

(defn get-serial-port [name]
  (loop [ports (SerialComm/getCommPorts)]
    (when (not-empty ports)
      (if (= name (.getDescriptivePortName (first ports)))
        (first ports)
        (recur (rest ports))))))

(defn read-bytes [port bytes]
  (let [buffer (byte-array bytes)]
    (.readBytes port buffer bytes)
    buffer))

(defn read-throws [port]
  (when (<= 3 (.bytesAvailable port))
    (let [byte-buffer (read-bytes port (.bytesAvailable port))
          string (apply str (map char byte-buffer))]
      (clojure.string/split string #"\r\n"))))

(defn read-str [port]
  (when (not= 0 (.bytesAvailable port))
    (let [byte-buffer (read-bytes port (.bytesAvailable port))]
      (apply str (map char byte-buffer)))))

(defn make-throw-message [thrw]
  (str "THROW;" (System/currentTimeMillis) ";bid1;" thrw ""))

(defn start [handler]
  (let [port (get-serial-port "VCP0")]
    (if-not port
      (println "Serial port not found. Is the USB connected?")
      (do
        (println "Serial monitor started: " (.openPort port))
        (loop [buf (read-str port)]
          (if (and buf (= \newline (last buf)))
            (do
              (doseq [thrw (clojure.string/split buf #"\r\n")]
                (handler (make-throw-message thrw)))
              (recur (read-str port)))
            (do
              (Thread/sleep 5)
              (recur (str buf (read-str port))))))))))

