(ns kaizen-helpdesk.helper
  (:require [dommy.core :as dom] 
            [hipo.core :as hipo]
            [hodgepodge.core :as storage]
            [goog.dom :as gdom]
            [taoensso.timbre :as log]
            [kaizen-helpdesk.web.template :as template]))

(defn gen-id []
  (let [id-1 (:id storage/local-storage)
        id   (if id-1 id-1 1)]    
    (assoc! storage/local-storage :id (inc id))
    id))

(defn remove-all-child-nodes
  "Remove all children under parent-node"
  [parent-node]
  (let [node (dom/sel1 parent-node)]
    (gdom/removeChildren node)))

