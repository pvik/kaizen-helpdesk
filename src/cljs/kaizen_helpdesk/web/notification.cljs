(ns kaizen-helpdesk.web.notification
  (:require [reagent.core :as r]
            [dommy.core :as dom] 
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.helper :as helper]))

(defonce ^:private toasts (r/atom (hash-map)))

(defn add-toast [type message]
  (let [id (helper/gen-id)]
    (swap! toasts assoc id {:id   id
                            :type type
                            :message message})))

(defn delete-toast [id]
  (swap! toasts dissoc id))

(defn clear-all-toasts []
  (reset! toasts (hash-map)))

(defn- toast-comp
  "Toast Notification Item
  Used to render Toast Notification in
  kaizen-helpdesk.web.notifications"
  [{:keys [id type message]}]
  [:div {:class (str "toast toast-" (name type))}
   [:button.btn.btn-clear.float-right
    {:on-click (fn [e]
                 (delete-toast id))}]
   [:div.text-center.text-dark message]])

(defn- toasts-component []
  [:div.column.col-12 
   (when (> (count @toasts) 0)
     [:button.btn.btn-sm.btn-link.float-right
      {:on-click (fn [e]
                   (clear-all-toasts))} "Clear All"])
   (for [[id t] @toasts]
     ^{:key id} [toast-comp t])])

(defn- render-toasts []
  (r/render [toasts-component]
            (dom/sel1 :#div-toasts)))

(defn render-notifications []
  (log/debug "render-notifications")
  (clear-all-toasts)
  (render-toasts))
