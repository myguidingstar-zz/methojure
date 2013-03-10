(defproject methojure/methojure-sockjs "0.1.0-SNAPSHOT"
  :description "A sockjs implementation on top of http-kit server"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["server/src"]
  :test-paths ["server/test"]
  :resource-paths ["server/resource"]

  :dependencies [[org.clojure/clojure "1.5.0"]
                 [http-kit "2.0.0-SNAPSHOT"]
                 [cheshire "5.0.2"]
                 [compojure "1.1.5"]]

  :profiles {:dev {:dependencies [[ring/ring-devel "1.1.8"]]}})
