(ns hicosql.core
  (:require [hicosql.substitution :as hico-main]
            [yaml.core :as yaml]
            [clojure.java.io :as io]))


;; MAIN API
(defn read-resource [path]
  (slurp (io/resource path)))


(defn parse-yaml [data]
  (yaml/parse-string data :keywords false))


(defn read-yaml [path]
  (parse-yaml (read-resource path)))


(defn run-file [path]
  (let [raw (read-resource path)
        data (parse-yaml raw)]
    (hico-main/substitute-all data)))


(defn run-str [s]
  (-> s parse-yaml hico-main/substitute-all))