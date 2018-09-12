(ns kaizen-helpdesk.qual
  (:require [instaparse.core :as insta]
            #?(:clj  [clojure.edn :refer [read-string]]
               :cljs [cljs.reader :refer [read-string]])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])))

;; Two Types of Qualifications
;;  - Query Qualifications
;;      Used to Generate WHERE clause for HoneySQL SELECT
;;  - Request Qualifications
;;      Used to process a Qualification against an API Request map 

(defonce  timestamp-formatter
  (f/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(def query-qual-parser
  (insta/parser
   "<S>       = SIMPLE | NESTOR | NESTAND
    SIMPLE    = <'('> <(' ')*> NAME <(' ')+> OP <(' ')+> VAL <(' ')*> <')'>
    NESTOR    = (<'('> <(' ')*> S ( <(' ')+> BOOLOR  <(' ')+> S )+ <(' ')*> <')'>)
    NESTAND   = (<'('> <(' ')*> S ( <(' ')+> BOOLAND <(' ')+> S )+ <(' ')*> <')'>)
    <BOOLOR>  = 'or' 
    <BOOLAND> = 'and'

    NAME      = #'[-A-Za-z0-9]+'

    VAL       = NUM | STRING | TIMESTAMP | BOOL | NIL 
    NUM       = #'[-.0-9]+'
    STRING    = #'\".+?\"'
    TIMESTAMP = #'\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z\"'
    BOOL      = 'true' | 'false'
    NIL       = 'nil'

    OP        = '=' | '>' | '<' | '!=' | '<=' | '>=' | 'like' | 'not-like'"))

(defn process-query-qual-val [[type val]]
  (cond
    (contains? #{:STRING :NUM :BOOL :NIL} type)
    (read-string val)

    (= :TIMESTAMP type)
    (f/parse timestamp-formatter
             (read-string val))))

(defn query-qual-parse [s]
  (insta/transform
   {:NAME   (comp keyword #(clojure.string/replace % #"-" "_"))
    :OP     keyword
    :BOOLOP keyword
    :VAL    process-query-qual-val}
   (query-qual-parser s)))

(defn query-qual-evaluate [expr]
  (first 
   (clojure.walk/postwalk
    (fn [v]
      (cond
        (and (coll? v) (= :SIMPLE (first v)))
        (let [[_ k f c] v]
          [f k c])

        (and (coll? v) (= :NESTAND (first v)))       
        (let [exps (filter #(not (.equals % "and")) (rest v))]
          (vec (conj exps :and)))

        (and (coll? v) (= :NESTOR (first v)))       
        (let [exps (filter #(not (.equals % "or")) (rest v))]
          (vec (conj exps :or)))

        :else v))
    (query-qual-parse expr))))

;; (defn eval-qual-2 [qual-str data]
;;   (let [[l op r] (read-string qual-str)]
;;     (cond
;;       (and (seq? l)
;;            (seq? r)) (eval (list op (list eval-qual-2 (str l) data) (list eval-qual-2 (str r) data)))
;;       (seq? l)       (eval (list op (list eval-qual-2 (str l) data) r))
;;       (seq? r)       (eval (list op (list (keyword  l) data) (list eval-qual-2 (str r) data)))
;;       :else          (eval (list op (list (keyword  l) data) r)))))

;; ------------------------
;; Request Qualifications
;; ------------------------

(def request-qual-parser
  (insta/parser
   "<S>       = SIMPLE | NESTOR | NESTAND | NESTNOT
    SIMPLE    = <'('> <(' ')*> NAME <(' ')+> OP <(' ')+> VAL <(' ')*> <')'>
    NESTOR    = (<'('> <(' ')*> S ( <(' ')+> BOOLOR  <(' ')+> S )+ <(' ')*> <')'>)
    NESTAND   = (<'('> <(' ')*> S ( <(' ')+> BOOLAND <(' ')+> S )+ <(' ')*> <')'>)
    NESTNOT   = (<'('> <(' ')*> BOOLNOT <(' ')+> S <(' ')*> <')'>)
    <BOOLOR>  = 'or' 
    <BOOLAND> = 'and'
    <BOOLNOT> = 'not'

    NAME      = NAMESTR (<('.')*> NAMESTR)*
    NAMESTR   = #'[-A-Za-z0-9]+'

    VAL       = NUM | STRING | KEYWORD | TIMESTAMP | BOOL | NIL 
    NUM       = #'[-.0-9]+'
    STRING    = #'\".+?\"'
    KEYWORD   = #'[:][-A-Za-z]+'
    TIMESTAMP = #'\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z\"'
    BOOL      = 'true' | 'false'
    NIL       = 'nil'

    OP        = '=' | '>' | '<' | '<=' | '>=' | 'contains' "))

(defn process-request-qual-val [[type val]]
  (cond
    (contains? #{:STRING :NUM :KEYWORD :BOOL :NIL} type)
    (read-string val)

    (= :TIMESTAMP type)
    (f/parse timestamp-formatter
             (read-string val))))

(defn process-request-qual-name [& o]
  (reduce #(conj % (keyword (second %2))) [] o))

(defn process-request-qual-op [op]
  (cond
    (= op "contains")
    (fn [str match]
      (boolean (re-find (re-pattern match) str)))
    
    :else
    ((comp resolve symbol) op)))

(defn request-qual-parse [s]
  (insta/transform
   {:NAME   process-request-qual-name
    :OP     process-request-qual-op
    :VAL    process-request-qual-val}
   (request-qual-parser s)))

(defn request-qual-evaluate [expr request]
  (boolean
   (first 
    (clojure.walk/postwalk
     (fn [v]
       (cond
         (and (coll? v) (= :SIMPLE (first v)))
         (let [[_ k f c] v]
           (f (reduce #(get % %2) request k) c))

         (and (coll? v) (= :NESTAND (first v)))       
         (let [exps (filter #(not (.equals % "and")) (rest v))]
           (every? identity exps))

         (and (coll? v) (= :NESTOR (first v)))       
         (let [exps (filter #(not (.equals % "or")) (rest v))]
           (some identity exps))

         (and (coll? v) (= :NESTNOT (first v)))       
         (let [[_ notop exp] v]
           (not exp))

         :else v))
     (request-qual-parse expr)))))
