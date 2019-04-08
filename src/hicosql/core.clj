(ns hicosql.core
  (:require [clojure.string :as string]
            [yaml.core :as yaml]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.java.io :as io]))

;(defn get-key [data key]
;  (select-one [(codewalker #(and (map? %) (get % key))) key]
;              data))
;
;
;(defn substitute-queries-one [data]
;  (transform [MAP-VALS map? MAP-VALS string?]
;             (fn [s]
;               (prn "string: " s)
;               (string/replace s #"&[a-z0-9_-]+" (fn [key]
;                                                   (let [k (keyword (subs key 1))]
;                                                     (prn "key2: " k)
;                                                     (str (get-key data k))))))
;             data))
;
;
;(defn substitute-queries [data]
;  (loop [data data]
;    (if (seq (select [MAP-VALS map? MAP-VALS #(string? %) #(re-find #"&[a-z0-9_-]+" %)]
;                     data))
;      (recur (substitute-queries-one data))
;      data)))
;
;
;(defn substitute-templates [data]
;  (let [derived-keys (select [MAP-VALS MAP-KEYS #(string/includes? (name %) ":")]
;                             data)]
;    derived-keys))


(defn str->key
  "  \"&key\" -> :key "
  [s]
  (keyword (subs s 1)))


(defn substitute-queries* [val data]
  (string/replace val #"&[a-z0-9_-]+" (fn [key] (->> key str->key (get data) str))))


(defn substitute-template* [parent data res]
  ;(prn "SUBS TEMP: " parent data res)
  (reduce (fn [s [k v]]
            (string/replace s
                            (->> k name (str "@") re-pattern)
                            v))
          (get res parent)
          data))


(defn templated? [key]
  (string/includes? (name key) ":"))


(defn original-name
  ":req:template -> :req"
  [key]
  (-> key name (string/split #":") first keyword))


(defn template-name
  ":req:templ -> :templ"
  [key]
  (-> key name (string/split #":") second keyword))


(defn substitute-all
  "Сделать все замены вида &key и @fields"
  [data]
  (reduce (fn [res [key val]]
            ;(prn :key key)
            ;(prn :val val)
            (cond
              (string? val) (assoc res key (substitute-queries* val res))
              (templated? key) (assoc res
                                 (original-name key)
                                 (substitute-template* (template-name key) val res))
              :else (assoc res key val)))
          (ordered-map)
          data))



(defn read-resource [path]
  (slurp (io/resource path)))


(defn read-yaml [path]
  (yaml/parse-string (read-resource path)))


(defn run-hico [path]
  (let [raw (slurp (io/resource path))
        data (yaml/parse-string raw)]
    (substitute-all data)))
