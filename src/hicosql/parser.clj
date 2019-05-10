(ns hicosql.parser
  (:require [instaparse.core :as insta]))


(def function-parser
  (insta/parser
    "
    main = name w? <'('> w? params w? <')'> w?
    params = name (w? <','> w? name)*
    <name> = #\"[a-zA-Z0-9_-]+\"
    <w> = <#'\\s+'>
    "))


(defn parse-function-def [s]
  (let [data (function-parser s)]
    {:type   :function
     :name   (second data)
     :params (-> data last rest)}))