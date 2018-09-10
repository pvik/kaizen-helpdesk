(ns kaizen-helpdesk.data.db
  (:require [hikari-cp.core :refer :all]
            [clj-time.core     :as t]
            [clj-time.format   :as tf]
            [clj-time.coerce   :as tc]
            [clojure.string    :as str]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core     :as hsql]
            ;;[honeysql.helpers :refer :all :as helpers]
            [taoensso.timbre   :as log]
            [kaizen-helpdesk.data.query :as q]))

;; For Joda Objects to be used by JDBC
(require 'clj-time.jdbc)

(defonce ^:private db-options
  (:db (clojure.edn/read-string (slurp "resources/config.edn"))))

(defonce ^:private datasource-options
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10
   :pool-name          "kaizen-db-pool"
   :adapter            (:dbtype   db-options)
   :username           (:user     db-options)
   :password           (:password db-options)
   :database-name      (:dbname   db-options)
   :server-name        (:host     db-options)
   :port-number        (:port     db-options)
   :register-mbeans    false})

(defonce datasource
  (make-datasource datasource-options))

(defonce ^:private ^:const jdbc-operation-map
  {:query   jdbc/query
   :execute jdbc/execute!})

(defn- sanitize-cols-for-db [map]
  (reduce (fn [m [k v]]
            (if (.contains (name k) "-")
              (assoc m (keyword (str/replace (name k) #"-" "_")) v)
              (assoc m k v)))
          {} map))

(defn- sanitize-cols-from-db [map]
  (log/debug "sanitizing:" map)
  (reduce (fn [m [k v]]
            (if (.contains (name k) "_")
              (assoc m (keyword (str/replace (name k) #"_" "-")) v)
              (assoc m k v)))
          {} map))

(defn- run-sql [hsql-map operation]
  (log/debug hsql-map)
  (jdbc/with-db-connection [conn {:datasource datasource}]
    ((operation jdbc-operation-map) conn (hsql/format hsql-map)
     {:row-fn sanitize-cols-from-db})))

(defn- query [hsql-map]
  (run-sql hsql-map :query))

(defn- execute [hsql-map]
  (run-sql hsql-map :execute))

(defn- insert [hsql-map]
  (log/debug hsql-map)
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (jdbc/insert-multi! conn
                        (:insert-into hsql-map)
                        (map sanitize-cols-for-db (:values hsql-map)))))

;; schema functions

(defn get-table-fields [table]
  (first
   (jdbc/with-db-connection [conn {:datasource datasource}]
     (jdbc/query conn
                 [(str "SELECT TOP 0 * " (first
                                          (hsql/format {:format table})))]
                 :as-arrays? true))))

;; User functions

(defn get-user-password-hash [user-name]
  ((comp :password
         first
         query
         q/get-user-password-hash) user-name))

(defn get-user-detail [user-name]
  ((comp first
         query
         q/get-user-detail) user-name))

(defn get-user-type [user-name]
  ((comp :user-type
         first
         query
         q/get-user-type) user-name))

(defn set-user-last-logged-in [user-name logged-in]
  (-> {:user-name user-name
       :logged-in logged-in}
      q/set-user-last-logged-in
      execute))

;; Ticket Functions

(defn get-ticket-detail [where-clause & [fs]]
  ((comp
    ;;first
    query
    #(q/get-ticket-detail % fs)) where-clause))

(defn create-ticket [ticket]
  ((comp insert
         q/create-ticket) ticket))

;; Tech

(defn get-user-id [user-name]
  ((comp :user-id
         first
         query
         q/get-user-id) user-name))

(defn get-created-by-id [user-name]
  (get-user-id user-name))

(defn get-updated-by-id [user-name]
  (get-user-id user-name))

(defn get-assigned-to-id [user-name]
  (get-user-id user-name))

;; Priority

(defn get-priority-id [priority-name]
  ((comp :ticket-priority-id
         first
         query
         q/get-priority-id) priority-name))

;; Status

(defn get-status-id [status-name]
  ((comp :ticket-status-id
         first
         query
         q/get-status-id) status-name))
