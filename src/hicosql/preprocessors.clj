(ns hicosql.preprocessors
  (:require [hicosql.utils :as utils]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as string])
  (:import (java.io File)))


(defn relative-path
  "Get relative source path, drop filename and add another relative path:
  \"sql/test.sql\", \"base/common.sql\" => \"sql/base/common.sql\"
  "
  [source-path path]
  (let [parts (string/split source-path (re-pattern File/separator))
        path (string/join File/separator (conj (vec (butlast parts)) path))]
    path))


(defn include
  "Read included file and it values to main values"
  [path m include-path]
  (let [include-data (utils/read-yaml (relative-path path include-path))]
    (merge m include-data)))


(defn preprocess
  "Preproccess key-values pairs for key started with '__', e.g.: '__include'"
  [path data]
  (reduce
    (fn [result [key value]]
      (case key
        "__include" (include path result value)
        (assoc result key value)))
    (ordered-map)
    data))