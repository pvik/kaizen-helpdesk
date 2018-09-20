(ns kaizen-helpdesk.data.query
  (:require [honeysql.helpers
             :refer [merge-where where sset insert-into values order-by]
             :as helpers]
            [taoensso.timbre   :as log]))

(def ^:const pg-limit 10)

(defn table-name [tbl]
  (keyword (str "kaizen." (name tbl))))

(defn set-query-limit [query limit]
  (if (= limit :no-limit)
    query
    (assoc query :limit limit)))

(defn set-query-offset [query offset]
  (if offset
    (assoc query :offset offset)
    query))

(defn select [tbl {:keys [fields limit page order]}]
  (let [fs     (or fields [:*])
        lmt    (or limit pg-limit)
        offset (if page (* (- page 1) lmt) nil)
        ord    (or order :desc)]
    (log/debug "select" fs "from" tbl)
    (-> {:select fs
         :from   [(table-name tbl)]}
        (set-query-limit lmt)
        (set-query-offset offset)
        (order-by [:id :desc]))))

;; Entity

(defn create-entity [entity data]
  (->
   (insert-into (table-name entity))
   (values [data])))

(defn get-entity [entity & [where-clause fs]]
  (-> (select entity fs)
      (merge-where where-clause)))

(defn update-entity [entity data]
  (->
   (helpers/update (table-name entity))
   (sset data)
   (where [:= :id (:id data)])))

;; User Queries

(defn get-user-detail [user-name & [fs]]
  (-> (select :user_detail fs)
      (merge-where [:and
                    [:= :user_name user-name]
                    [:= :active true]])))

(defn get-user-password-hash [user-name]
  (-> (select :user_auth {:fields [:password]})
      (merge-where
       [:= :user_id (get-user-detail user-name {:fields [:id]})])))

(defn get-user-type [user-name]
  (-> (select :user_type {:fields [:user_type]})
      (merge-where
       [:= :id (get-user-detail user-name {:fields [:user_type_id]})])))

(defn set-user-last-logged-in [{:keys [user-name logged-in]}]
  (-> (helpers/update :kaizen.user_detail)
      (sset {:last_login logged-in})
      (where [:= :id (get-user-detail user-name {:fields [:id]})])))

(defn get-user-groups [user-id]
  (-> (select :user_group_membership {:fields [:user_group_id] :limit :no-limit})
      (merge-where
       [:= :user_id user-id])))

;; Tech

(defn get-user-id [user-name]
  (-> (select :user_detail {:fields [:id]})
      (merge-where 
       [:= :user_name user-name])))

;; Status
;; (defn get-status-id [status-name]
;;   (-> (select :ticket_status {:fields [:id]})
;;       (merge-where
;;        [:= :status_name status-name])))

;; Permissions

(defn get-permissions-assignment [user-id]
  (-> (select :permission_assignment {:fields [:permission_rule_id]
                                      :limit :no-limit})
      (merge-where
       [:or
        [:and [:= :assignment_type "USR"] [:= :user_id user-id]]
        [:and [:= :assignment_type "GRP"] [:in :user_group_id (get-user-groups user-id)]]])))

(defn get-permissions-group-assignment [user-id]
  (-> (select :permission_group_member {:fields [:permission_rule_id]
                                        :limit :no-limit})
      (merge-where [:in :permission_group_id 
                    (-> (select :permission_group_assignment
                                {:fields [:permission_group_id]
                                 :limit :no-limit})
                        (merge-where
                         [:or
                          [:and [:= :assignment_type "USR"] [:= :user_id user-id]]
                          [:and [:= :assignment_type "GRP"] [:in :user_group_id (get-user-groups user-id)]]]))])))

(defn get-permissions [{:keys [user-id entity]}]
  (-> (select :permission_rule {:fields [:*]
                                :limit :no-limit})
      (merge-where
       [:and
        [:= :entity entity]
        [:= :enabled true]
        [:or
         [:= :default_rule true]
         [:in :id (get-permissions-assignment user-id)]
         [:in :id (get-permissions-group-assignment user-id)]]])
      (order-by [:rule_order :asc])))
