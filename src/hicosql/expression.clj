(ns hicosql.expression
  (:require [clojure.string :as string]))


;; expression functions
(defn re-seq-pos
  "Returns a lazy sequence of successive matches of pattern in string,
  using java.util.regex.Matcher.find(), each such match processed with
  re-groups and returns start and end positions"
  [^java.util.regex.Pattern re s]
  (let [m (re-matcher re s)]
    ((fn step []
       (when (. m (find))
         (cons {:start (.start m)
                :group (re-groups m)
                :end   (.end m)}
               (lazy-seq (step))))))))


(defn name-char? [c]
  (re-matches #"[a-zA-Z0-9_-]" (str c)))


(defn whitespace-char? [c]
  (re-matches #"\s" (str c)))


(defn get-end-position-by-bracket
  "Find the ending position of expression starting from start position."
  [s start]
  (loop [num 0 pos start]
    (let [begin (= start pos)
          c (when (< pos (count s))
              (.charAt s pos))]
      ;(prn pos (.charAt s pos))
      (cond
        (nil? c) (if (zero? num)
                   pos
                   (throw (Exception. (str "Cannot find end of expression from position " start " in: " (subs s start)))))

        (= \( c) (if (and (zero? num)
                          (not begin))
                   pos
                   (recur (inc num) (inc pos)))

        (= \) c) (cond
                   (zero? num) pos
                   (= 1 num) (inc pos)
                   :else (recur (dec num) (inc pos)))

        (zero? num) (if (name-char? c)
                      (recur num (inc pos))
                      pos)

        (and (pos? num)
             (= \& c)) (throw (Exception. (str "Expression couldn't contain subexpression starting with & at pos: "
                                               pos " in \"" (subs s start (inc pos)) "\"")))

        :else (recur num (inc pos))))))


(defn get-expressions
  "Find all expressions starting from & and returns them in form:
  [{:start <start-pos>
    :end   <end-pos>
    :expr  <(f1 arg1 (f2 arg 2))> | <symbol>} ... ]"
  [s]
  (let [starts (re-seq-pos #"&" s)
        exprs (map (fn [data]
                     (let [start (:start data)
                           end (get-end-position-by-bracket s (inc start))]
                       {:start start
                        :end   end
                        :expr  (subs s (inc start) end)}))
                   starts)
        exprs (filter #(not= "&" (:expr %)) exprs)]
    exprs))
