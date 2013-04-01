(defproject methojure/methojure-mongo "0.1.0-SNAPSHOT"
  :description "Methojure mongo"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["server/src" ".generated/clj"
                 "comp/clojurescript/src/clj"
                 "comp/clojurescript/src/cljs"]
  :test-paths ["server/test" ".generated/clj-test"]
  :resource-paths ["server/resources" "client/resources"]

  :dependencies [[org.clojure/clojure "1.5.0"]
                 [methojure/methojure-communication "0.1.0-SNAPSHOT"]
                 [methojure/methojure-test "0.1.0-SNAPSHOT"]
                 [com.novemberain/monger "1.5.0"]
                 [org.clojure/data.json "0.2.1"]]

  :plugins [[lein-cljsbuild "0.3.0"]
            [com.keminglabs/cljx "0.2.1"]]

  :hooks [cljx.hooks leiningen.cljsbuild]

  :cljx {:builds [{:source-paths ["common/src"]
                   :output-path ".generated/clj"
                   :rules cljx.rules/clj-rules}
                  {:source-paths ["common/test"]
                   :output-path ".generated/clj-test"
                   :rules cljx.rules/clj-rules}
                  {:source-paths ["common/src"]
                   :output-path ".generated/cljs"
                   :extension "cljs"
                   :include-meta true
                   :rules methojure.rules/cljs-rules}
                  {:source-paths ["common/test"]
                   :output-path ".generated/cljs-test"
                   :extension "cljs"
                   :include-meta true
                   :rules methojure.rules/cljs-rules}]}

  :cljsbuild {:builds
              [{:source-paths ["client/src" ".generated/cljs"],
                :jar true
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/main-debug.js",
                 :externs ["resouces/externs/sockjs.js"]
                 :optimizations :whitespace},
                :id "dev"}
               {:source-paths ["client/src" ".generated/cljs"
                               "client/test" ".generated/cljs-test"],
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/test-debug.js",
                 :externs ["resouces/externs/sockjs.js"],
                 :optimizations :whitespace,
                 },
                :id "test"}
               {:source-paths ["client/src" ".generated/cljs"
                               "client/test" ".generated/cljs-test"],
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
                                "--config" "karma-advanced.conf.js"]}
              }

  :profiles {:dev {:dependencies [[ring/ring-devel "1.1.8"]]}})
