(ns hicosql.core-test
  (:require [clojure.test :refer :all]
            [hicosql.core :refer :all]))


(deftest run-file-test
  (testing "__include test"
    (is (= (run-file "../test/data/main.sql"))
        (flatland.ordered.map/ordered-map
          :a 300
          :a2 5
          :b 10
          :c 20
          :main_req "hello world"))))
