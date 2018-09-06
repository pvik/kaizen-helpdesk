(set-env!
 :source-paths #{"src/scss" "src/clj" "src/cljs" "src/cljc"}
 :resource-paths #{"html"}

 :dependencies '[[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 ;; server
                 [compojure "1.6.1"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring-middleware-format "0.7.2"]
                 [javax.servlet/servlet-api "2.5"]
                 ;; common
                 [org.clojure/core.async "0.4.474"]
                 [org.clojars.akiel/async-error "0.3"]
                 [hickory "0.7.1"]
                 [instaparse "1.4.9"]
                 ;; clj
                 [clj-time "0.14.4"]
                 [clj-http "3.9.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.codec "0.1.1"] ;; Base64
                 ;; cljs
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [prismatic/dommy "1.1.0"]
                 [hipo "0.5.2"]
                 [hodgepodge "0.1.3"]
                 [cljs-http "0.1.45"]
                 [com.cemerick/url "0.1.1"]
                 [reagent "0.8.1"]
                 ;; db
                 [ragtime "0.7.2"]     ;; Migrations
                 [hikari-cp "2.6.0"]    ;; Connection Pooling
                 [org.clojure/java.jdbc "0.7.8"]
                 [honeysql "0.9.3"]
                 [org.postgresql/postgresql "42.2.5"]
                 ;; Authentication framework with JWT
                 [buddy/buddy-auth "2.1.0"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-sign "3.0.0"]
                 ;; argon2 for password hashing
                 [de.mkammerer/argon2-jvm "2.4"]
                 ;; logging
                 [com.taoensso/timbre "4.10.0"] ;; logging
                 [com.fzakaria/slf4j-timbre "0.3.12"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 ;; spec , test & gen
                 [org.clojure/test.check "0.9.0"]
                 ;; Dependencies for build process
                 [adzerk/boot-cljs "2.1.4"]
                 [pandeiro/boot-http "0.8.3"]
                 [adzerk/boot-reload "0.6.0"]
                 [adzerk/boot-cljs-repl "0.3.3"]
                 [com.cemerick/piggieback "0.2.2" :scope "test"]
                 [weasel "0.7.0" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                 [mbuczko/boot-ragtime "0.3.1"]
                 [deraen/boot-sass "0.3.1"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[mbuczko.boot-ragtime :refer [ragtime]]
         '[deraen.boot-sass :refer [sass]])

(def db-opts (:db (clojure.edn/read-string (slurp "resources/config.edn"))))
(task-options!
 ragtime {:database (str "jdbc:"
                         (:dbtype db-opts) "://"
                         (:user db-opts) ":"
                         (:password db-opts) "@"
                         (:host db-opts) ":"
                         (:port db-opts) "/" (:dbname db-opts))})

(deftask dev
  "Launch Immediate Feedback Development Environment"
  []
  (comp
   (serve :handler 'kaizen-helpdesk.core/app ;; ring handler
          :resource-root "target"            ;; root classpath
          :reload true)                      ;; reload ns
   (watch)
   (reload)
   (cljs-repl) ;; before cljs task
   (cljs)
   (sass)
   (target :dir #{"target"})))
