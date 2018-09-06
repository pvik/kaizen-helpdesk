(ns kaizen-helpdesk.core
  (:require [dommy.core :as dom] 
            [hipo.core :as hipo]
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.auth :as auth]))

(enable-console-print!)

(defn init []
  (log/info "Initializing...")
  (if (auth/logged-in?)
    (do
      (log/info "logged in"))
    (do
      (log/info "not logged in"))))

(defn ^:export dashboard []
  (init)
  (log/info "kaizen dashboard"))

(defn ^:export ticket []
  (init)
  (log/info "kaizen - ticket"))
