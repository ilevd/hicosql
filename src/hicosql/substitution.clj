(ns hicosql.substitution
  (:require [hicosql.evaluation :as hico-eval]
            [hicosql.utils :refer :all]
            [hicosql.parser :as parser]
            [hicosql.expression :as expr]
            [clojure.string :as string]
            [flatland.ordered.map :refer [ordered-map]]))


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


;; =====================================================================================================================
;; Substitution
;; =====================================================================================================================
(defn make-function [res key val]
  (let [data (parser/parse-function-def key)
        func-name (:name data)]
    (assoc res func-name
               (assoc data :text (hico-eval/evaluate-all res val)))))


(defn substitute-all
  "Parses and creates functions, makes substitutions and expansions"
  [data]
  (->> data
       ;; main queries evaluation
       (reduce (fn [res [key val]]
                 (cond
                   (function? key) (make-function res key val)
                   (string? val) (assoc res key (hico-eval/evaluate-all res val))
                   :else (assoc res key val)))
               (ordered-map))
       ;; convert function types to string, string keys to keywords
       (map (fn [[k v]]
              [(keyword k)
               (cond
                 (map? v) (:text v)
                 :else v)]))
       (into (ordered-map))))
