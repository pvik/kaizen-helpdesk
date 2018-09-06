(ns kaizen-helpdesk.auth
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]))

(defn logged-in? []
  false)
