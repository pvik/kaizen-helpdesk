(ns kaizen-helpdesk.ev-handler
  (:require [dommy.core :as dom] 
            [cljs.core.async :refer [<! go]]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.auth :as auth]))

(defn login-ev [e]
  (dom/add-class! (dom/sel1 :#btn-login) :loading)
  (go
    (let [username (.-value (dom/sel1 :#input-username))
          password (.-value (dom/sel1 :#input-password))
          login?   (<! (auth/login username password))]
      (dom/remove-class! (dom/sel1 :#btn-login) :loading)
      (when login?
        (set! (.-href js/location) "index.html")))))
