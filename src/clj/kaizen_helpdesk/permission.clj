(ns kaizen-helpdesk.permission
  (:require [clojure.core.async :refer [go <!!]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.data.db :as db]
            [kaizen-helpdesk.qual :as qual]))

;; TODO Read permissions from DB in pages and process them async

(defn check-permission
  "Checks if permission satisfies given request"
  [request permission]
  (log/debug "checking permission" permission)
  (log/debug "parse perm" (qual/request-qual-parse (:qualification permission)))
  (log/debug "eval perm" (qual/request-qual-evaluate (:qualification permission)
                                                     request))
  (qual/request-qual-evaluate (:qualification permission)
                              request))

(defn check-permissions
  [request permissions]
  (log/debug "check-permissions" request)
  (if (first (filter #(check-permission request %) permissions))
    (or (:db request) [])
    false))

(defn has-permission?
  "Entry point to permission module.
  
  Takes in a request and returns the request if a matching permission
  is found

  If no permission is found, an exception is thrown"
  [request]
  (log/debug "has-permissions?" request)
  (let [perm-rules (db/get-permissions {:entity ((comp name :entity) request)
                                        :user-id ((comp :id :identity) request)})
        match-perm (if (and (not (map? (:db request)))
                            (> (count (:db request)) 1))
                     (reduce #(if (check-permissions (assoc request :db %2) perm-rules)
                                (conj % %2)
                                (conj % :no-permission))
                             [] (:db request))
                     (check-permissions request perm-rules))
        _ (log/debug "match-perm" match-perm)]
    (if match-perm
      (assoc request :db match-perm)
      (throw (ex-info "Not enough permission"
                      {:causes "not enough permission"})))))
