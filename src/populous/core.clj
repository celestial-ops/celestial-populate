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
    [populous.common :refer (add-type add-user add-action)]
    [clojure.edn :as edn]
    [clojure.java.io :refer [file]]
    [taoensso.timbre :as timbre]
    ))

(timbre/refer-timbre)

(defmulti add (fn [root auth m up] (keys m)))
(defmethod add [:puppet-std :type :classes :description] [root auth m up] 
  (add-type m root auth up))
(defmethod add [:username :password :envs :roles :operations] [root auth u up] 
  (add-user u root auth up))
(defmethod add [:operates-on :src :capistrano :timeout :name :description] [root auth a up] 
  (add-action a root auth up))
(defmethod add :default [root auth m] (info "nothing to add for" m))

(defn data [path]
  (map (comp edn/read-string slurp) (filter #(.isFile %) (file-seq path))))

(defn -main 
  "import files from path matching expected structure"
  [path conf & args]
   (let [{:keys [host user password update]} (edn/read-string (slurp conf))]
     (doseq [item (doall (data (file path)))] 
       (add host [user password ] item update)))
    (info "done adding items")
    (System/exit 0))




  
