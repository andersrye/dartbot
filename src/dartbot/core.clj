(ns dartbot.core
  (:import [java.net InetAddress]
           [java.io File])
  (:require [dartbot.ws :as ws]
            [dartbot.udp :as udp]
            [dartbot.http :as http]
            [dartbot.serial :as serial]
            [cheshire.core :refer :all]
            [clojure.data])
  (:gen-class))

(defn parse-int [string]
  (cond
    (number? string) string
    (nil? string) 0
    :else (read-string (re-find #"\d+" string))))

(defn parse-message [string]
  (let [msg (clojure.string/split (clojure.string/lower-case (clojure.string/trim string)) #";")
        command (first msg)]
    (case command
      "start" (let [[ts gid bid rules plrs] (rest msg)]
                {:command command, :gid gid, :payload {:timestamp (parse-int ts), :bid bid, :rules rules, :players (vec (clojure.string/split plrs #","))}})
      "next" (let [[ts gid plr] (rest msg)]
               {:command command, :gid gid, :payload {:timestamp (parse-int ts), :player plr}})
      "throw" (let [[ts bid scr mlt] (rest msg)]
                {:command command, :bid bid, :payload {:timestamp (parse-int ts), :score (parse-int scr), :multiplier (parse-int mlt)}})
      "delete" (let [[gid] (rest msg)]
                 {:command command, :gid gid})
      "hello?" {:command command}
      "undo" {:command command}
      nil)))

(defn total-points [throws]
  (apply + (for [t throws] (* (:score t) (:multiplier t)))))

(defn bust? [game]
  (< (- (get-in game [:players (:currentplayer game) :score]) (total-points (:currentthrows game))) 0)
  )

(defn win? [game]
  (= (- (get-in game [:players (:currentplayer game) :score]) (total-points (:currentthrows game))) 0)
  )

(defn add-throw [game payload]
  (if-not (or (bust? game) (win? game) (> (count (:currentthrows game)) 2))
    (assoc game :currentthrows (conj (:currentthrows game) payload))
    game
    ))

(defn print-to-file [str]
  (spit "print-output.txt" str :append true)
  )

(defn print-world-to-file [world]
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
  {gid {:timestamp (parse-int (:timestamp payload))
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
        players (remove #(and (get-in game [:players % :position]) (not (= current %))) (:playerorder game))
        index (.indexOf players current)
        get-throws #(get-in game [:players % :throws])
        throws (map get-throws players)
        next-player (if (= current (last players))
                      (first players)
                      (nth players (inc index)))]

    (if (= next-player current)
      nil
      next-player
      )))

(defn set-next-player [game payload]
  (if (nil? (:player payload))
    (assoc game :currentplayer (get-next-player game))
    (assoc game :currentplayer (:player payload))
    ))

(defn update-field [game field fn data]
  (assoc-in game field (fn (get-in game field) data))
  )

(defn set-field [a b]
  b)

(defn set-position [game player]
  (let [positions (map #(get-in game [:players % :position]) (:playerorder game))
        next-position (inc (count (remove nil? positions)))
        next-player (loop [order (:playerorder game)]
                      (if (and (not= (first order) player) (nil? (get-in game [:players (first order) :position])))
                        (first order)
                        (recur (rest order))))]
    (if (= (count (:playerorder game)) (inc next-position))
      (-> game
        (assoc-in [:players player :position] next-position)
        (assoc-in [:players next-player :position] (inc next-position))
        )
      (assoc-in game [:players player :position] next-position)
      )))

(defn clear-current-throws [game]
  (assoc game :currentthrows []))

(defn update-score [game]
  (let [new-score (- (get-in game [:players (:currentplayer game) :score]) (total-points (:currentthrows game)))]
    (if-not (< new-score 0)
      (if (= new-score 0)
        (-> game
          (update-field [:players (:currentplayer game) :score] set-field 0)
          (set-position (:currentplayer game)))
        (update-field game [:players (:currentplayer game) :score] - (total-points (:currentthrows game))))
      game
      )))

(defn update-throws [game]
  (update-field game [:players (:currentplayer game) :throws] + 3)
  )


(defn update-history [game payload]
  (let [miss {:timestamp (:timestamp payload) :score 0 :multiplier 1}]
    (if (and (< (count (:currentthrows game)) 3) (not (bust? game)) (not (win? game)))
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
  (dissoc world gid)
  )

(defn valid? [world {:keys [command bid gid payload]}]
  (case command
    "next" (or (contains? world gid) (contains? world (find-game bid world)))
    "throw" (contains? world (find-game bid world))
    "start" true
    "delete" true
    "end" true
    "hello?" true
    "undo" true
    false))

(defn score-after-throw [world {:keys [command bid payload]}]
  (when (= command :throw)
    (let [gid (find-game bid world)
          game (gid world)]
      (- (get-in game [:players (:currentplayer game) :score]) (* (:score payload) (:multiplier payload)) (total-points (get-in world [gid :currentthrows])))
      )))

(defn bust-after-throw?
  [world message]
  (if (= (:command message) :throw)
    (< (score-after-throw world message) 0)
    false
    ))

(defn win-after-throw? [world message]
  (if (= (:command message) :throw)
    (= (score-after-throw world message) 0)
    false
    ))

(defn send-message [world message]
  ;  (let [gid (if (nil? (:gid message)) (find-game (:bid message) world) (:gid message))
  ;        currentplayer (name (get-in world [gid :currentplayer]))]
  ;    (cond
  ;      (bust-after-throw? world message)
  ;      (udp/broadcast (str "BUST;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer))
  ;      (win-after-throw? world message)
  ;      (udp/broadcast (str "WIN;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer))
  ;      :else (udp/broadcast (str "SCORE;" (System/currentTimeMillis) ";" (name gid) ";" currentplayer ";" (score-after-throw world message)))
  ;      ))
  world)

(defn update-game [world gid fn payload]
  (assoc world gid (fn (get world gid) payload))
  )

(defn end-game [world gid]
  (http/upload-game gid (get world gid))
  (spit (str "games/" gid ".game") (get world gid))
  ;(delete-game gid world)
  world
  )

(def world-atom (atom {}))
(def undo-list (atom ()))

(defn undo []
  (if (not-empty @undo-list)
    (let [return (first @undo-list)]
      (reset! undo-list (rest @undo-list))
      return)
    @world-atom))

(add-watch world-atom :watch-change (fn [key a old-val new-val]
                                      (spit "world-data" new-val)
                                      ;(print-world-to-file new-val)
                                      (ws/send-update "all" new-val)
                                      (ws/send-update "diff" (first (clojure.data/diff new-val old-val)))
                                      ;(reset! undo-list (take 5 (conj @undo-list old-val)))

                                      ))

(defn update-world [world {:keys [command bid gid payload] :as message}]
  (if-not (valid? world message)
    (do
      (prn (str "ERROR: Invalid message. (" message ")\n\n"))
      world)
    (do
      (when (not= command "undo") (reset! undo-list (take 10 (conj @undo-list world))))
      (ws/send-update "messages" message)
      (case command
        "start" (into world (make-game payload gid))
        "next" (if gid
                 (update-game world gid finish-round payload)
                 (update-game world (find-game bid world) finish-round payload)
                 )
        "throw" (do (send-message world message) (update-game world (find-game bid world) add-throw payload))
        "delete" (delete-game gid world)
        "end" (end-game world gid)
        "hello?" (do (udp/broadcast-ip) world)
        "undo" (undo)
        (do (prn "Unknown command, ignoring.") world)
        ))))



(defn load-backup []
  (println "loading from" (.getAbsolutePath (File. "world-data")) ":" (.exists (File. "world-data")))
  (if (.exists (File. "world-data"))
    (let [backup (slurp "world-data")]
      (if (not= backup "")
        (read-string backup)
        {}))
    {}))


(defn request-handler [sid {:keys [game update] :as params}]
  (ws/update-client sid (dissoc params :command))
  (println "handling request")
  (when game
    (case game
      "all" (ws/ws-generate-response @world-atom)
      "archive" (ws/ws-generate-response (vec (for [f (.listFiles (File. (str "games/")))] (.getName f))))
      "none" nil
      (if (nil? (get @world-atom game))
        (when (.exists (File. (str "games/" game ".game")))
          (ws/ws-generate-response {game (read-string (slurp (str "games/" game ".game")))}))
        (ws/ws-generate-response {game (get @world-atom game)})))))

(defn response-handler [sid data]
  (let [msg (parse-string data true)
        cmd (:command msg)]
    (prn msg)
    (case cmd
      "request" (request-handler sid msg)
      (do (reset! world-atom (update-world @world-atom msg)) nil))))

(defn udp-handler [msg]
  (prn msg)
  (let [message (parse-message msg)]
    (when (valid? @world-atom message)
      (reset! world-atom (update-world @world-atom message)))))

(defn serial-handler [msg]
  (udp-handler msg))

(defn start-bot []
  (ws/start response-handler)
  (println "Websocket up")

  (udp/start udp-handler)
  (println "UDP listener up")

  (serial/start serial-handler)

  (reset! world-atom (load-backup))
  (println "World loaded")

  (println "Dartbot started, waiting for messages."))

(defn -main [& args]
  (start-bot))

;(start-bot)

