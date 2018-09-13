(ns kaizen-helpdesk.data.user
  "Handle all user operations like login, authentication, signup, etc"
  (:require [clojure.spec.alpha      :as s]
            [clj-time.core           :as time]
            [taoensso.timbre         :as log]
            [kaizen-helpdesk.data.db :as db])
  (:import  (de.mkammerer.argon2 Argon2Factory
                                 Argon2Factory$Argon2Types)))

(def ^:private argon2
  "Create a Argon2id object to hash and verify passwords"
  (Argon2Factory/create Argon2Factory$Argon2Types/ARGON2id))

;; defining constants to use for Argon2 hashing
(defonce ^:private ^:const arg2-iterations 8)
(defonce ^:private ^:const arg2-memory 65536)
(defonce ^:private ^:const arg2-parallelism 1)

(defn- argon2-hash
  "Hash string with Argon2id"
  [str-to-be-hashed]
  (.hash argon2 arg2-iterations arg2-memory arg2-parallelism str-to-be-hashed))

(defn- argon2-verify
  "verify argon2-hash(plaintext_str) == hashed_str"
  [hashed-str plaintext-str]
  (.verify argon2 hashed-str plaintext-str))

(defn login? [user-name password]
  (log/debug "Login Request for:" user-name)
  (let [db-pass-hash (db/get-user-password-hash user-name)]
    (if db-pass-hash
      (argon2-verify db-pass-hash password)
      false)))

(defn user-id [user-name]
  (db/get-user-id user-name))

(defn user-type [user-name]
  (db/get-user-type user-name))

(defn set-last-logged-in-now! [user-name]
  (log/debug "Setting last login")
  (db/set-user-last-logged-in user-name (time/now)))

(s/fdef login
  :args (s/cat :user-id string? :password string?)
  :ret boolean?)
