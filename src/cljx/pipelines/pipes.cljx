(ns pipelines.pipes
  (:use [clojure.string :only [split trim join]])
  (:require
   #+cljs [cljs.tools.cli :refer [parse-opts]]
   #+clj [clojure.tools.cli :refer [parse-opts]]
   ))

(defn quoted-split [s]
  (lazy-seq
   (when-let [c (first s)]
     (cond
      (= " " c)
      (quoted-split (rest s))
      (= \' c)
      (let [[w* r*] (split-with #(not= \' %) (rest s))]
        (if (= \' (first r*))
          (cons (apply str w*) (quoted-split (rest r*)))
          (cons (apply str w*) nil)))
      :else
      (let [[w r] (split-with #(not (= " " %)) s)]
        (cons (apply str w) (quoted-split r)))))))

(defn str->lines
  [x]
  (split x #"\n"))

(defn lines->str
  [x]
  (join "\n" x))

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

(def wc-opts
  [["-l" "--lines" "Print number of lines"]
   ["-c" "--chars" "Print number of characters"]
   ["-w" "--words" "Print number of words"]])

(defn wc
  ([x] (wc x "-l -c -w"))
  ([x args]
     (let [opts (:options (parse-opts (quoted-split args) wc-opts))
           lines (if (opts :lines) (count (str->lines x)))
           chars (if (opts :chars) (count x))
           words (if (opts :words) (count (split x #"\s+")))
           result (remove nil? [lines chars words])]
       (join "\t" result))))

(def fgrep-opts
  [["-i" "--ignore-case" "Case insensitive"]])

(def grep-opts
  (cons ["-e" "--egrep" "Support regular expressions"]
        fgrep-opts))

(defn fgrep
  [x args]
  (let [{:keys [arguments options errors summary]} (parse-opts (quoted-split args) fgrep-opts)
        pattern (first arguments)]
    (if-not pattern
      (str errors "\n" summary)
      (let [lines (str->lines x)]
        (if (:ignore-case options)
          (lines->str (remove #(= -1 (.indexOf (clojure.string/lower-case %)
                                               (clojure.string/lower-case pattern))) lines))
          (lines->str (remove #(= -1 (.indexOf % pattern)) lines)))))))

(defn egrep
  [x args]
  (let [{:keys [arguments options errors summary]} (parse-opts (quoted-split args) fgrep-opts)
        pattern (first arguments)]
    (if-not pattern
      (str errors "\n" summary)
      (let [lines (str->lines x)
            meta (if (:ignore-case options) "(?i)")
            regexp (re-pattern (str meta ".*" pattern ".*"))]
        (lines->str (filter #(re-matches regexp %) lines))))))

(defn grep
  [x args]
  (print grep-opts)
  (let [{:keys [arguments options errors summary]} (parse-opts (quoted-split args) grep-opts)]
    (print options)
    (if (:egrep options)
      (egrep x args)
      (fgrep x args))))

(defn sed
  ([x] "sed requires an argument")
  ([x args]
     (if args
       (let [[cmd in out scope] (split (clojure.string/replace args "'" "") #"/")]
         (if (and (= cmd "s"))
           (let [replacer #(clojure.string/replace % (re-pattern in) out)]
             (lines->str (map replacer (str->lines x))))
           (str "Unknown args " args))))))

(defn err
  [x]
  "Unknown function")

(def fn-choices
  (sorted-map
   "echo" identity
   "sort" sort
   "uniq" uniq
   "shuffle" shuffle
   "fgrep" fgrep
   "egrep" egrep
   "grep" grep
   "sed" sed
   "wc" wc))

(defn lookup-fn
  "Given a string like 'wc -l', return a new function which takes one
  argument (s) and calls wc with the -l argument."
  [x]
  (let [[fname args] (split x #"\s" 2)
        f (fn-choices fname err)]
    (if args #(f % args) f)))

(defn apply-with-catch
  [fn in]
  (try
    (fn in)
    (catch
        #+clj Exception #+cljs js/Object
        e (str "Error: " e))))

(defn chain-fns
  "Apply each fn in fns consecutively, piping the output of the
  previous fntion into the next"
  [fns in]
  (if (not-empty fns)
    (let [fn (lookup-fn (first fns))
          r (apply-with-catch fn in)]
      (if (not-empty (rest fns))
        (cons r (chain-fns (rest fns) r))
        [r]
        ))
    []))

(defn compute-pipeline
  [pipeline in]
  (chain-fns
   (map trim (split pipeline #"\|"))
   in))
