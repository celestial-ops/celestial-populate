(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns populous.core
  "A tool for populating Celestial with data"
  (:gen-class true)
  (:require 
    [clojure.edn :as edn]
    [clojure.java.io :refer [file]]
    [clojure.core.strint :refer (<<)]
    [taoensso.timbre :as timbre]
    [cheshire.core :refer :all]
    [slingshot.slingshot :refer  [throw+ try+]]
    [org.httpkit.client :as client]))

(timbre/refer-timbre)

(defn call [verb root api args auth-headers]
  (let [{:keys [body error status] :as res} @(verb (<< "~(root)~{api}") (merge args {:headers auth-headers}))]
  (when-not (= status 200) 
    (throw+ (assoc res :type ::call-failed)))
  (:data (parse-string body true))))

(defn add-user 
   "Adding a user" 
   [u]
   (info "adding user" u)
  )

(defn add-type 
   "Adding a type" 
   [t]
   (info "adding type" t)
  )

(defmulti add (fn [m] (keys m)))
(defmethod add [:puppet-std :type :classes] [m] (add-type m))
(defmethod add [:username :password :envs :roles :operations] [m] (add-user m))
(defmethod add :default [m] (info "nothing to add for" m))

(defn data [path]
  (map (comp edn/read-string slurp) (filter #(.isFile %) (file-seq path))))

(defn -main 
  "import files from path matching expected structure"
  [path & args]
   (doseq [item (data (file path))] (add item)))


(clojure.pprint/pprint (data (file "data/example")))
(-main "data/example")
