(ns dartbot.core)

(defn parse-int [string]
  (read-string (re-find #"\d+" string))
  )

(defn parse-message [string]
  (let [msg (clojure.string/split (clojure.string/lower-case string) #";")
        command (keyword (first msg))]
    (case command
      :start (let [[ts gid bid rules plrs] (rest msg)]
               {:command command, :gid (keyword gid), :payload {:timestamp (parse-int ts), :bid (keyword bid), :rules rules, :players (vec (map keyword (clojure.string/split plrs #",")))}})
      :next (let [[ts gid plr] (rest msg)]
              {:command command, :gid (keyword gid), :payload {:timestamp (parse-int ts), :player (keyword plr)}})
      :throw (let [[ts bid scr mlt] (rest msg)]
               {:command command, :bid (keyword bid), :payload {:timestamp (parse-int ts), :score (parse-int scr), :multiplier (parse-int mlt)}})
      nil)
    ))

(defn total-points [throws]
  (apply + (for [t throws] (* (:score t) (:multiplier t))))
  )

(defn add-throw [game payload]
  (if (< (count (:currentthrows game)) 3)
    (assoc game :currentthrows (conj (:currentthrows game) payload))
    game
    )
  )

(defn print-game [gid game]
  (println "GAME  " (name gid) "  -----------------------------")
  (println "TIMESTAMP:\t" (:timestamp game))
  (println "CURRENT PLAYER:\t" (:currentplayer game))
  (print "CURRENT THROWS:\t ")
  (doseq [t (:currentthrows game)] (print (str (case (:multiplier t) 2 "D" 3 "T" "") (:score t) " ")))
  (print "=" (total-points (:currentthrows game)))
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

(defn get-next-player [game]
  (let [current (:currentplayer game)
        players (:playerorder game)
        index (.indexOf players current)
        get-throws #(get-in game [:players % :throws])
        throws (map get-throws players)]

    ;    (println "current" current "players" players "index" index "throws" throws)
    (if (= current (last players)) ;last player in player order?
      (if (apply = throws) ;all players have same amount of throws?
        (first players) ;first player
        (apply first (filter #(< (second %) (get-throws current)) (map list players throws))) ;first player with fewer throws
        )
      (if (< (get-throws (nth players (inc index))) (get-throws current)) ;next player has fewer throws?
        (nth players (inc index)) ;next player in order is next
        (if (apply = throws) ;next player with fewer throws is next
          (if (empty? (:currentthrows game))
            (first players)
            (nth players (inc index)))
          (nth players (inc index)))
        )
      )

    ))

(defn set-next-player [game payload]
  (if (nil? (:player payload))
    (assoc game :currentplayer (get-next-player game))
    (assoc game :currentplayer (:player payload))
    ))

(defn update-field [game field fn data]
  (assoc-in game field (fn (get-in game field) data))
  )

(defn clear-current-throws [game]
  (assoc game :currentthrows []))

(defn update-score [game]
  (update-field game [:players (:currentplayer game) :score] - (total-points (:currentthrows game)))
  )

(defn update-throws [game]
  (update-field game [:players (:currentplayer game) :throws] + 3)
  )


(defn update-history [game payload]
  (let [miss {:timestamp (:timestamp payload) :score 0 :multiplier 1}]
    (if (< (count (:currentthrows game)) 3)
      (update-field game [:players (:currentplayer game) :history] conj (take 3 (concat (:currentthrows game) [miss miss miss])))
      (update-field game [:players (:currentplayer game) :history] conj (:currentthrows game))

      )))

;TODO: unfuck
(defn finish-round [game payload]
  (if (empty? (:currentthrows game))
    (set-next-player game payload)
    (-> game
      update-score
      update-throws
      (update-history payload)
      clear-current-throws
      (set-next-player payload)
      )
    )
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

(def world-atom (atom {}))

(add-watch world-atom :watch-change (fn [key a old-val new-val]
                                      (spit "test.tmp"  new-val)))

(defn load-backup []
  (if-let [has-backup (.exists (java.io.File. "test.tmp"))]
    (read-string (slurp "test.tmp"))
    {}
    ))

(defn -main []
  (println "Dartbot started, waiting for messages.")
  (loop [world (load-backup) line (read-line)]
    (let [message (parse-message line)]
      (recur (reset! world-atom (print-world (update-world world message))) (read-line))
      )))