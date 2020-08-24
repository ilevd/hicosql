(ns hicosql.core-test
  (:require [clojure.test :refer :all]
            [hicosql.core :refer :all]))


(deftest run-file-test
  (testing "FIXME, I fail."
    (is (= (run-file "../test/data/main.sql"))
        (assoc (flatland.ordered.map/ordered-map)
          :a 10
          :b 20
          :main_req "hello world"))))
