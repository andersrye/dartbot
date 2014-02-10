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
      :start (let [[ts gid bid plrs] (rest msg)]
               {:command command, :gid (keyword gid), :payload {:timestamp (parse-int ts), :bid  (keyword bid), :players (map keyword (clojure.string/split plrs #","))}})
      :next (let [[ts gid plr] (rest msg)]
              {:command command, :gid (keyword gid), :payload {:timestamp (parse-int ts), :player (keyword plr)}})
      :throw (let [[ts bid scr mlt] (rest msg)]
               {:command command, :bid (keyword bid), :payload {:timestamp (parse-int ts), :score (parse-int scr), :multiplier (parse-int mlt)}})
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
  (println " ( =" (* (:points payload) (:multiplier payload)) ")")
  )

(defn total-points [throws]
  (apply + (for [t throws] (* (:score t) (:multiplier t))))
  )



(defn add-throw [game payload]
  (if (< (count (:currentthrows game)) 3)
    (assoc game :currentthrows (conj (:currentthrows game) payload))
    game
    )
  )

(defn get-next-player [game]
  (let [curr (:currentplayer game)
        plrs (:playerorder game)
        idx (.indexOf plrs curr)
        get-thws #(get-in game [:players % :throws])
        thws (map get-thws plrs)]
    (if (= curr (last plrs))                                            ;last player in player order?
      (if (apply = thws)                                                ;all players with have same amount of throws?
        (first plrs)                                                    ;first player
        (apply first (filter #(< (second %) (get-thws curr)) (map list plrs thws))) ;first player with fewer throws
        )
      (if (< (get-thws (get plrs (inc idx))) (get-thws curr))                                                       ;next player has fewer throws?
        (get plrs (inc idx))                                                 ;next player in order is next
        (if true                                                            ;next player with fewer throws is next
        ()  ;TODO fix this
        ())
        )
      )

    ))

(defn print-game [gid game]
  (println "GAME  " (name gid) "  -----------------------------")
  (println "TIMESTAMP:\t" (:timestamp game))
  (println "CURRENT PLAYER:\t" (name (:currentplayer game)))
  (print "CURRENT THROWS:\t ")
  (doseq [t (:currentthrows game)] (print (str (case (:multiplier t) 2 "D" 3 "T" "") (:score t) " ")))
  (println "\nPLAYER ORDER:\t" (vec (map name (:playerorder game))))
  (println "BOARDS:\t\t" (:boards game))
  (println "PLAYERS:")
  (doseq [[k p] (:players game)]
    (println "\t" (name k))
    (println "\t\t" "POSITION:\t" (:position p))
    (println "\t\t" "SCORE:\t\t" (:score p))
    (println "\t\t" "THROWS:\t" (:throws p))
    (print "\t\t" "HISTORY:\t ")
    (doseq [thws (:history p)] (print "(") (doseq [t thws] (print (str (case (:multiplier t) 2 "D" 3 "T" "") (:score t) " "))) (print ") "))
    (print "\n"))
  (println "\n")
  )

(defn print-world [world]
  (println "CURRENT GAMES: =================================\n")
  (doseq [[gid game] world]
    (print-game gid game)
    )
  (println "END GAMES ===============================\n\n")
  world)

(defn make-game [payload gid]
  {gid {:timestamp (:timestamp payload)
                     :currentplayer (first (:players payload))
                     :currentthrows []
                     :playerorder (:players payload)
                     :boards #{(:bid payload)}
                     :players (into {} (for [p (:players payload)]
                                         [p {:position nil
                                             :score 301
                                             :throws 0
                                             :history []}]))

                     }})

(defn update-game [world gid fn payload]
  (assoc world gid (fn (gid world) payload))
  )

(defn set-next-player [game payload]
  (if (nil? (:player payload))
    (assoc game :currentplayer nil) ;TODO (get-next-player game)
    (assoc game :currentplayer (:player payload))
    ))

(defn update-field [game field fn data]
  (assoc-in game field (fn (get-in game field) data))
  ;[:players :ary :score] - 10
  )

(defn clear-current-throws [game]
  (assoc game :currentthrows []))

(defn update-score [game]
  (update-field game [:players (:currentplayer game) :score] - (total-points (:currentthrows game)))
  )

(defn update-throws [game]
  (update-field game [:players (:currentplayer game) :throws] + 3)
  )

(defn update-history [game]
  (update-field game [:players (:currentplayer game) :history] conj (:currentthrows game))
  )

;TODO: unfuck
(defn finish-round [game payload]
  (-> game
    update-score
    update-throws
    update-history
    clear-current-throws
    (set-next-player payload)
    )

;  ((comp
;    #(assoc % :currentthrows [])
;    #(update-field % [:players (:currentplayer %) :score] - (total-points (:currentthrows %)))
;    #(update-field % [:players (:currentplayer %) :throws] + 3)
;    #(update-field % [:players (:currentplayer %) :history] conj (:currentthrows %))
;    ) game)
  )

(defn find-game [board-id world]
  (loop [w world]
    (if (empty? w)
      nil
      (if (contains? (get (val (first w)) :boards) board-id)
      (key (first w))
      (recur (rest w)))
      )
    )
  )

(defn update-world [world {:keys [command bid gid payload]}]
  (case command
    :start (into world (make-game payload gid))
    :next (update-game world gid finish-round payload)
    :throw (update-game world (find-game bid world) add-throw payload)
    (do (println "Unknown command, ignoring.") world))
  )

(defn -main []
  (println "Dartbot started, waiting for messages.")
  (loop [world {} line (read-line)]
    (let [message (parse-message line)]
      (recur (print-world (update-world world message)) (read-line))
      )))

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
