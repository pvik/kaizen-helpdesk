(ns kaizen-helpdesk.core
  (:require [dommy.core :as dom] 
            [hipo.core :as hipo]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [hodgepodge.core :as storage]
            [cemerick.url :as url]
            [taoensso.timbre :as log]))

(enable-console-print!)

(defn init []
  (log/info "Initializing..."))

(defn ^:export dashboard []
  (init)
  (log/info "initializing whd-frontend dashboard"))

(defn ^:export ticket []
  (init)
  (log/info "initializing whd-frontend - ticket"))
