(ns kaizen-helpdesk.data.query
  (:require [honeysql.helpers
             :refer [merge-where where sset insert-into values]
             :as helpers]
            [taoensso.timbre   :as log]))

(def ^:const pg-limit 10)

(defn select [table-name {:keys [fields limit page]}]
  (let [fs     (if fields fields [:*])
        lmt    (if limit limit pg-limit)
        offset (if page (* (- page 1) lmt) 0)]
    (log/debug "select" fs "from" table-name)
    {:limit lmt
     :offset offset
     :select fs
     :from   [(keyword (str "kaizen." (name table-name)))]}))

;; User Queries

(defn get-user-detail [user-name & [fs]]
  (-> (select :user_detail fs)
      (merge-where [:and
                    [:= :user_name user-name]
                    [:= :active true]])))

(defn get-user-password-hash [user-name]
  (-> (select :user_auth {:fields [:password]})
      (merge-where
       [:= :user_id (get-user-detail user-name {:fields [:user_id]})])))

(defn get-user-type [user-name]
  (-> (select :user_type {:fields [:user_type]})
      (merge-where
       [:= :user_type_id (get-user-detail user-name {:fields [:user_type_id]})])))

(defn set-user-last-logged-in [{:keys [user-name logged-in]}]
  (-> (helpers/update :kaizen.user_detail)
      (sset {:last_login logged-in})
      (where [:= :user_id (get-user-detail user-name {:fields [:user_id]})])))


;; Ticket Queries

(defn get-ticket-detail [ & [where-clause fs]]
  (-> (select :ticket fs)
      (merge-where where-clause)))

;; (defn get-ticket-detail [ticket-id & fs]
;;   (-> (select :ticket (first fs))
;;       (merge-where [:= :ticket_id ticket-id])))

(defn create-ticket [ticket]
  (->
   (insert-into :kaizen.ticket_detail)
   (values [ticket])))

;; Tech

(defn get-user-id [user-name]
  (-> (select :user_detail {:fields [:user_id]})
      (merge-where 
       [:= :user_name user-name])))

;; Priority

(defn get-priority-id [priority-name]
  (-> (select :ticket_priority {:fields [:ticket_priority_id]})
      (merge-where
       [:= :priority_name priority-name])))

;; Status

(defn get-status-id [status-name]
  (-> (select :ticket_status {:fields [:ticket_status_id]})
      (merge-where
       [:= :status_name status-name])))
