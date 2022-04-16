(defproject jepsen.neo4j "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main jepsen.neo4j
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.2.1-SNAPSHOT"]
                 [verschlimmbesserung "0.1.3"]
                 [org.json/json "20210307"]
                 [org.neo4j.driver/neo4j-java-driver "4.0.1"]
                 [fullspectrum/neo4clj "1.0.0-ALPHA7"]]
  :jvm-opts ["-Djava.awt.headless=true"]
  :java-source-paths ["src/java/"]
  :source-paths      ["src/clojure"]
  :repl-options {:init-ns jepsen.neo4j})
  ;; :repl-options {:init-ns jepsen.neo4j})
