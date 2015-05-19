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
    [clj-http.client :as client]))

(timbre/refer-timbre)

(def defaults {:insecure? true :throw-exceptions false})

(defn call [verb root api args]
  (let [{:keys [body error status] :as resp} (verb (str root api) (merge args defaults))
         resp-body (parse-string body true)]
    (when (= status 401)
      (throw+ {:type ::un-autorized}))
    (when-not (= status 200) 
      (throw+ (merge resp-body {:type ::call-failed})))
    resp-body
    ))

(defn add-user 
   "Adding a user" 
   [{:keys [username] :as u} root auth up]
    (try+
       (call client/post root "/users" {:form-params u :basic-auth auth :content-type :json})
       (info "added user" u)
      (catch [:type ::call-failed] {:keys [object]}
        (when (and (= :celestial.persistency.users/conflicting-user (keyword (:type object))) up)
          (info "updated user" u)
          (call client/put root "/users" {:form-params u :basic-auth auth :content-type :json}))))
      )

(defn add-type 
   "Adding a type" 
   [t root auth up]
    (try+ 
      (call client/post root "/types" {:form-params t :basic-auth auth :content-type :json})
      (info "added type" t)
      (catch [:type ::call-failed] {:keys [object]}
        (when (and (= :celestial.persistency.types/conflicting-type  (keyword (:type object))) up)
          (info "updated type" t)
          (call client/put root "/types" {:form-params t :basic-auth auth :content-type :json})))))

(defn action-by-name [action operates-on root auth ]
  (first 
    (filter (fn [[k {:keys [name] :as v}]] (= name action))
      (call client/get root (str "/actions/type/" operates-on) {:basic-auth auth :content-type :json}))))

(defn add-action
   "Adding an action" 
   [{:keys [operates-on name] :as a} root auth up]
    (try+ 
      (call client/post root "/actions" {:form-params a :basic-auth auth :content-type :json})
      (info "added action" a)
      (catch [:type ::call-failed] {:keys [object]}
        (when (and (= :celestial.persistency.actions/duplicated-action (keyword (:type object))) up)
          (let [[id _] (action-by-name name operates-on root auth)]
            (call client/put root (str "/actions/" (Integer. (clojure.core/name id))) {:form-params a :basic-auth auth :content-type :json})
            (info "updated action" a))))))

(defmulti add (fn [root auth m up] (keys m)))
(defmethod add [:puppet-std :type :classes :description] [root auth m up] (add-type m root auth up))
(defmethod add [:username :password :envs :roles :operations] [root auth m up] (add-user m root auth up))
(defmethod add [:operates-on :src :capistrano :timeout :name :description] [root auth m up] (add-action m root auth up))
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




  
