(ns methojure.mongo.api
  (:refer-clojure :exclude [find remove count]))

(defprotocol IMongoCursor
  (fetch [this])
  (count [this]))

(defprotocol IMongoCollection
  (find [this] [this selector] [this selector options])
  (find-one [this] [this selector] [this selector options])
  (insert [this doc])
  (remove [this selector])
  (update [this selector mod] [this selector mod options]))
