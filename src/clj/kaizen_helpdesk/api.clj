(ns kaizen-helpdesk.api
  (:require [ring.util.response :refer [response]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [go <!!]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.data.db :as db]
            [kaizen-helpdesk.qual :as qual]))

;; TODO: retrieve all ticket columns as *-id from DB on startup
(defonce ^:const ticket-id-props
  #{:priority :status :assigned-to :updated-by :created-by})

(defn wrap-response [resp]
  {:status 200 :body resp})

(defn is-admin? [request]
  (= "admin" ((comp :type :identity) request)))

(defn gen-api-op-fn [request]
  (let [fn-name   (str (name (:api-op request)) "-" (name (:entity request)))]
    (log/debug "api-op-fn:" fn-name)
    fn-name))

(defn exec-api-op [request]
  (apply
   (resolve
    (symbol (str (the-ns 'kaizen-helpdesk.api)) (gen-api-op-fn request)))
   [request]))

(defn exec-db-fn [fn-name & fn-vals]
  (log/debug "fn:" fn-name "; vals:" fn-vals)
  (apply
   (resolve
    (symbol (str (the-ns 'kaizen-helpdesk.data.db)) fn-name))
   fn-vals))

(defn process
  "start api process pipeline
  request -> permissions -> validations -> actions -> db -> resp (notify, audit)"
  [request]
  (log/debug "process api request ->" request)
  (exec-api-op request))

(defn read-ticket [request]
  (log/debug "getting ticket" (:payload request))
  (let [payload   (:payload request)
        paginate  (:paginate request)
        where-str (cond (:id payload)   (str "(ticket-id = " (:id payload) ")")
                        (:qual payload) (:qual payload)
                        :else (throw (ex-info "Invalid Read Operation")))
        _ (log/debug where-str)
        where      (qual/query-qual-evaluate where-str)
        _ (log/debug where)
        ticket-chan (go (db/get-ticket-detail where paginate))]
    (wrap-response (<!! ticket-chan))))

(defn create-ticket [request]
  (log/debug "create ticket request by"
             ((comp :user :identity) request) "->" request)
  (let [user ((comp :user :identity) request)
        data (assoc (:payload request)
                    :created-by user
                    :updated-by user)
        payload (reduce (fn [p [k v]]
                          (if (contains? ticket-id-props k)
                            (assoc (dissoc p k)
                                   (keyword (str (name k) "-id"))
                                   (exec-db-fn (str "get-" (name k) "-id") v))
                            (assoc p k v)))
                        {} data)
        _ (log/debug payload)]
    (wrap-response (db/create-ticket payload))))


