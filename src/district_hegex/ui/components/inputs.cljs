(ns district-hegex.ui.components.inputs
(:require
   [district-hegex.ui.home.subs :as home-subs]
   [re-frame.core :refer [subscribe]]))

(defn- little-arrow []
  [:span.little-arrow.primary
   "⌄"])

(defn select [& children]
  [:div.hegex-select
   [little-arrow]
   [:div.select
    (into [:select.primary] children)]])

(defn text-input [{:keys [type min max on-change label] :as props}]
  [:div.hinput-wrapper
   [:input.hegex-input.primary (merge props {:type      type
                                             :on-change on-change
                                             :min       min
                                             :max       max})]
   (when label
     ;;NOTE ideally  should be set via less inheritance
     [:div.hinput-label
      label])])
