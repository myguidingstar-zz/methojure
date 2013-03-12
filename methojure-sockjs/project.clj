(defproject methojure/methojure-sockjs "0.1.0-SNAPSHOT"
  :description "A sockjs implementation on top of http-kit server"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["server/src"]
  :test-paths ["server/test"]
  :resource-paths ["server/resources" "client/resources"]

  :dependencies [[org.clojure/clojure "1.5.0"]
                 [http-kit "2.0.0-SNAPSHOT"]
                 [cheshire "5.0.2"]
                 [compojure "1.1.5"]
                 [methojure/methojure-test "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "0.3.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              [{:source-paths ["client/src"],
                :jar true
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/main-debug.js",
                 :externs ["client/resouces/externs/sockjs.js"]
                 :optimizations :whitespace},
                :id "dev"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/test-debug.js",
                 :externs ["client/resouces/externs/sockjs.js"],
                 :optimizations :whitespace,
                 },
                :id "test"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print false,
                 :output-to "client/resources/public/js/test-optimized.js",
                 :externs ["client/resouces/externs/sockjs.js"],
                 :optimizations :advanced,
                 },
                :id "test-advanced"}]}

  :profiles {:dev {:dependencies [[ring/ring-devel "1.1.8"]]}})
