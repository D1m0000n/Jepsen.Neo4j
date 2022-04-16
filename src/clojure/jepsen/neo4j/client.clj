(ns jepsen.neo4j.client
  (:require [clojure.tools.logging :refer :all]
            [clojure.reflect :as r]
            )
  ;; (:use [clojure.pprint :only [print-table]])
  (:import (org.neo4j.driver Driver
                             GraphDatabase)))

(defn ^Driver build-driver
  [^String host]
  ;; (info (print-table
  ;;        (sort-by :name
  ;;                 (filter :exception-types (:members (r/reflect GraphDatabase))))))
  (. GraphDatabase (driver host))
  )
