(ns district-hegex.ui.components.tabs
  (:require
   [district.ui.router.utils :as router-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn tab [{:keys [caption]}]
  [:div.tab caption])

(defn tabs ^:WIP [])
