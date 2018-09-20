(ns kaizen-helpdesk.helpers
  (:require [kaizen-helpdesk.data.db :as db]
            [taoensso.timbre :as log]))

(defn get-ticket-id-fields []
  (log/debug "retrieving ticket id fields")
  (let [ticket-fields (db/get-table-fields :ticket)
        _ (log/debug "ticket-fields:" ticket-fields)]
    (set (filter #(boolean (re-find #"-id" (name %)))
                 ticket-fields))))

