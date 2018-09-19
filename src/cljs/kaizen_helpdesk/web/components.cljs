(ns kaizen-helpdesk.web.components
  (:require [reagent.core :as r]
            [dommy.core :as dom]
            [hipo.core :as hipo]
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.auth :as auth]))

(defn user-nav-comp []
  [:div.dropdown
   [:a.btn.btn-link.dropdown-toggle.text-primary
    {:tabIndex "0" :href "#"}
    [:i.icon.icon-people]
    " " (:username @auth/user-identity)
    [:i.icon.icon-caret]]
   [:ul.menu.bg-dark
    (if (:is-admin @auth/user-identity)
      [:li.menu-item.bg-dark
       [:a {:href "admin.html"} [:i.icon.icon-link] "Admin"]])
    [:li.menu-item.bg-dark
     [:a {:on-click (fn [e]
                      (auth/logout)
                      (set! (.-href js/location) "login.html"))}
      [:i.icon.icon-link] "Logout"]]]])

(defn render-user-nav []
  ;; No Need to use reagent for this, there should not be
  ;; any reactive changes to logged in user
  (.appendChild (dom/sel1 :#user-nav)
                (hipo/create (user-nav-comp))))

(defn render-components []
  (render-user-nav))
