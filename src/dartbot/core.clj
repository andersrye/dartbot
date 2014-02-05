(ns dartbot.core)


(defn read-input []
  (print "Please enter something: ")
  (let [input (read-line)]
    (println "You entered")
    input)
  )

(defn parse-int [string]
  (read-string (re-find  #"\d+" string ))
  )

(defn parse-data [string]
  (let [[id points multi ts] (map parse-int (clojure.string/split string #";"))]
    (hash-map :id id :points points :multiplier multi :timestamp ts))
  )

(defn print-data [data]
  (println (str "Id: b" (:id data)))
  (println "Points: " (* (:points data) (:multiplier data)))
  (println "Timestamp:" (:timestamp data))
  )

(defn print-throw [data]
  (case (:multiplier data )
    1 (print (:points data))
    2 (print "DOUBLE" (:points data))
    3 (print "TRIPLE!" (:points data))
    )
  (println " ( =" (* (:points data) (:multiplier data)) ")")
  )

(defn total-points [throws]
  )

(defn -main []
  (println "Game started")
  (loop [line (read-line)]
    (let [data (parse-data line)]
    (if (= "stop" data)
      "stopped"
      (do
        (print-throw data)
        (recur (read-line))
        )))))
;
;
;
;(def world1
;  (atom {
;    :game1 {
;      :boards ["board1"]
;      :players {
;                 :ary {
;                        :position nil
;                        :score 123
;                        :throws 5
;                        :history [60 1 1 1 1 1 1]
;                        }
;                 }
;      }
;
;    })
;  )
;
;
;(defn handle-message [message world]
;
;  (let [changes (get-changes message world)]
;    (update world changes))
;    world
;  )
