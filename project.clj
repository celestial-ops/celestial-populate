(defproject populous "0.1.0-SNAPSHOT"
  :description "A tool for populating Celestial from edn files"
  :url "https://github.com/celestial-ops/celestial-populate"
  :license {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  
  :dependencies [
     [cheshire "5.4.0"]
     [slingshot "0.10.3" ]
     [org.clojure/core.incubator "0.1.3"]
     [clj-http "1.1.0"]
     [com.taoensso/timbre "2.6.3"]
     [org.clojure/clojure "1.6.0"]
  ]

  :main populous.core
)
