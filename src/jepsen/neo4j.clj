(ns jepsen.neo4j
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [neo4clj.client :as neo]
            [jepsen [cli :as cli]
             [client :as client]
             [control :as c]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.ubuntu :as ubuntu]
            [jepsen.neo4j [db :as db]
             [append :as append]
             [nemesis :as nemesis]]
            )
  (:import (java.net URI)))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value  (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (neo/connect "bolt://localhost:7687" {:log {:level :info}
                                                            :encryption :none})))
    ;; (assoc this :conn (neo/connect (URI. "bolt://localhost:7687")
    ;;           "neo4j"
    ;;           "YA4jI)Y}D9a+y0sAj]T5s|C5qX!w.T0#u<be5w6X[p")))

  (setup! [this test])

  (invoke! [this test op]
    (case (:f op)
      :read (assoc op :type :ok, :value (neo/find-node conn {:ref-id "biba"
                                                             :id 2}))
      ;; :write (do (neo/create-node! conn {:ref-id (:value op)
      :write (do (neo/create-node! conn {:ref-id "biba"
                                         :id (:value op)})
                 (assoc op :type :ok))))

  (teardown! [this test])

  (close! [_ test]))

(defn neoj4-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:name "neo4j"
          :os   ubuntu/os
          :db   (db/db "3.5.4")
          :pure-generators true
          :client (Client. nil)
          :generator       (->> (gen/mix [r w])
                                (gen/stagger 1)
                                (gen/nemesis nil)
                                (gen/time-limit 15))
          }))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]

  (cli/run! (merge (cli/single-test-cmd {:test-fn neoj4-test})
                   (cli/serve-cmd))
            args))
