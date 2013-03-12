(defproject methojure "0.1.0-SNAPSHOT"
  :description "A clojure/clojurescript web framework similar to meteor.js"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[methojure/sockjs "0.1.0-SNAPSHOT"]]
  :plugins [[lein-sub "0.2.4"]]
  :sub ["methojure-sockjs" "methojure-test"])
