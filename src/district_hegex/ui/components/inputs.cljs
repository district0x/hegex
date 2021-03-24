(ns district-hegex.ui.components.inputs
(:require
   [district-hegex.ui.home.subs :as home-subs]
   [re-frame.core :refer [subscribe]]))

(defn- little-arrow [color]
  [:span.little-arrow {:className (or color "primary")}
   "⌄"])

(defn select [& children]
  (let [color (some-> children first :color)
        c (case color
            :primary :select.primary
            :secondary :select.secondary
            :select.primary)]
    [:div.hegex-select
    [little-arrow color]
    [:div.select
     (into [c] children)]]))

(defn text-input [{:keys [type min max on-change label color ] :as props}]
  [:div.hinput-wrapper
   [:input.hegex-input (merge props {:type      type
                                     :className (or color "primary")
                                     :on-change on-change
                                     :min       min
                                     :max       max})]
   (when label
     ;;NOTE ideally  should be set via less inheritance
     [:div.hinput-label
      label])])

(defn loader "pure css loader" [{:keys [on? color]}]
  (let [c :div.hloader]
    [:span {:style {:display (if on? "initial" "none")}}
    [(or (some->> color name (str (name c) ".") keyword) c)
     [:div]
     [:div]
     [:div]
     [:div]]]))
