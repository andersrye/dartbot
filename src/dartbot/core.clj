(ns dartbot.core
  (:require [dartbot.ws :as ws]
            [clojure.data.json :as json]))

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
      :delete (let [[gid] (rest msg)]
                {:command command, :gid gid})
      nil)
    ))

(defn total-points [throws]
  (apply + (for [t throws] (* (:score t) (:multiplier t))))
  )

(defn add-throw [game payload]
  (if (< (count (:currentthrows game)) 3)
    (assoc game :currentthrows (conj (:currentthrows game) payload))
    game
    ))

(defn print-to-file [str]
  (spit "print-output.txt" str :append true)
  )

(defn print-world-to-file [world]
  (ws/ws-send-data world)
  (print-to-file
    (str
      "CURRENT GAMES: ==========================\n\n"
      (apply str
        (for [[gid game] world]
          (str
            "GAME  " (name gid) "  ---------------------\n"
            "TIMESTAMP:\t" (:timestamp game) "\n"
            "CURRENT PLAYER:\t" (:currentplayer game) "\n"
            "CURRENT THROWS:\t "
            (apply str
              (for [t (:currentthrows game)]
                (str (case (:multiplier t) 2 "D" 3 "T" "") (:score t) " ")))
            "= " (total-points (:currentthrows game)) "\n"
            "PLAYER ORDER:\t" (vec (map name (:playerorder game))) "\n"
            "BOARDS:\t\t" (:boards game) "\n"
            "PLAYERS:\n"
            (apply str
              (for [[k p] (:players game)]
                (str "\t" (name k) "\n"
                  "\t\t" "POSITION:\t" (:position p) "\n"
                  "\t\t" "SCORE:\t\t" (:score p) "\n"
                  "\t\t" "THROWS:\t\t" (:throws p) "\n"
                  "\t\t" "HISTORY:\t"
                  (apply str
                    (for [thws (:history p)]
                      (str "("
                        (apply str
                          (for [t thws]
                            (str (case (:multiplier t) 2 "D" 3 "T" "") (:score t) " "))) ") ")))
                  "\n")))
            "\n"
            )
          ))
      "END GAMES ===============================\n\n"
      )
    )
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



;TODO: rewrite w/ position
(defn get-next-player [game]
  (let [current (:currentplayer game)
        players (:playerorder game)
        index (.indexOf players current)
        get-throws #(get-in game [:players % :throws])
        throws (map get-throws players)]

    (if (= current (last players))
      (first players)
      (nth players (inc index)))
    ;    (println "current" current "players" players "index" index "throws" throws)
    ;    (if (= current (last players)) ;last player in player order?
    ;      (if (apply = throws) ;all players have same amount of throws?
    ;        (first players) ;first player
    ;        (apply first (filter #(< (second %) (get-throws current)) (map list players throws))) ;first player with fewer throws
    ;        )
    ;      (if (< (get-throws (nth players (inc index))) (get-throws current)) ;next player has fewer throws?
    ;        (nth players (inc index)) ;next player in order is next
    ;        (if (apply = throws) ;next player with fewer throws is next
    ;          (if (empty? (:currentthrows game))
    ;            (first players)
    ;            (nth players (inc index)))
    ;          (nth players (inc index)))
    ;        )
    ;      )

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
  (if-not (< (- (get-in game [:players (:currentplayer game) :score]) (total-points (:currentthrows game))) 0)
    (update-field game [:players (:currentplayer game) :score] - (total-points (:currentthrows game)))
    game
    ))

(defn update-throws [game]
  (update-field game [:players (:currentplayer game) :throws] + 3)
  )


(defn update-history [game payload]
  (let [miss {:timestamp (:timestamp payload) :score 0 :multiplier 1}]
    (if (< (count (:currentthrows game)) 3)
      (update-field game [:players (:currentplayer game) :history] conj (take 3 (concat (:currentthrows game) [miss miss miss])))
      (update-field game [:players (:currentplayer game) :history] conj (:currentthrows game))
      )))

(defn finish-round [game payload]
  (if (empty? (:currentthrows game))
    (set-next-player game payload)
    (-> game
      update-score
      update-throws
      (update-history payload)
      clear-current-throws
      (set-next-player payload)
      )))

(defn set-position [player game]
  (let [positions (map #(get-in game [:players % :position]) (:playerorder game))
        next-position (inc (count (remove nil? positions)))]
    (if (= (count (:playerorder game)) (inc next-position))
      (-> game
        (assoc-in [:players player :position] next-position)
        (assoc-in [:players (nth (:playerorder game) (.indexOf positions nil)) :position] (inc next-position)))
      (assoc-in game [:players player :position] next-position)
    )))

(defn find-game [board-id world]
  (last (for [[k v] world :when (contains? (:boards v) board-id)]
                             k))
;  (loop [w world]
;    (if (empty? w)
;      nil
;      (if (contains? (get (val (first w)) :boards) board-id)
;        (key (first w))
;        (recur (rest w)))
;      ))
  )

(defn delete-game [gid world]
  (dissoc world (keyword gid))
  )

(defn valid? [world {:keys [command bid gid payload]}]
  (case command
    :next (contains? world gid)
    :throw (contains? world (find-game bid world))
    :start true
    :delete true
    false))

(defn score-after-throw [world {:keys [command bid payload]}]
  (when (= command :throw)
    (let [gid (find-game bid world)
          game (gid world)]
      (- (get-in game [:players (:currentplayer game) :score]) (* (:score payload) (:multiplier payload)) (total-points (get-in world [gid :currentthrows])))
      )))

(defn bust? [world message]
  (if (= (:command message) :throw)
    (< (score-after-throw world message) 0)
    false
    ))

(defn win? [world message]
  (if (= (:command message) :throw)
    (= (score-after-throw world message) 0)
    false
    ))

(defn send-message [world message]

  (let [gid (if (nil? (:gid message)) (find-game (:bid message) world) (:gid message))
        currentplayer (name (get-in world [gid :currentplayer]))]
    (cond
      (bust? world message)
        (println (str "BUST;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer))
      (win? world message)
        (println (str "WIN;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer))
      :else
        (println (str "SCORE;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer ";" (score-after-throw world message)))
      ))
  world)

(defn update-game [world gid fn payload]
  (assoc world gid (fn (gid world) payload))
  )

(defn update-world [world {:keys [command bid gid payload] :as message}]
  (if-not (valid? world message)
    (do
      (print-to-file (str "ERROR: Invalid message. (" message ")\n\n"))
      world)
    (case command
      :start (into world (make-game payload gid))
      :next (do (send-message world message) (update-game world gid finish-round payload))
      :throw (do (send-message world message) (update-game world (find-game bid world) add-throw payload))
      :delete (delete-game gid world)
      (do (print-to-file "Unknown command, ignoring.") world))
    ))

(def world-atom (atom {}))

(add-watch world-atom :watch-change (fn [key a old-val new-val]
                                      (spit "test.tmp" new-val)))

(defn load-backup []
  (if-let [has-backup (.exists (java.io.File. "test.tmp"))]
    (read-string (slurp "test.tmp"))
    {}
    ))


(defn response-handler [data]
  (ws/ws-generate-response @world-atom))

(defn -main []
  (println "Dartbot started, waiting for messages.")
  (ws/start response-handler)
  (println "Websocket up")
  (reset! world-atom (load-backup))
  (loop [world (load-backup) line (read-line)]
    (let [message (parse-message line)]
      ;(ws/ws-send (str message))
      (recur (reset! world-atom (print-world-to-file (update-world world message))) (read-line))
      )))