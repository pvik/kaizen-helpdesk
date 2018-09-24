(ns kaizen-helpdesk.data.db
  (:import [java.sql SQLException]
           [org.postgresql.util PGobject])
  (:require [hikari-cp.core :refer :all]
            [clj-time.core     :as t]
            [clj-time.format   :as tf]
            [clj-time.coerce   :as tc]
            [clojure.string    :as str]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core     :as hsql]
            [cheshire.core :refer [generate-string parse-string]]
            ;; [clojure.data.json :as json]
            ;;[honeysql.helpers :refer :all :as helpers]
            [taoensso.timbre   :as log]
            [kaizen-helpdesk.data.query :as q]))

;; For Joda Objects to be used by JDBC
(require 'clj-time.jdbc)

;; For PostgreSQL JSON fields
;; From: https://github.com/siscia/postgres-type/
(defn- add-jsonx-type [jsonx write-json read-json]
  {:pre [(or (= "json" jsonx) (= "jsonb" jsonx))
         (fn? write-json)
         (fn? read-json)]}
  (let [to-pgjson (fn [value]
                    (log/debug "PROTO" value "->" (write-json value) " type" (type (write-json value)))
                    (doto  (PGobject.)
                      (.setType jsonx)
                      (.setValue (write-json value))))]

    (extend-protocol jdbc/ISQLValue
      clojure.lang.IPersistentMap
      (sql-value [value] (to-pgjson value))
      clojure.lang.IPersistentVector
      (sql-value [value] (to-pgjson value)))

    (extend-protocol jdbc/IResultSetReadColumn
      PGobject
      (result-set-read-column [pgobj _metadata _index]
        (let [type  (.getType pgobj)
              value (.getValue pgobj)]
          (if (= type jsonx)
            (read-json value)
            value))))))
(def add-json-type (partial add-jsonx-type "json"))
(def add-jsonb-type (partial add-jsonx-type "jsonb"))

;; (add-json-type json/write-str json/read-str)
(add-json-type generate-string parse-string)

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

;; handle SQL Exceptions
(defn print-sql-exception
  "Prints the contents of an SQLException to *out*"
  [^SQLException exception]
  (let [^Class exception-class (class exception)]
    (format (str "%s:" \newline
                 " Message: %s" \newline
                 " SQLState: %s" \newline
                 " Error Code: %d")
            (.getSimpleName exception-class)
            (.getMessage exception)
            (.getSQLState exception)
            (.getErrorCode exception))))

(defn print-sql-exception-chain
  "Prints a chain of SQLExceptions to *out*"
  [^SQLException exception]
  (loop [e exception
         err-str ""]
    (if (not e)
      err-str
      (recur (.getNextException e)
             (str (print-sql-exception e) "\n" err-str)))))

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

(defonce ^:private ^:const jdbc-operation-map
  {:query   jdbc/query
   :execute jdbc/execute!})

(defn- run-sql [hsql-map operation]
  (log/debug hsql-map)
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (try
      ((operation jdbc-operation-map) conn (hsql/format hsql-map)
       {:row-fn sanitize-cols-from-db})
      (catch SQLException e#
        (let [ex-msg (print-sql-exception-chain e#)]
          (log/error "SQLException:" ex-msg)
          (throw (ex-info "db error"
                          {:cause ex-msg})))))))

(defn- query [hsql-map]
  (run-sql hsql-map :query))

(defn- execute [hsql-map]
  (run-sql hsql-map :execute))

(defn- insert [hsql-map]
  (log/debug "insert" hsql-map)
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (try
      (jdbc/insert-multi! conn
                          (:insert-into hsql-map)
                          (map sanitize-cols-for-db (:values hsql-map)))
      (catch SQLException e#
        (let [ex-msg (print-sql-exception-chain e#)]
          (log/error "SQLException:" ex-msg)
          (throw (ex-info "db error"
                          {:cause ex-msg})))))))

;; schema functions

(defn get-table-fields [table]
  (let [tbl (q/table-name table)
        qry (hsql/format {:limit 1
                          :select [:*]
                          :from [tbl]})]
    (map #(keyword (str/replace (name %) #"_" "-"))
         (first
          (jdbc/with-db-connection [conn {:datasource datasource}]
            (jdbc/query conn
                        qry
                        {:as-arrays? true}))))))

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

;; Entity

(defn create-entity [entity data]
  ((comp insert
         #(q/create-entity entity %)) data))

(defn get-entity [entity where-clause & [fs]]
  ((comp
    ;;first
    query
    #(q/get-entity entity % fs)) where-clause))

(defn update-entity [entity data]
  ((comp execute
         #(q/update-entity entity %)) data))

(defn delete-entity [entity id]
  ((comp execute
         #(q/delete-entity entity %)) id))

;; Tech

(defn get-user-id [user-name]
  ((comp :id
         first
         query
         q/get-user-id) user-name))

;; (defn get-status-id [status-name]
;;   ((comp :id
;;          first
;;          query
;;          q/get-status-id) status-name))

;; Permissions

(defn get-permissions [permission-for]
  ((comp query
         q/get-permissions) permission-for))
