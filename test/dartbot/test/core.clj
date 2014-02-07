(ns dartbot.test.core
  (:use midje.sweet
        [clojure.test :only [with-test]]
        dartbot.core))

(fact
  "parse int"
  (parse-int "b12") => 12
  (parse-int "987654321987654321") => 987654321987654321)

(fact
  (parse-message "START;1391449631516;GID1;BID1;BNO,YNS,HEN") => {:command :start, :gid :gid1, :payload {:timestamp 1391449631516, :bid #{:bid1}, :players [:bno :yns :hen]}}
  (parse-message "START;1391449732816;GID2;BID1;BNO,YNS,HEN,ary") => {:command :start, :gid :gid2, :payload {:timestamp 1391449732816, :bid #{:bid1}, :players [:bno :yns :hen :ary]}}
  (parse-message "NEXT;1391449631516;GID1;HEN") => {:command :next, :gid :gid1, :payload {:timestamp 1391449631516, :player :hen}}
  (parse-message "NEXT;1391449631516;GID1") => {:command :next, :gid :gid1, :payload {:timestamp 1391449631516, :player nil}}
  (parse-message "THROW;1391449631516;BID1;20;3") => {:command :throw, :bid :bid1, :payload {:timestamp 1391449631516, :score 20, :multiplier 3}}
  (parse-message "324rhyu34k") => nil
  )

(def test-payload {:timestamp 1391449732816, :gid :gid2, :bid "bid1", :players [:bno :yns :hen :ary]})

(defn mock-world [& opts]
  (let [default {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :bno
                  :currentthrows []
                  :playerorder [:bno :yns :hen :ary]
                  :boards ["bid1"]
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
  (merge-with into default (first opts))))


(mock-world {:players [:ank :yns :bno :ary] :currentPlayer :bno})

(def new-world {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :bno
                  :currentthrows []
                  :playerorder [:bno :yns :hen :ary]
                  :boards ["bid1"]
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

                            }}})



;(-> (mock-world)
;  (set-score {:ary [nil 122 6 []]})
;  (set-score {:ary [nil 122 6 []]})
;  (set-score {:ary [nil 122 6 []]})
;  (set-score {:yns [nil 132 7 []]})
;  (set-current-player :bno)
;  )


(def test-world {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :bno
                  :currentthrows []
                  :playerorder [:bno :yns :hen :ary]
                  :boards #{"bid1"}
                  :players {:bno
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :yns
                            {:position nil
                             :score 301
                             :throws 3
                             :history []}
                            :hen
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :ary
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}

                            }}})

(def test-world-throw {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :bno
                  :currentthrows [{:timestamp 1391449631516, :score 20, :multiplier 3}]
                  :playerorder [:bno :yns :hen :ary]
                  :boards #{"bid1"}
                  :players {:bno
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :yns
                            {:position nil
                             :score 301
                             :throws 3
                             :history []}
                            :hen
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :ary
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}

                            }}})

(def test-world-next {:gid2
                 {:timestamp 1391449732816
                  :currentplayer :yns
                  :currentthrows []
                  :playerorder [:bno :yns :hen :ary]
                  :boards #{"bid1"}
                  :players {:bno
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :yns
                            {:position nil
                             :score 301
                             :throws 3
                             :history []}
                            :hen
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}
                            :ary
                            {:position nil
                             :score 301
                             :throws 6
                             :history []}

                            }}})



(fact "make game"
  (make-game test-payload :gid2) => (mock-world))

(fact "get next player"
  (get-next-player (:gid2 test-world)) => :yns
  (get-next-player (:gid2 (assoc-in test-world [:gid2 :currentplayer] :yns))) => :bno
  (get-next-player (:gid2 (assoc-in test-world [:gid2 :currentplayer] :hen))) => :ary
  (get-next-player (:gid2 (assoc-in test-world [:gid2 :currentplayer] :ary))) => :yns
  )

(fact "next player"
  (next-player {:timestamp 1391449631516, :gid :gid1, :player :hen} (:gid2 test-world)) =>
                                  {:timestamp 1391449732816
                                   :currentplayer :yns
                                   :currentthrows []
                                   :boards ["bid1"]
                                   :players {:bno
                                             {:order 0
                                              :position nil
                                              :score 301
                                              :throws 0
                                              :history []}
                                             :yns
                                             {:order 1
                                              :position nil
                                              :score 301
                                              :throws 0
                                              :history []}
                                             :hen
                                             {:order 2
                                              :position nil
                                              :score 301
                                              :throws 0
                                              :history []}
                                             :ary
                                             {:order 3
                                              :position nil
                                              :score 301
                                              :throws 0
                                              :history []}

                                             }})

(def next-payload {:timestamp 1391449631516, :player :yns})
(def throw-payload {:timestamp 1391449631516, :score 20, :multiplier 3})

(fact "update game: next"
  (update-game test-world :gid2 next-player next-payload) => test-world-next)

(fact "update game: throw"
  (update-game test-world :gid2 add-throw throw-payload) => test-world-throw)