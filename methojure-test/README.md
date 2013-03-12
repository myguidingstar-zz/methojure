# Methojure Test

Methojure Test is Clojurescript test library based on the Google Closure testing framework.
It has the advantage that it will also work in advanced compilation. To run the test
[testacular](http://testacular.github.com/0.6.0/index.html) is used.

## Getting started
* Install testacular on node.js: `npm install -g testacular`
* Create a `testacular.conf.js` file in your project. Have look at this project
  to see an example.
* Build your code using leiningen cljsbuild: `lein cljsbuild once`
* start a testacular server: `testacular start`
* run the test: `testacular run`

## Writing test

See `client/test/methojure/test/test/core.cljs` for an example.
