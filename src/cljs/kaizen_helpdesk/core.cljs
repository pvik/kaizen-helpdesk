(ns kaizen-helpdesk.core
  (:require [dommy.core :as dom] 
            [hipo.core :as hipo]
            [hodgepodge.core :as storage]
            [cljs.core.async :refer [<! go]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.ev-handler :as handler]
            [kaizen-helpdesk.auth :as auth]
            [kaizen-helpdesk.web.components :as comp]
            [kaizen-helpdesk.web.notification :as notification]))

(enable-console-print!)

;; Code executed on all pages
(defonce _
  (do
    (notification/render-notifications)))

(defn init []
  (log/info "Initializing...")
  (if (auth/logged-in?)
    (do
      (log/info "logged in")
      (comp/render-components))
    (do
      (log/info "not logged in")
      (auth/logout)
      (set! (.-href js/location) "login.html"))))

(defn ^:export login []
  (log/info "login")
  (when (auth/logged-in?)
    (set! (.-href js/location) "index.html"))
  (dom/listen! (dom/sel1 :#btn-login)
               :click
               handler/login-ev)
  ;; (dom/listen! (dom/sel1 :#form-login)
  ;;              :submit
  ;;              handler/login-ev)
  )

(defn ^:export dashboard []
  (init)
  (log/info "kaizen dashboard"))

(defn ^:export ticket []
  (init)
  (log/info "kaizen - ticket"))

(defn ^:export admin []
  (init)
  (if (auth/is-admin?)
    (do 
      (log/info "kaizen - admin panel"))
    (do
      (set! (.-href js/location) "index.html"))))
