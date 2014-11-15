(ns pipelines.pipes)

(defn lines
  [x]
  (clojure.string/split x #"\n"))

(defn sort
  [x]
  (clojure.core/sort (lines x)))
