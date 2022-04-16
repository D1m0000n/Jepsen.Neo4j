(ns jepsen.neo4j.db
  (:require [clojure.tools.logging :refer :all]
            [jepsen [db :as db]
             [control :as c]]
            [jepsen.control.util :as cu]
            [jepsen.neo4j [client :as client]]))

(def dir "/opt/neo4j")
(def binary "/opt/neo4j/bin/neo4j")
(def admin "/opt/neo4j/bin/neo4j-admin")
(def helper "/home/d1m0000n/helpers_neo4j/auth.sh")
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
