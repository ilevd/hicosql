(ns hicosql.core
  (:require [hicosql.substitution :as hico-main]
            [hicosql.utils :as utils]))


;; MAIN API
(defn run-file [path]
  (hico-main/substitute-all (utils/read-yaml path) path))
