(ns pipelines.pipes)

(defn str->lines
  [x]
  (clojure.string/split x #"\n"))

(defn lines->str
  [x]
  (clojure.string/join "\n" x))

(defn sort
  [x]
  (lines->str (clojure.core/sort (str->lines x))))

(defn dedupe
  [xs]
  (concat [(first xs)] (mapcat #(when (not= % %2) [%]) (rest xs) xs)))

(defn uniq
  [x]
  (lines->str (dedupe (str->lines x))))

(defn wc
  [x]
  (let [lines (count (str->lines x))
        chars (count x)
        words (count (clojure.string/split x #"\s+"))]
    (str lines "\t" words "\t" chars)))
