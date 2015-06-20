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

(ns populous.migrate
  (:require 
    [clojure.edn :as edn]
    [clojure.java.io :refer [file]]
    [populous.common :refer :all]
    [taoensso.timbre :as timbre]
    )
  )

(timbre/refer-timbre)

(defn is-normalized? [t]
  (every? #{:puppet-std :type :description} (keys t)))

(defn normalize 
   "convert type into new form" 
   [{:keys [description classes puppet-std type]}]
   (info "normalizing, insert env for" type)
   (let [env (keyword (read-line)) {:keys [args module]} puppet-std] {
      :puppet-std {
        env { :module module :args args :classes classes }
       }
      :type type
      :description description
    }))

(defn merger [f s] (if (map? f) (merge f s) s))

(defn merge-type 
   "merges type into another" 
   [t dest root auth]
    (if (is-normalized? dest)
      (merge-with merger (normalize t)  dest) 
      (merge-with merger (normalize t) (normalize dest))))

(defn update-system-types
   "updated systems with dest type (in case of fold)" 
   [source dest root auth]
   (doseq [id (:ids (get-systems source root auth)) :let [s (get-system id root auth)]]
      (update-system id (assoc s :type dest) root auth)))

(defn types-2
  "2.0 types migration"
  [root auth]
  (doseq [{:keys [type description] :as t}  (:types (get-types root auth))]
    (when-not (is-normalized? t)
      (info type "-" description "isn't migrated, insert either f (fold), n (norm), s (skip)")  
      (case (keyword (read-line))
        :f
        (do (info "insert folding dest")
            (let [dest (get-type (read-line) root auth)]
              (update-type (merge-type t dest root auth) root auth)
              (delete-type type root auth)
              (update-system-types type (:type dest) root auth)))
        :n (update-type (normalize t) root auth)
        :s identity
        ))))

(defn actions-2 
   "2.0 actions migrations"
   [root auth r env]
  (let [ts (:types (get-types root auth)) 
        actions (reduce (fn [m v] (merge m (get-actions root auth (:type v)))) {} ts)] 
    (doall (doseq [[id a] actions :let [{:keys [args]} (a r) src (a :src)]]
        (update-action root auth (Integer. (name id)) 
           (-> a (dissoc r) (dissoc :provided) (dissoc :timeout) 
             (assoc r {env {:timeout (a :timeout) :args args}})))
      ))
    )
  )

(defn -main 
  [conf & args]
  (let [{:keys [host user password]} (edn/read-string (slurp conf))]
    (types-2 host [user password])
    (System/exit 0)))
