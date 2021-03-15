(ns district-hegex.ui.components.components
  (:require
   [camel-snake-kebab.core :as camel-snake-kebab]
   [oops.core :refer [oget+]]))

;; verify there's no perf hit with dynamic get-by-str
(defn c [el]
  :div)

(defn i [{:keys [i size intent class]}]
  [:div])
