(defproject celestial-populate "0.1.0-SNAPSHOT"
  :description "A tool for populating Celestial from files"
  :url "https://github.com/celestial-ops/celestial-populate"
  :license {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  
  :dependencies [
     [slingshot "0.10.3" ]
     [http-kit "2.1.16"]
     [com.taoensso/timbre "2.6.3"]
     [org.clojure/clojure "1.6.0"]
  ]

  :main celestial.populate
)
