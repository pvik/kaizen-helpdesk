(ns kaizen-helpdesk.auth
  (:require [cljs.core.async :refer [<! go]]
            [cljs-http.client :as http]
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]))

(def session-key-timeout-ms 1800000)

(defn logged-in?
  "Checks if :identity exists in local-storage.
  If not, it will also remove :login from local-storage
  Returns :login/:username or false"
  []
  (if (:identity storage/local-storage)
    (:token (:identity storage/local-storage))
    false))

(defn login [username password]
  (log/info "login" username)
  (go (let [response (<! (http/get "/login"
                                   {:query-params {"username" username
                                                   "password" password}}))
            body (:body response)]
        (log/debug "response:" response)
        (log/debug "status:" (:status response))
        (log/debug "body:" (:body response))
        (if (= (:status response) 200)
          (do 
            (assoc! storage/local-storage :identity {:username username
                                                     :token (:token body)})
            true)
          (do 
            (log/error "login failed:" (:message body))
            false)))))
