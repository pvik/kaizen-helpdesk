(ns kaizen-helpdesk.core
  (:require [compojure.handler    :as handler]
            [ring.middleware.json :refer [wrap-json-response
                                          ;;wrap-json-params
                                          wrap-json-body]]
            ;;[ring.middleware.format :refer [wrap-restful-format]]
            ;;[ring.middleware.cookies :refer [wrap-cookies]]
            ;;[ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.routes  :refer [app-routes
                                             access-rules
                                             wrap-api-request]]
            [kaizen-helpdesk.auth  :refer [auth-backend]])
  (:gen-class))

;; initialize stuff here
(defonce _
  (do
    (log/merge-config! (eval (clojure.edn/read-string
                              (slurp "resources/logging.edn"))))
    (log/info "Initializing Kaizen Help Desk")
    nil))


(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn access-error
  [request value]
  {:status 403
   :headers {}
   :body "Not authorized"})

(defn wrap-debug [handler lbl]
  (fn [request]
    (log/debug lbl " -> " request "\n")
    (handler request)))

(def app (as-> #'app-routes $
           (wrap-debug $ "After wrap-api-request: ")
           (wrap-api-request $)
           (wrap-access-rules $ {:rules access-rules :on-error access-error})
           (wrap-authorization $ auth-backend)
           (wrap-authentication $ auth-backend)
           (wrap-json-body $ {:keywords? true})
           (wrap-json-response $)
           (handler/site $)))

