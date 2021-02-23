(ns district-hegex.ui.components.inputs)

(defn- little-arrow []
  [:span.little-arrow
   "⌄"])

(defn select [& children]
  [:div.hegex-select
   [little-arrow]
   [:div.select
    (into [:select] children)]])

(defn text-input [{:keys [type min max on-change]}]
  [:input.hegex-input {:type type
                        :on-change on-change
                        :min min
                        :max max}])
