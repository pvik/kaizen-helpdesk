(ns kaizen-helpdesk.routes
  (:require [compojure.core     :refer [defroutes context routes GET POST PUT DELETE]]
            [compojure.route    :refer [not-found files resources]]
            [ring.util.response :refer [response]]
            [taoensso.timbre    :as    log]
            [kaizen-helpdesk.auth :as auth]
            [kaizen-helpdesk.api :as api]))

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
          {:keys
           [limit page]} (:params request)
          lim            (if limit (Integer/parseInt limit) nil)
          pg             (if page (Integer/parseInt page) nil)
          api-request    {:api-op api-op
                          :identity (:identity request)
                          :paginate {:limit lim :page pg}
                          :payload (:body request)}
          r           (assoc request :api-request api-request)
          _ (log/debug "api-request:" api-request)]
      (handler r))))

(defroutes api-routes
  (context "/api" [_ :as {api-request :api-request}]
           (PUT "/ticket" []
                (api/process (assoc api-request
                                    :entity :ticket)))
           (GET "/ticket/:id{[0-9]+}" [id]
                (api/process (assoc api-request
                                    :entity  :ticket
                                    :payload {:id (Integer/parseInt id)})))
           (GET "/ticket" [qual]
                (api/process (assoc api-request
                                    :entity  :ticket
                                    :payload {:qual qual})))
           (GET "/tickets" [list style qual limit page]
                (log/info "Tickets:" list style qual limit page))))

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
                   {:pattern #"^/api/.*"
                    :handler auth/authenticated-access}])

(def app-routes (routes api-routes
                        gen-routes))

