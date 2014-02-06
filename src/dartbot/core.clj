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

(def start-msg "START;1391449631516;GID1;BID1;BNO,YNS,HEN")
(def next-msg "NEXT;1391449631516;GID1;HEN")
(def throw-msg "THROW;1391449631516;BID1;20;3")

(defn parse-message [string]
  (let [msg (clojure.string/split (clojure.string/lower-case string) #";")
        command (keyword (first msg))]
    (case command
      :start (let [[ts gid bid plrs] (rest msg)] {:command command, :payload {:timestamp (parse-int ts), :gid (keyword gid), :bid (keyword bid), :players (map keyword (clojure.string/split plrs #","))}})
      :next (let [[ts gid plr] (rest msg)] {:command command, :payload {:timestamp (parse-int ts), :gid (keyword gid), :player (keyword plr)}})
      :throw (let [[ts bid scr mlt] (rest msg)] {:command command, :payload {:timestamp (parse-int ts), :bid (keyword bid), :score (parse-int scr), :multiplier (parse-int mlt)}})
      nil)
    ))

(defn print-data [payload]
  (println (str "Id: b" (:id payload)))
  (println "Points: " (* (:score payload) (:multiplier payload)))
  (println "Timestamp:" (:timestamp payload))
  )

(defn print-throw [payload]
  (case (:multiplier payload )
    1 (print (:points payload))
    2 (print "DOUBLE" (:points payload))
    3 (print "TRIPLE!" (:points payload))
    )
  (println " ( =" (* (:points data) (:multiplier data)) ")")
  )

(defn total-points [throws]
  )

(defn make-game [])

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
