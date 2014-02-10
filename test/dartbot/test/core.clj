(ns dartbot.test.core
  (:use midje.sweet
        [clojure.test :only [with-test]]
        dartbot.core))

(def new-payload {:timestamp 1391449732816, :gid :gid2, :bid :bid1, :players [:bno :yns :hen :ary]})
(def next-payload {:timestamp 1391449631516, :player :yns})
(def throw-payload {:timestamp 1391449631516, :score 20, :multiplier 3})

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn mock-world [& opts]
  (let [default {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :bno
                  :currentthrows []
                  :playerorder [:bno :yns :hen :ary]
                  :boards #{:bid1}
                  :players {:bno
                            {:position nil
                             :score 301
                             :throws 0
                             :history []}
                            :yns
                            {:position nil
                             :score 301
                             :throws 0
                             :history []}
                            :hen
                            {:position nil
                             :score 301
                             :throws 0
                             :history []}
                            :ary
                            {:position nil
                             :score 301
                             :throws 0
                             :history []}
                            }}}]
    (if (empty? opts)
      default
      (deep-merge default (first opts)))
    ))

(defn mock-game [& opts]
  (if (empty? opts)
    (:gid2 (mock-world))
    (deep-merge (:gid2 (mock-world)) (first opts))
    )
  )

;; TESTS

(fact
  "parse int"
  (parse-int "b12") => 12
  (parse-int "987654321987654321") => 987654321987654321)

(fact
  (parse-message "START;1391449631516;GID1;BID1;BNO,YNS,HEN") => {:command :start, :gid :gid1, :payload {:timestamp 1391449631516, :bid :bid1, :players [:bno :yns :hen]}}
  (parse-message "START;1391449732816;GID2;BID1;BNO,YNS,HEN,ary") => {:command :start, :gid :gid2, :payload {:timestamp 1391449732816, :bid :bid1, :players [:bno :yns :hen :ary]}}
  (parse-message "NEXT;1391449631516;GID1;HEN") => {:command :next, :gid :gid1, :payload {:timestamp 1391449631516, :player :hen}}
  (parse-message "NEXT;1391449631516;GID1") => {:command :next, :gid :gid1, :payload {:timestamp 1391449631516, :player nil}}
  (parse-message "THROW;1391449631516;BID1;20;3") => {:command :throw, :bid :bid1, :payload {:timestamp 1391449631516, :score 20, :multiplier 3}}
  (parse-message "324rhyu34k") => nil
  )

(fact "make game"
  (make-game new-payload :gid2) => (mock-world))

;(fact "get next player"
;  (get-next-player (mock-game {:players {:bno {:throws 6} :yns {:throws 3} :hen {:throws 6} :ary {:throws 6}}})) => :yns
;  (get-next-player (mock-game {:currentplayer :yns :players {:bno {:throws 6} :yns {:throws 3} :hen {:throws 6} :ary {:throws 6}}})) => :bno
;  (get-next-player (mock-game {:currentplayer :hen :players {:bno {:throws 6} :yns {:throws 3} :hen {:throws 6} :ary {:throws 6}}})) => :ary
;  (get-next-player (mock-game {:currentplayer :ary :players {:bno {:throws 6} :yns {:throws 3} :hen {:throws 6} :ary {:throws 6}}})) => :yns
;  )

(fact "set next player"
  (set-next-player (mock-game) {:timestamp 1391449631516, :gid :gid1, :player :hen}) => (mock-game {:currentplayer :hen})
  )

(fact "")

(fact "find game by board-id"
  (find-game :bid1 (mock-world)) => :gid2
  )

(fact "finish round"
  (finish-round (mock-game {:currentthrows [throw-payload throw-payload throw-payload]}) {:timestamp 1391449631516, :player :hen})
  => (mock-game {:currentplayer :hen :players {:bno {:score 121 :throws 3 :history [[throw-payload throw-payload throw-payload]]}}})
  )

(fact "update game: next"
  (update-game (mock-world) :gid2 set-next-player next-payload) => (mock-world {:gid2 {:currentplayer :yns}})
  )

(fact "update game: throw"
  (update-game (mock-world) :gid2 add-throw throw-payload) => (mock-world {:gid2 {:currentthrows [{:timestamp 1391449631516, :score 20, :multiplier 3}]}})
  )

(fact "update world"
  (update-world {} {:command :start, :gid :gid2, :payload {:timestamp 1391449732816, :bid :bid1, :players [:bno :yns :hen :ary]}})
  => (mock-world)
  (update-world (mock-world {:gid2 {:currentthrows [throw-payload throw-payload throw-payload]}}) {:command :next, :gid :gid2, :payload {:timestamp 1391449631516, :player :hen}})
  => (mock-world {:gid2 {:currentplayer :hen :players {:bno {:score 121 :throws 3 :history [[throw-payload throw-payload throw-payload]]}}}})
  (update-world (mock-world) {:command :throw, :bid :bid1, :payload {:timestamp 1391449631516, :score 20, :multiplier 3}})
  => (mock-world {:gid2 {:currentthrows [throw-payload]}})
  )