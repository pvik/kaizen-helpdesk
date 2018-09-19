(ns kaizen-helpdesk.auth
  (:require [cljs.core.async :refer [<! go close!]]
            [cljs-http.client :as http]
            [hodgepodge.core :as storage]
            [taoensso.timbre :as log]))

(def jwt-timeout-ms 1800000)

(defonce user-identity (if (:identity storage/local-storage)
                         (atom (:identity storage/local-storage))
                         (atom {})))

(defn set-jwt-last-used-now! []
  (if (:identity storage/local-storage)
    (let [user-id-map (:identity storage/local-storage)]
      (assoc! storage/local-storage
              :identity
              (assoc user-id-map
                     :jwt-last-used
                     (.getTime (js/Date.)))))))

(defn logout []
  (when (:identity storage/local-storage)
    (reset! user-identity {})
    (dissoc! storage/local-storage :identity)))

(defn logged-in?
  "Checks if :identity exists in local-storage.
  If not, it will also remove :login from local-storage
  Returns :login/:username or false"
  []
  (if (:identity storage/local-storage)
    (let [{:keys [username jwt-last-used]} (:identity storage/local-storage)
          current-epoch (.getTime (js/Date.))]
      (if (< current-epoch
             (+ jwt-last-used
                jwt-timeout-ms))
        username 
        (do
          (logout)
          false)))
    false))

(defn is-admin? []
  (log/info "is-admin?")
  (let [username (logged-in?)]
    (if (and username (contains? (:identity storage/local-storage) :is-admin))
      ((comp :is-admin :identity) storage/local-storage)
      (go (let [user-id-map (:identity storage/local-storage)
                response (<! (http/get
                              "/api/is-admin"
                              {:headers {"Authorization" (str "Token " (:token user-id-map))}}))
                body        (:body response)]
            (log/debug "response:" response)
            (if (= (:status response) 200)
              (do
                (assoc! storage/local-storage
                        :identity
                        (assoc user-id-map
                               :is-admin (:is-admin body)))
                (swap! user-identity :is-admin (:is-admin body))
                true)
              (do
                (assoc! storage/local-storage
                        :identity
                        (assoc user-id-map
                               :is-admin false))
                (swap! user-identity :is-admin false)
                false)))))))

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
            (reset! user-identity (:identity storage/local-storage))
            (set-jwt-last-used-now!)
            true)
          (do 
            (log/error "login failed:" (:message body))
            (reset! user-identity {})
            false)))))
