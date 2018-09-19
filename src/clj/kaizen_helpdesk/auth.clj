(ns kaizen-helpdesk.auth
  (:require
   [buddy.sign.jwt :as jwt]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.accessrules :refer (success error)]
   [buddy.auth :refer [authenticated?]]
   [clj-time.core :as time]
   [clojure.data.codec.base64 :as b64]
   [taoensso.timbre :as log]
   [kaizen-helpdesk.data.user :as u]))

;; Secret used in HMAC Signing of JWT
;;   Take 5 random number, concatenate them and convert to base64
(defonce ^:private secret
  (String.
   (b64/encode
    (.getBytes
     (apply str (take 5 (repeatedly #(str (rand)))))))
   "UTF-8"))

(defonce ^:private ^:const auth-alg {:alg :hs512})
(defonce ^:private ^:const jwt-expire-duration-secs 3600)

;; Create an instance of auth backend.
(def auth-backend (jws-backend {:secret secret :options auth-alg}))

;; Semantic response helpers
(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn authenticate
  "Checks if request (with username/password :query-params)
  or username/password is valid"
  ([request]
   (let [username (get-in request [:params :username])
         password (get-in request [:params :password])]
     (authenticate username password)))
  ([username password]
   (if (and username password)
     (u/login? username password)
     false)))

(defn auth-handler
  [request]
  (if (authenticate request)
    (let [username (get-in request [:params :username])
          password (get-in request [:params :password])
          claims {:user (keyword username)
                  :id   (u/user-id username)
                  :exp  (time/plus (time/now)
                                   (time/seconds jwt-expire-duration-secs))
                  :type (u/user-type username)}
          token (jwt/sign claims secret auth-alg)
          _     (u/set-last-logged-in-now! username)]
      (ok {:token token}))
    (bad-request {:message "invalid authentication"})))

;; Access Level Handlers

(defn authenticated-access
  "Check if request coming in is authenticated with user/password
  or a valid JWT token"
  [request]
  (if (or (authenticated? request)
          (authenticate request))
    true
    (error "invalid authentication")))

(defn admin-access
  "Check if request coming in has admin access"
  [request]
  (if (= "admin" ((comp :type :identity) request))
    true
    (error "admin access required")))

