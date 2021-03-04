(ns district-hegex.ui.components.components
  (:require
   [camel-snake-kebab.core :as camel-snake-kebab]
   [oops.core :refer [oget+]]
   #_["@blueprintjs/core" :as blueprint]))

;; verify there's no perf hit with dynamic get-by-str
(defn c [el]
  :div
 #_ (if-not (string? el)
    (oget+ blueprint (camel-snake-kebab/->PascalCaseString el))
    (oget+ blueprint el)))

(defn i [{:keys [i size intent class]}]
  [:div]
  #_[:> (oget+ blueprint "Icon")
   {:icon i
    :intent intent
    :class-name class
    :icon-size (or size "15")}])
