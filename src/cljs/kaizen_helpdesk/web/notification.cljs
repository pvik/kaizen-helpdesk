(ns kaizen-helpdesk.web.notification
  (:require [reagent.core :as r]
            [dommy.core :as dom] 
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.helper :as helper]))

(defonce toasts (r/atom (hash-map)))

(defn add-toast [type message]
  (let [id (helper/gen-id)]
    (swap! toasts assoc id {:id   id
                            :type type
                            :message message})))

(defn delete-toast [id]
  (swap! toasts dissoc id))

(defn toast-comp
  "Toast Notification Item
  Used to render Toast Notification in
  kaizen-helpdesk.web.notifications"
  [{:keys [id type message]}]
  [:div {:class (str "toast toast-" (name type))}
   [:button.btn.btn-clear.float-right
    {:on-click (fn [e]
                 (delete-toast id))}]
   [:span.text-dark message]])

(defn toasts-component []
  [:div
   (for [[id t] @toasts]
     ^{:key id} [toast-comp t])])

(defn clear-all-toasts []
  (reset! toasts (hash-map)))

(defn render-toasts []
  (r/render [toasts-component]
            (dom/sel1 :#div-toasts)))

(defn render-notifications []
  (log/debug "render-notifications")
  (clear-all-toasts)
  (render-toasts))
