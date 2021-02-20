(ns district-hegex.ui.components.inputs)

(defn- little-arrow []
  [:span.little-arrow
   "⌄"])

(defn select [& children]
  [:div.hegex-select
   [little-arrow]
   [:div.select
    (into [:select] children)]])


