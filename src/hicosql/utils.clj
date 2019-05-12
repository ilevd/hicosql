(ns hicosql.utils
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [yaml.core :as yaml]))


;; BASE
(defn read-resource [path]
  (slurp
    (or (io/resource path)
        (throw (Exception. (str "Cannot open resource: " path))))))


(defn parse-yaml [data]
  (yaml/parse-string data :keywords false))


(defn read-yaml [path]
  (parse-yaml (read-resource path)))


;; STRINGS
(defn str->key
  "  \"&key\" -> key "
  [s]
  (subs s 1))


(defn function? [key]
  (re-matches #"[a-zA-Z0-9_]+\([a-zA-Z0-9_, ]+\)" key))


;; deprecated
(defn templated? [key]
  (string/includes? key ":"))


(defn original-name
  "req:template -> req"
  [key]
  (-> key (string/split #":") first))


(defn template-name
  "req:templ -> templ"
  [key]
  (-> key (string/split #":") second))
