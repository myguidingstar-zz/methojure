(ns methojure.test.core
  (:require [clojure.string :as str]
            [goog.testing.asserts :as gasserts]
            [goog.testing.TestRunner :as gTestRunner]
            [goog.testing.AsyncTestCase :as gTestCase]
            [goog.testing.TestCase.Test :as gTest]))

;; Set the variable to true for additionally output
(def *speak* false)

;; Each namespace has is on test. This makes it easier
;; to add `setUpPage`, `setUp`, `tearDown` and `tearDownPage`
;; methods.
(def *tests* (atom {}))

(defn create-test-case
  "Creates a new asynchronous test case"
  [name]
  (let [testcase (goog.testing.AsyncTestCase. name)]
    ;; don't speak...
    (when-not *speak*
      (set! (.-log testcase) (fn [] nil)))
    testcase))

(defn create-test-case-if-not-exists [ns]
  (when-not (@*tests* ns)
    (swap! *tests* assoc ns (create-test-case ns))))

(defn deftest
  "Adds a new test to the testcase of new namespace. If there
   is no testcase for the namespace a new one is created.

   @param {String} ns The namespace where the test lives.
   @param {String} name The name of the test.
   @param {Function} f The test itsself."
  [ns name f]
  (create-test-case-if-not-exists ns)
  (.add (@*tests* ns) (goog.testing.TestCase.Test. name f)))

(defn done
  "call done to indicate a asynchronous test that it is finished."
  [ns]
  (.continueTesting (@*tests* ns)))

(defn wait-for-async
  "calling this method in a asynchronous test tells the test
   that its need to wait for a `done`"
  [ns & [msg]]
  (.waitForAsync (@*tests* ns) msg))

(defn set-up
  "registers a function that is call once before any test are executed"
  [ns f]
  (create-test-case-if-not-exists ns)
  (set! (.-setUpPage (@*tests* ns)) f))

(defn before-each
  "registers a function that is call before a test is executed"
  [ns f]
  (create-test-case-if-not-exists ns)
  (set! (.-setUp (@*tests* ns)) f))

(defn after-each
  "registers a function that is call after a test is executed"
  [ns f]
  (create-test-case-if-not-exists ns)
  (set! (.-tearDown (@*tests* ns)) f))

(defn tear-down
  "registers a function that is after all test are executed"
  [ns f]
  (create-test-case-if-not-exists ns)
  (set! (.-tearDownPage (@*tests* ns)) f))


;; testacular runner

(defn testacular-on-success [tc testcase]
  (fn [test]
    (set! (.-successCount (.-result_ testcase))
          (inc (.-successCount (.-result_ testcase))))
    ((aget tc "result") (clj->js {:description (.-name test)
                                  :suite [(.-name_ testcase)]
                                  :success true
                                  :skipped 0
                                  :time 0
                                  :log []}))))

(defn testacular-on-error [tc testcase]
  (fn [test e]
    (let [err (.toString (.logError testcase (.-name test) e))]
      ((aget tc "result") (clj->js {:description (.-name test)
                                    :suite [(.-name testcase)]
                                    :success false
                                    :skipped 0
                                    :time 0
                                    :log [err]})))))

(defn testacular-on-complete [tc runner]
  (fn []
    (let [result (.-result_ (.-testCase runner))
          total (reduce + (map #(.-totalCount (.-result_ %)) (vals @*tests*)))]
      ((aget tc "info")
       (js-obj "total" total))
      ((aget tc "complete")
       (js-obj "coverage" (aget js/window "__coverage__"))))))

(defn run-test-suite [tc tests]
  (let [tr (goog.testing.TestRunner.)
        test (first tests)]
    (.initialize tr test)
    (set! (.-doSuccess test) (testacular-on-success tc test))
    (set! (.-doError test) (testacular-on-error tc test))
    (set! (.-onComplete_ tr) (if (> (count tests) 1)
                               (fn [] (run-test-suite tc (rest tests)))
                               (testacular-on-complete tc tr)))
    (.execute tr)))

(defn testacular-runner [tc]
  (fn []
    (if (> (count @*tests*) 0)
      (run-test-suite tc (vals @*tests*))
      (do
        ((aget tc "info") (js-obj "total" 0))
        ((aget tc "complete")
         (js-obj "coverage" (aget js/window "__coverage__")))))))

(defn testacular-dump [tc serialize]
  (fn [& args]
    (.info tc (js-obj "dump" (clj->js (map serialize args))))))


(defn initialize-testacular []
  (let [testacular (or (aget js/window "__karma__") (js-obj))]
    (aset testacular "start" (testacular-runner testacular))
    (aset js/window "dump"
          (testacular-dump
           testacular
           (fn [v]
             (and (aget js/window "angular")
                  (aget (aget js/window "angular") "mock")
                  (or ((aget (aget (aget js/window "angular") "mock") "dump") v)
                      v)))))))

(initialize-testacular)
