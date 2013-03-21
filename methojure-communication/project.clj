(defproject methojure/methojure-communication "0.1.0-SNAPSHOT"
  :description "Defines the basic communication paradigments of methojure:
                RPC calls and Publish/Subscribe"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["server/src"]
  :test-paths ["server/test"]
  :resource-paths ["server/resources" "client/resources"]

  :dependencies [[org.clojure/clojure "1.5.0"]
                 [methojure/methojure-sockjs "0.1.0-SNAPSHOT"]
                 [methojure/methojure-test "0.1.0-SNAPSHOT"]
                 [crate "0.2.4"]]

  :plugins [[lein-cljsbuild "0.3.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              [{:source-paths ["client/src"],
                :jar true
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/main-debug.js",
                 :externs ["resouces/externs/sockjs.js"]
                 :optimizations :whitespace},
                :id "dev"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/test-debug.js",
                 :externs ["resouces/externs/sockjs.js"],
                 :optimizations :whitespace,
                 },
                :id "test"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print false,
                 :output-to "client/resources/public/js/test-optimized.js",
                 :externs ["resouces/externs/sockjs.js"],
                 :optimizations :advanced,
                 },
                :id "test-advanced"}]

              :test-commands
              {"test" [~(str (.getParent (clojure.java.io/file *file*))
                             "/bin/test.sh")]
               
               "test-advanced" [~(str (.getParent (clojure.java.io/file *file*))
                                      "/bin/test.sh")
                                "--config" "karma-advanced.conf.js"]}}

  :profiles {:dev {:dependencies [[ring/ring-devel "1.1.8"]]}})
