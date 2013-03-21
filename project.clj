(defproject methojure "0.1.0-SNAPSHOT"
  :description "A clojure/clojurescript web framework similar to meteor.js"
  :url "https://github.com/jenshaase/methojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.5.0"]]
  
  :plugins [[lein-sub "0.2.4"]]
  
  :sub ["methojure-sockjs"
        "methojure-test"])
