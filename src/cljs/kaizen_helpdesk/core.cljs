(ns kaizen-helpdesk.core
  (:require [dommy.core :as dom] 
            [hipo.core :as hipo]
            [hodgepodge.core :as storage]
            [cljs.core.async :refer [<! go]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.ev-handler :as handler]
            [kaizen-helpdesk.auth :as auth]
            [kaizen-helpdesk.web.notification :as notification]))

(enable-console-print!)

(defonce _
  (do
    (notification/render-notifications)))

(defn init []
  (log/info "Initializing...")
  (if (auth/logged-in?)
    (do
      (log/info "logged in"))
    (do
      (log/info "not logged in")
      (dissoc! storage/local-storage :identity)
      (set! (.-href js/location) "login.html"))))

(defn ^:export login []
  (log/info "login")
  (dissoc! storage/local-storage :identity)
  (dom/listen! (dom/sel1 :#btn-login)
               :click
               handler/login-ev))

(defn ^:export dashboard []
  (init)
  (log/info "kaizen dashboard"))

(defn ^:export ticket []
  (init)
  (log/info "kaizen - ticket"))
