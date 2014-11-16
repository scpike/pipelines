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

(defn shuffle
  [x]
  (lines->str (clojure.core/shuffle (str->lines x))))

(defn wc
  [x]
  (let [lines (count (str->lines x))
        chars (count x)
        words (count (clojure.string/split x #"\s+"))]
    (str lines "\t" words "\t" chars)))

(def func-choices
  (sorted-map
   "echo" identity
   "sort" sort
   "uniq" uniq
   "shuffle" shuffle
   "wc" wc))

(defn lookup-func
  [x]
  (func-choices
   x
   identity))

(defn compute-pipeline
  [funcs in]
  (if (not-empty funcs)
    (let [fn (lookup-func (first funcs))
          r (fn in)]
      (if (not-empty (rest funcs))
        (cons r (compute-pipeline (rest funcs) r))
        [r]
        ))
    []))
