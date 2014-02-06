(ns dartbot.test.core
  (:use midje.sweet
        [clojure.test :only [with-test]]
        dartbot.core))

(fact
  "parse int"
  (parse-int "b12") => 12
  (parse-int "987654321987654321") => 987654321987654321)

(fact
  (parse-message "START;1391449631516;GID1;BID1;BNO,YNS,HEN") => {:command :start, :payload {:timestamp 1391449631516, :gid :gid1, :bid :bid1, :players [:bno :yns :hen]}}
  (parse-message "START;1391449732816;GID2;BID1;BNO,YNS,HEN,ary") => {:command :start, :payload {:timestamp 1391449732816, :gid :gid2, :bid :bid1, :players [:bno :yns :hen :ary]}}
  (parse-message "NEXT;1391449631516;GID1;HEN") => {:command :next, :payload {:timestamp 1391449631516, :gid :gid1, :player :hen}}
  (parse-message "THROW;1391449631516;BID1;20;3") => {:command :throw, :payload {:timestamp 1391449631516, :bid :bid1, :score 20, :multiplier 3}}
  (parse-message "324rhyu34k") => nil
  )

(def test-start-msg {:command :start, :payload {:timestamp 1391449732816, :gid :gid2, :bid :bid1, :players [:bno :yns :hen :ary]}})

(fact
  (make-game test-start-msg) => {:gid2
                                 {:timestamp 1391449732816
                                  :currentplayer nil
                                  :currentthrows []
                                  :boards ["bid1"]
                                  :players {:bno
                                            {:order 1
                                             :position nil
                                             :score 301
                                             :throws 0
                                             :history []}
                                            :yns
                                            {:order 2
                                             :position nil
                                             :score 301
                                             :throws 0
                                             :history []}
                                            :hen
                                            {:order 3
                                             :position nil
                                             :score 301
                                             :throws 0
                                             :history []}
                                            :ary
                                            {:order 4
                                             :position nil
                                             :score 301
                                             :throws 0
                                             :history []}

                                             }}})