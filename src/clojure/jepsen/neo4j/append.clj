(ns jepsen.neo4j.append
  (:require [clojure.tools.logging :refer :all]
            [jepsen [client :as client]
             [util :as util :refer [timeout]]
             [independent :as independent]]
            [neo4clj.client :as neo]
            [jepsen.tests.cycle.append :as list-append]
            [slingshot.slingshot :refer [try+]])
  (:import (java.util ArrayList List)
           (mipt.bit.utils Operation)
           (org.json JSONObject)))

(defn update-operations!
  [^ArrayList operations [f k v :as mop]]
  (info :update-operations mop)
  (case f
    :r       (.add operations (Operation. "r" k v))
    :append  (.add operations (Operation. "append" k v))
    )
  )

(def operations
  {"r"   :r
   "append" :append})

(defn processing-results!
  [^JSONObject result]
  (info :in-processing-results (.toString result))
  (let [operation (.get result "type")
        f         (get operations operation)
        k         (Long/valueOf (.get result "key"))
        values    (if (.has result  "labels") (.get result "labels") [])
        v         (.get result "value")]
  (case operation
    "r"       [f k (vec values)]
    "append"  [f k v]))
  )

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (neo/connect "bolt://localhost:7687" {:log {:level :info}
                                                            :encryption :none})))

  (setup! [this test])

  (invoke! [this test op]
    (try+
      ;(timeout 5000 (assoc op :type :info, :error :timeout)
      ;         (let [txn       (:value op)
      ;               db        (c/db conn databaseName)
      ;               container (c/container db containerName)
      ;               txn'      (mapv (partial mop! test container) txn)]
      ;           (assoc op :type :ok, :value txn')))
     (info :count (count (:value op)))

     (timeout 10000 (assoc op :type :info, :error :timeout)
              (let [txn' (let [operations   (ArrayList.)]
                           (mapv (partial update-operations! operations) (:value op))
                           (let [^List results (.execute (TransactionsExecute. container) operations)]
                             (if (not= (.size results) (count (:value op)))
                               (assoc op :type :fail, :value :transaction-fail)
                               (mapv (partial processing-results!) results))))]
                (assoc op :type :ok, :value txn')))
     #_{:clj-kondo/ignore [:unresolved-symbol]}
     (catch [:errorCode 100] ex
       (assoc op :type :fail, :error :request-rate-too-large))
))

  (teardown! [this test])

  (close! [_ test]))

(defn workload
  "A generator, client, and checker for a list-append test."
  [opts]
  (assoc (list-append/test {:key-count          10
                            ;;   :key-dist           :exponential
                            :max-txn-length     (:max-txn-length opts 4)
                            :max-writes-per-key (:max-writes-per-key opts)})
         :client (Client. nil)))
