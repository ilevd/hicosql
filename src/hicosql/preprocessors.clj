(ns hicosql.preprocessors
  (:require [hicosql.utils :as utils]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as string])
  (:import (java.io File)
           (java.util.regex Pattern)))


;; Support Windows an Unix separator
(def separator-pattern
  (re-pattern (str (Pattern/quote "/")
                   "|"
                   (Pattern/quote "\\")
                   "|"
                   (Pattern/quote File/separator))))


(defn relative-path
  "Get relative source path, drop filename and add another relative path:
  \"sql/test.sql\", \"base/common.sql\" => \"sql/base/common.sql\"
  "
  [source-path path]
  (let [parts (string/split source-path separator-pattern)
        path  (string/join File/separator (conj (vec (butlast parts)) path))]
    path))


(declare preprocess)


(defn include
  "Read included file and it values to main values"
  [path result include-path]
  (if (string? include-path)
    (let [full-include-path (relative-path path include-path)
          include-data      (utils/read-yaml full-include-path)
          full-include-data (preprocess full-include-path include-data)]
      (merge result full-include-data))
    ;;  if it's vector, include each file alternately
    (reduce
      (fn [res item]
        (include path res item))
      result
      include-path)))


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