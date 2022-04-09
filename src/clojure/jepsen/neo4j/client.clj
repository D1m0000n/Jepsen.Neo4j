(ns jepsen.neo4j.client
  (:require [neo4clj.client :as nc]))

(defn create-node-with-id
  "Create node with id"
  [conn id]
  (nc/create-node! conn {:ref-id "p"
                         :id id})
)

(defn find-node-by-id
  "Find node with exact id"
  [conn id]
  (nc/find-node conn {:ref-id "p"
                      :id id})
)
