(ns kaizen-helpdesk.permission
  (:require [clojure.core.async :refer [go <!!]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.data.db :as db]
            [kaizen-helpdesk.qual :as qual]))

;; TODO Read permissions from DB in pages and process them async

(defn check-permission
  "Checks if permission satisfies given request"
  [permission request]
  (log/debug "Checking permission" permission)
  (log/debug "parse perm" (qual/request-qual-parse (:qualification permission)))
  (log/debug "eval perm" (qual/request-qual-evaluate (:qualification permission)
                                                     request))
  (qual/request-qual-evaluate (:qualification permission)
                              request))

(defn has-permission?
  "Entry point to permission module.
  
  Takes in a request and returns the request if a matching permission
  is found

  If no permission is found, an exception is thrown"
  [request]
  (log/debug "has-permissions?" request)
  (let [perm-rules (db/get-permissions {:entity ((comp name :entity) request)
                                        :user-id ((comp :id :identity) request)})
        match-perm (first (filter #(check-permission % request) perm-rules))]
    (if match-perm
      (assoc request :permission match-perm)
      (throw (ex-info "Not enough permission"
                      {:causes "not enough permission"})))))
