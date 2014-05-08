(defproject dartbot "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.stuartsierra/lazytest "1.2.3"]
                 [midje "1.5.0"]
                 [ring/ring-json "0.2.0"]
                 [http-kit "2.0.0"]
                 [ring/ring-devel "1.1.8"]
                 [compojure "1.1.5"]
                 [ring-cors "0.1.0"]
                 [org.clojure/data.json "0.2.4"]]

  :min-lein-version "2.0.0"
  :properties {:project.build.sourceEncoding "UTF-8"}

  :repositories {"stuart" "http://stuartsierra.com/maven2"}

  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]}}

  :aot :all
  :main dartbot.core
  )
