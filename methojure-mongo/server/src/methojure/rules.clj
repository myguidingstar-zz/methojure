(ns methojure.rules
  (:refer-clojure :exclude [==])
  (:require [cljx.rules :as cljx])
  (:use [clojure.core.logic :only [matche conde pred lvar == firsto]]
        [kibit.rules.util :only [compile-rule defrules]]))

(def cljs-protocols
  (let [x (lvar)]
    [#(conde
       ((== % 'clojure.lang.IFn)  (== x 'IFn))
       ((== % 'clojure.lang.IDeref)  (== x 'IDeref))
       ;;other protocol renaming goes here
       )
     #(== % x)]))

(def cljs-rules
  (concat cljx/cljs-rules
          [cljs-protocols]))
