(ns district-hegex.shared.macros
  (:require
   [clojure.java.io :as io]))

(defmacro slurp-resource [s]
  (-> s
    io/resource
    io/reader
    slurp))

(defmacro get-environment []
  (let [env "prod"  #_(or (System/getenv "DISTRICT_HEGEX_ENV") "qa")]
    ;; Write to stderr instead of stdout because the cljs compiler
    ;; writes stdout to the raw JS file.
    (binding [*out* *err*]
      (println "Building with environment:" env))
    env))
