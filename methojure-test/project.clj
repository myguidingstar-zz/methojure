(defproject methojure/methojure-test "0.1.0-SNAPSHOT"
  :description "A clojurescript test library base on the Google closure library
                and executed with testacular."
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["server/src"]
  :test-paths ["server/test"]
  :resource-paths ["server/resources" "client/resources"]

  :dependencies [[org.clojure/clojure "1.5.0"]]

  :plugins [[lein-cljsbuild "0.3.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              [{:source-paths ["client/src"],
                :jar true
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/main-debug.js",
                 :optimizations :whitespace},
                :id "dev"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print true,
                 :output-to "client/resources/public/js/test-debug.js",
                 :optimizations :whitespace},
                :id "test"}
               {:source-paths ["client/src" "client/test"],
                :compiler
                {:pretty-print false,
                 :output-to "client/resources/public/js/test-optimized.js",
                 :optimizations :advanced},
                :id "test-advanced"}]})
