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

(ns populous.common
  (:require 
    [clojure.core.strint :refer (<<)]
    [taoensso.timbre :as timbre]
    [cheshire.core :refer :all]
    [slingshot.slingshot :refer  [throw+ try+]]
    [clj-http.client :as client])
 )

(timbre/refer-timbre)

(def defaults {:insecure? true :throw-exceptions false})

(defn call [verb root api args]
  (let [{:keys [body error status] :as resp} (verb (str root api) (merge args defaults))]
    (when (= status 401)
      (throw+ {:type ::un-autorized}))
    (when-not (= status 200) 
      (throw+ (merge (parse-string body true) {:type ::call-failed})))
    (parse-string body true)))

(defn update-user [u root auth]
  (call client/put root "/users" {:form-params {:user u} :basic-auth auth :content-type :json}))

(defn add-user 
  "Adding a user" 
  ([{:keys [username] :as u} root auth]
    (call client/post root "/users" {:form-params u :basic-auth auth :content-type :json}))
  ([u root auth up]
   (try+
     (add-user u root auth)
     (info "added user" u)
     (catch [:type ::call-failed] {:keys [object]}
       (when (and (= :celestial.persistency.users/conflicting-user (keyword (:type object))) up)
         (update-user u root auth)
         (info "updated user" u))))))

(defn get-types 
   "grab all types" 
   [root auth]
  (call client/get root "/types" {:basic-auth auth :content-type :json}))

(defn get-type 
   "grab a type" 
   [t root auth]
  (call client/get root (<< "/types/~{t}") {:basic-auth auth :content-type :json}))

(defn delete-type 
   "delete a type" 
   [t root auth]
  (call client/delete root (<< "/types/~{t}") {:basic-auth auth :content-type :json}))

(defn update-type 
  [t root auth]
  (call client/put root "/types" {:form-params t :basic-auth auth :content-type :json}))

(defn add-type
  ([t root auth]
   (call client/post root "/types" {:form-params t :basic-auth auth :content-type :json}))
  ([t root auth up]
   (try+ 
     (add-type t root auth)
     (info "added type" t)
     (catch [:type ::call-failed] {:keys [object]}
       (when (and (= :celestial.persistency.types/conflicting-type  (keyword (:type object))) up)
         (update-type t root auth up)
         (info "updated type" t))))))

(defn action-by-name [action operates-on root auth ]
  (first 
    (filter (fn [[k {:keys [name] :as v}]] (= name action))
      (call client/get root (str "/actions/type/" operates-on) {:basic-auth auth :content-type :json}))))

(defn get-actions
   "grab all actions for type" 
   [root auth type]
  (call client/get root (str "/actions/type/" type) {:basic-auth auth :content-type :json}))


(defn update-action 
  [root auth id a]
  (call client/put root (str "/actions/" id) {:form-params a :basic-auth auth :content-type :json}))

(defn add-action
  ([a root auth]
   (call client/post root "/actions" 
      {:form-params a :basic-auth auth :content-type :json}))
  ([{:keys [operates-on name] :as a} root auth up]
   (try+ 
     (add-action a root auth)
     (info "added action" a)
     (catch [:type ::call-failed] {:keys [object]}
       (when (and (= :celestial.persistency.actions/duplicated-action (keyword (:type object))) up)
         (let [[id _] (action-by-name name operates-on root auth)]
           (update-action root auth (Integer. (clojure.core/name id)) a) 
           (info "updated action" a)))))))

(defn get-systems 
   [t root auth]
   (call client/get root (<< "/systems/type/~{t}") {:basic-auth auth :content-type :json}))

(defn get-system
   [id root auth]
   (call client/get root (<< "/systems/~{id}") {:basic-auth auth :content-type :json}))

(defn update-system
   [id s root auth]
   (call client/put root (<< "/systems/~{id}") {:form-params s :basic-auth auth :content-type :json})
  )
