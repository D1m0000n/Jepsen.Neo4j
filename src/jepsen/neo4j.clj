(ns jepsen.neo4j
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [neo4clj.client :as neo]
            [jepsen [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.ubuntu :as ubuntu])
  (:import (java.net URI)))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value  (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(def dir "/opt/neo4j")
;; (def binary "./bin/neo4j")
(def binary "/opt/neo4j/bin/neo4j")
(def admin "/opt/neo4j/bin/neo4j-admin")
(def helper "/home/d1m0000n/helpers_neo4j/auth.sh")
;; (def logfile (str dir "/neo4j.log"))
(def logfile "/opt/neo4j/logs/neo4j.log")
(def debugfile "/opt/neo4j/logs/debug.log")
(def pidfile (str dir "/neo4j.pid"))

(defn db
  "Neo4j DB for a particular version."
  [version]
  (reify db/DB
    (setup! [_ test node]
      (info node "installing neo4j" version)
      (c/su (c/exec :rm :-rf dir))
      
      (c/su
       (let [url (str "http://dist.neo4j.org/neo4j-community-" version
                      "-unix.tar.gz")]
         (cu/install-archive! url dir))

      ;;  (cu/start-daemon!
      ;;     {:logfile logfile
      ;;      :pidfile pidfile
      ;;      :chdir   dir}
      ;;     binary :start)
       (c/exec helper)
       (c/exec binary :start)

       (Thread/sleep 5000)
      ;;  (c/exec admin :set-default-admin)
      ;;  (c/exec binary :status)
       ))
    

    (teardown! [_ test node]
      (info node "tearing down neo4j")
      (cu/stop-daemon! binary pidfile)
      (c/exec binary :stop)
        (Thread/sleep 3000)
      ;; (c/su (c/exec :rm :-rf dir))
               )
    
    db/LogFiles
    (log-files [_ test node]
      [logfile debugfile])))

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
          :db   (db "3.5.4")
          :pure-generators true
          :client (Client. nil)
          :generator       (->> (gen/mix [r w])
                                (gen/stagger 1)
                                (gen/nemesis nil)
                                ;; (gen/time-limit 5))
                                (gen/time-limit 15))
          }))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]

  (cli/run! (merge (cli/single-test-cmd {:test-fn neoj4-test})
                   (cli/serve-cmd))
            args))
