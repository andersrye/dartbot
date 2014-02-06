(defproject dartbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.stuartsierra/lazytest "1.2.3"]
                 [midje "1.5.0"]]

  :min-lein-version "2.0.0"
  :properties {:project.build.sourceEncoding "UTF-8"}

  :repositories {"stuart" "http://stuartsierra.com/maven2"}

  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]}}

  :main dartbot.core)
