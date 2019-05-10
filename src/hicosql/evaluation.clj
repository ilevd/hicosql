(ns hicosql.evaluation
  (:require [hicosql.expression :as exp]
            [clojure.string :as string]))


;; evaluation
(defn get-val
  "Get value from queries map"
  [res ex]
  (get res (str ex)))


(declare evaluate)


(defn eval-symbol [res ex]
  (if-let [subquery (get-val res ex)]
    (cond
      (map? subquery) (:text subquery)
      :else subquery)
    (throw (Exception. (str "Can't get value of: " ex)))
    ;; ex
    ))


(defn replace-function
  "Replace query which is defined as function in form: f(a,b) | ...
   fn-data {:type :function
            :name \"function_name\"
            :params [\"a\", \"b\"]}
   args    (1, \"Hello\", previosly_defined_query)"
  [fn-data args]
  (if (> (count args)
         (count (:params fn-data)))
    (throw (Exception. (str "too many params for query-function: " (:name fn-data) " : " args)))
    (reduce
      (fn [s [param-name value]]
        (string/replace s
                        (re-pattern (str ":" param-name))
                        (str value)))
      (:text fn-data)
      (map vector (:params fn-data) args))))


(defn replace-query
  "Replace params of the form :a :b in text. E.g:
  text - SELECT :a, :b FROM users
  params - (:a 1 :b previosly_defined_query)"
  [text params]
  (if (even? (count params))
    (reduce
      (fn [s [param-name value]]
        (string/replace s
                        (re-pattern (str param-name))
                        (str value)))
      text
      (partition 2 params))
    (throw (Exception. (str "query call should contain even number of params: " params)))))


(defn try-built-in-fns
  "Evaluate undefined function. It is here the evaluation of built-in Clojure functions,
   such as *, / , + , - comes in."
  [fn-sym params]
  ;(prn "built-in: " (cons fn-sym params))
  (eval (cons fn-sym params)))


(defn eval-list
  "Evaluate list"
  [res ex]
  (let [fn-sym (first ex)
        fn-name (str fn-sym)]
    (if (string/ends-with? fn-name "!")

      (let [fn-name (subs fn-name 0 (dec (count fn-name)))]
        (if-let [sub-func-data (get-val res fn-name)]
          (let [text (cond
                       (map? sub-func-data) (:text sub-func-data)
                       (string? sub-func-data) sub-func-data)]
            (replace-query text
                           (doall (map #(evaluate res %) (rest ex)))))
          (throw (Exception. (str "Can't find query " fn-name)))))

      (if-let [sub-func-data (get-val res fn-sym)]
        (if (map? sub-func-data)
          (replace-function sub-func-data
                            (doall (map #(evaluate res %) (rest ex))))
          (throw (Exception. (str "Can't expand the value of: " ex ", cause it's not defined as a function"))))
        (try-built-in-fns fn-sym
                          (doall (map #(evaluate res %) (rest ex))))))))


(defn evaluate
  "Evaluate expression ex with context res - defined queries"
  [res ex]
  ; (prn "evaluate: " ex)
  (cond
    (symbol? ex) (eval-symbol res ex)
    (list? ex) (eval-list res ex)
    ;; else can be string, integer
    :else ex))


(defn evaluate-all
  "Replace all expressions starting with & in string s"
  [res s]
  (let [exprs (-> s exp/get-expressions reverse)]
    (reduce
      (fn [result-s expr-data]
        ;(prn (read-string (:expr expr-data)))
        (str
          (subs result-s 0 (:start expr-data))
          (evaluate res (-> expr-data :expr read-string))
          (subs result-s (:end expr-data))))
      s
      exprs)))
