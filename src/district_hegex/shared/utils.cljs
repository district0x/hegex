(ns district-hegex.shared.utils
  (:require [clojure.set :as set]
            [district.web3-utils :as web3-utils]
            [cljs-time.format :as tf])
  (:import [goog.async Debouncer]))

(def ^:private simple-date-format (tf/formatter "MM/dd/YY"))

(def ^:private full-format (tf/formatter "MM/dd/YY HH:mm"))

(defn to-simple-time [s]
  (tf/unparse simple-date-format
              (web3-utils/web3-time->local-date-time s)))

(defn to-full-time [s]
  (tf/unparse full-format s))

(def vote-option->kw
  {0 :vote-option/neither
   1 :vote-option/include
   2 :vote-option/exclude})

(def vote-option->num (set/map-invert vote-option->kw))

(def reg-entry-status->kw
  {0 :reg-entry.status/challenge-period
   1 :reg-entry.status/commit-period
   2 :reg-entry.status/reveal-period
   3 :reg-entry.status/blacklisted
   4 :reg-entry.status/whitelisted})

(def reg-entry-status->num (set/map-invert reg-entry-status->kw))

(def kit-district-app->kw
  {0 :voting
   1 :vault
   2 :finance})

(def kit-district-app->num (set/map-invert kit-district-app->kw))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn file-write [filename content & [mime-type]]
  (js/saveAs (new js/Blob
                  (clj->js [content])
                  (clj->js {:type (or mime-type (str "application/plain;charset=UTF-8"))}))
             filename))
