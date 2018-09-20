(ns kaizen-helpdesk.api
  (:require [ring.util.response :refer [response]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [go <!!]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.data.db :as db]
            [kaizen-helpdesk.permission :as perm]
            [kaizen-helpdesk.qual :as qual]
            [kaizen-helpdesk.helpers :as h]))

(defonce ^:const ticket-id-props
  (h/get-ticket-id-fields))

(defn wrap-response [resp]
  {:status 200 :body resp})

(defn is-admin? [request]
  (= "admin" ((comp :type :identity) request)))

(defn get-db-entity [request]
  (log/debug "getting DB entity" (:entity request) "-" (:payload request))
  (if (= (:api-op request) :create)
    (assoc request :db nil)
    (let [payload   (:payload request)
          entity    (:entity request)
          paginate  (:paginate request)
          where-str (cond (:id payload)   (str "(id = " (:id payload) ")")
                          (:qual payload) (:qual payload)
                          :else (throw (ex-info "invalid read operation"
                                                {:cause "no ID or qualification specified"})))
          _         (log/debug where-str)
          where     (qual/query-qual-evaluate where-str)
          _         (log/debug where)
          entity    (db/get-entity entity where paginate)]
      (if (<= (count entity) 1)
        (assoc request :db (first entity))
        (assoc request :db entity)))))

(defn create-op [request]
  (log/debug "create entity" (:entity request) "request by"
             ((comp :user :identity) request) "->" request)
  (let [user-id ((comp :id :identity) request)
        entity  (:entity request)
        data    (assoc (:payload request)
                       :created-by-id user-id
                       :updated-by-id user-id)
        _ (log/debug data)]
    (db/create-entity entity data)))

;; read-op function not needed, data read in with get-db-entity
;; will be used as output of read-op

(defn update-op [request]
  (log/debug "update entity" (:entity request) "request by"
             ((comp :user :identity) request) "->" request)
  (let [user-id ((comp :id :identity) request)
        entity  (:entity request)
        data    (assoc (:payload request)
                       :created-by-id user-id
                       :updated-by-id user-id)
        _ (log/debug data)]
    (db/update-entity entity data)))

(defn delete-op [request]
  (log/debug "delete entity" (:entity request) "request by"
             ((comp :user :identity) request) "->" request)
  (let [user-id ((comp :id :identity) request)
        entity  (:entity request)
        id      ((comp :id :payload) request)]
    (if id
      (db/delete-entity entity id)
      (throw (ex-info "invalid operation"
                      {:cause "no id specified to delete"})))))

(defn exec-api-op [request]
  (log/debug "exec-api-op")
  (let [api-op (:api-op request)]
    (log/debug "api-op:" api-op)
    (cond
      (= api-op :create) (create-op request)
      (= api-op :read)   (:db request)
      (= api-op :update) (update-op request)
      :else (throw (ex-info "invalid operation"
                            {:cause "use valid HTTP method"})))))

(defn exec-db-fn [fn-name & fn-vals]
  (log/debug "exec db fn:" fn-name "; vals:" fn-vals)
  (apply
   (resolve
    (symbol (str (the-ns 'kaizen-helpdesk.data.db)) fn-name))
   fn-vals))

(defn has-permission? [request]
  (if (is-admin? request)
    request
    (perm/has-permission? request)))

(defn process
  "start api process pipeline
  request -> permissions -> validations -> actions -> exec (db) -> resp (notify, audit)"
  [request]
  (log/debug "process api request ->" request)
  (try (-> request
           get-db-entity
           has-permission?
           exec-api-op
           wrap-response)
       (catch Exception e
         (log/error "api error" (.getMessage e))
         {:status 500 :body {:message (.getMessage e)
                             :info    (ex-data e)}})))



