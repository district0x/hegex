(ns district-hegex.ui.components.inputs
(:require
   [reagent.core :as r]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       gget
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [district-hegex.ui.home.subs :as home-subs]
   [re-frame.core :refer [subscribe]]))

(defn- little-arrow [color]
  [:span.little-arrow {:className (or color "primary")}
   ])

(defn text-input [{:keys [type step min max on-change label color size] :as props}]
  (println "stepis" step)
  (let [el :div.hinput-wrapper
        ->sized #(keyword (str (name %) "." (name size)))]
    [(if-not size el (->sized el))
     [:span.stock-input
      [:input.hegex-input (merge props {:type      type
                                        :step      step
                                        :className (or color "primary")
                                        :on-change on-change
                                        :min       min
                                        :max       max})]]
    (when label
      ;;NOTE ideally  should be set via less inheritance
      [:div.hinput-label
       label])]))

(defn loader "pure css loader" [{:keys [on? color]}]
  (let [c :div.hloader]
    [:span {:style {:display (if on? "initial" "none")}}
    [(or (some->> color name (str (name c) ".") keyword) c)
     [:div]
     [:div]
     [:div]
     [:div]]]))

(defn fancy-select []
  (let [on-change #(js/console.log (oget % ".target.value"))]
    [:div.fancy-input.primary
    [:div {:class "select secondary"
           :tabindex "1"}
     [little-arrow :secondary]
     [:input {:class "fancy"
              :on-change on-change
              :type "radio"
              :id "opt1"
              :value "oranges"
              }]
     [:label {:for "opt1" :class "option"} "Oranges"]
     [:input {:class "fancy"
              :on-change on-change
              :value "apples"
              :checked true
              :type "radio"
              :id "opt2"}]
     [:label {:for "opt2" :class "option"} "Apples"]]]))

(defn select [& children]
  (let [color (some-> children first :color)
        size (some-> children first :size)
        c (case color
            :primary :select.primary
            :secondary :select.secondary
            :yellow :select.yellow
            :select.primary)
        s (case size
            :small :div.hegex-select.small
            :div.hegex-select)]
    [:span.stock-input
     [s
      [little-arrow color]
      [:div.select
       (into [c] children)]]]))
