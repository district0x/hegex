(ns district-hegex.ui.components.inputs
(:require
   [district-hegex.ui.home.subs :as home-subs]
   [re-frame.core :refer [subscribe]]))

(defn- little-arrow []
  [:span.little-arrow
   "⌄"])

(defn select [& children]
  [:div.hegex-select
   [little-arrow]
   [:div.select
    (into [:select] children)]])

(defn text-input [{:keys [type min max on-change label]}]
  (let [dark? @(subscribe [::home-subs/dark-mode?])]
    [:div.hinput-wrapper
    [:input.hegex-input {:type type
                         :on-change on-change
                         :min min
                         :max max}]
    (when label
      ;;NOTE ideally  should be set via less inheritance
      [:div.hinput-label {:style {:background-color (if dark? "black" "white")}}
       label])]))
