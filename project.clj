(defproject hicosql "0.2.2"
  :description "Highly Configurable SQL"
  :url "https://github.com/ilevd/hicosql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; :main ^:aot hicosql.core
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.flatland/ordered "1.5.9"]
                 [io.forward/yaml "1.0.9" :exclusions [org.flatland/ordered]]
                 [instaparse "1.4.10"]])
