(ns methojure.test.macros
  (:require [cljs.analyzer :refer (*cljs-ns*)]
            [clojure.template :as temp]))

(defmacro before-each [& body]
  `(methojure.test.core/before-each ~(str *cljs-ns*) (fn [] ~@body)))

(defmacro after-each [& body]
  `(methojure.test.core/after-each ~(str *cljs-ns*) (fn [] ~@body)))

(defmacro set-up [& body]
  `(methojure.test.core/set-up ~(str *cljs-ns*) (fn [] ~@body)))

(defmacro tear-down [& body]
  `(methojure.test.core/tear-down ~(str *cljs-ns*) (fn [] ~@body)))

(defmacro deftest [test-title & body]
  `(methojure.test.core/deftest
     ~(str *cljs-ns*)
    ~(clojure.string/replace (clojure.core/name test-title) #"-" " ")
    (fn [] ~@body)))

(defn assert-predicate
  [msg form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (when-not result#
         (goog.testing.asserts/raiseException_
          ~msg
          (str "  expected: " (pr-str '~form) "\n"
               "    actual: " (pr-str (list '~'not (cons '~pred values#)))
               "\n"))))))

(defmacro is
  ([form] `(is ~form nil))
  ([form msg] `~(assert-predicate msg form)))


(defmacro done []
  `(methojure.test.core/done ~(str *cljs-ns*)))

(defmacro wait-for-async [msg]
  `(methojure.test.core/wait-for-async ~(str *cljs-ns*) ~msg))

(defmacro are
  [argv expr & args]
  (if (or
       ;; (are [] true) is meaningless but ok
       (and (empty? argv) (empty? args))
       ;; Catch wrong number of args
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    `(temp/do-template ~argv (is ~expr) ~@args)
    (throw (IllegalArgumentException.
            "The number of args doesn't match are's argv."))))
