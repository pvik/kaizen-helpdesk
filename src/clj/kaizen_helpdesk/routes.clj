(ns kaizen-helpdesk.routes
  (:require [compojure.core     :refer [defroutes context routes GET POST PUT DELETE]]
            [compojure.route    :refer [not-found files resources]]
            [ring.util.response :refer [response]]
            [buddy.auth.accessrules :refer [error]]
            [clout.core         :as    clout]
            [taoensso.timbre    :as    log]
            [kaizen-helpdesk.auth :as  auth]
            [kaizen-helpdesk.api :as   api]))

(defonce admin-entity
  #{"user-type" "user-detail" "user-auth" "user-group" "user-group-membership"
    "ticket-custom-field"
    "permission-rule" "permission-group" "permission-group-member" "permission-assignment" "permission-group-assignment"
    })

(defonce request-method-api-op-map
  {:put    :create
   :get    :read
   :post   :update
   :delete :delete})

(defn wrap-api-request
  "Used in ring handler to generate api-request object"
  [handler]
  (fn [request]
    (let [api-op         ((:request-method request) request-method-api-op-map)
          is-admin?      (api/is-admin? request)
          {:keys
           [limit page qual]} (:params request)
          lim            (if limit (Integer/parseInt limit) nil)
          pg             (if page (Integer/parseInt page) nil)
          {:keys
           [entity id]}  (#(or
                            (clout/route-matches "/api/admin/:entity" %)
                            (clout/route-matches "/api/admin/:entity/:id{[0-9]+}" %)
                            (clout/route-matches "/api/:entity" %)
                            (clout/route-matches "/api/:entity/:id{[0-9]+}" %))
                          request)
          api-req        {:api-op    api-op
                          :is-admin? is-admin?
                          :identity  (:identity request)
                          :paginate  {:limit lim :page pg}
                          ;; :payload  (:body request)
                          :entity   entity}
          api-req2       (cond
                           id    (assoc api-req :payload {:id (Integer/parseInt id)})
                           qual  (assoc api-req :payload {:qual qual})
                           :else (assoc api-req :payload (:body request)))
          r              (assoc request :api-request api-req2)
          _              (log/debug "api-request:" api-req2)]
      (if (and (contains? admin-entity entity) (not is-admin?))
        (error "requires admin privileges")
        (handler r)))))

(defn entity-routes [api-request]
  (routes
   ;; READ
   (GET "/:entity" [] (api/process api-request)) 
   (GET "/:entity/:id{[0-9]+}" [id]
        (api/process api-request))
   ;; CREATE
   (PUT "/:entity" [] (api/process api-request))
   ;; UPDATE
   (POST "/:entity" [] (api/process api-request))))

(defroutes api-routes
  (context "/api" [_ :as {api-request :api-request}]
           
           (GET "/is-admin" []
                (api/wrap-response {:is-admin? (:is-admin? api-request)}))
           
           (context "" []
                    (entity-routes api-request))
           (context "/admin" []
                    (entity-routes api-request))))

(defroutes gen-routes 
  (GET "/" [] "Hello from Compojure!")  ;; for testing only
  (GET "/login" [] auth/auth-handler)
  (files "/" {:root "target"})          ;; to serve static resources
  (resources "/" {:root "target"})      ;; to serve anything else
  (not-found "404 Page Not Found"))     ;; page not found

(defn any-access [_]
  true)

(def access-rules [{:pattern #"^/login$"
                    :handler any-access}
                   {:pattern #"^/api/admin.*"
                    :handler auth/admin-access}
                   {:pattern #"^/api/.*"
                    :handler auth/authenticated-access}])

(def app-routes (routes api-routes
                        gen-routes))

