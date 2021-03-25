(ns district-hegex.ui.home.orderbook
  (:require
   [re-frame.core :refer [subscribe dispatch]]
    [district-hegex.ui.trading.events :as trading-events]
   [district-hegex.ui.external.subs :as external-subs]
    [district-hegex.shared.utils :refer [to-simple-time debounce]]
   [district-hegex.ui.components.inputs :as inputs]
   [district.format :as format]
   [district-hegex.ui.home.subs :as home-subs]
   [district-hegex.ui.home.events :as home-events]
   [reagent.core :as r]))

(def ^:private table-state (r/atom {:draggable false}))

(def ^:private columns [{:path   [:option-type]
                         :header "Option Type"
                         :attrs  (fn [data] {:style {:text-align     "left"
                                                    :text-transform "capitalize"}})
                         :key    :option-type}
                        {:path   [:asset]
                         :header "Currency"
                         :format (fn [v] (case v 1 "WBTC" 0 "ETH" "ETH"))
                         :attrs  (fn [data] {:style {:text-align     "left"
                                                    :text-transform "uppercase"}})
                         :key    :asset}
                        {:path   [:amount]
                         :header "Option Size"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :amount}
                        {:path   [:strike]
                         :header "Strike Price"
                         :format (fn [v] (str "$" v))
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :strike}
                        {:path   [:eth-price]
                         :header "Price"
                         :format (fn [v] (str v " ETH"))
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :eth-price}
                        ;;NOTE a bit cryptic model, P&L is fetched later via (price+-strike(+-premium*price))
                        ;;NOTE P&L with premium is inaccurate since we _can't_ fetch historical price for premium
                        {:path   [:p&l]
                         :header "P&L"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :p&l}
                        ;;NOTE requires parse-log info
                        #_{:path   [:holding-period]
                         :header "Holding Period"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :holding-period}
                        {:path   [:expiration]
                         :header "Expires On"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :expiration}
                        {:path   [:sra-order :metaData :createdAt]
                         :header "Offered"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :format (fn [v] (when v (to-simple-time v)))
                         :key    :created-at}
                        #_{:path   [:actions]
                         :header "Actions"
                         :attrs  (fn [data] {:style {:text-align     "left"
                                                    :text-transform "uppercase"}})
                         :key    :actions}])

(defn- row-key-fn
  "Return the reagent row key for the given row"
  [row row-num]
  (get-in row [:hegic-id]))

(defn- cell-data
  "Resolve the data within a row for a specific column"
  [row cell]
  (let [{:keys [path expr]} cell]
    (or (and path
             (get-in row path))
        (and expr
             (expr row)))))

(defn p&l [[paid strike amount] option-type]
  (let [current-price @(subscribe [::external-subs/eth-price])
        pl (if (= :call option-type)
             (- (* current-price amount) (* strike amount) paid)
             (- (* strike amount) (* current-price amount) paid))]
    ;;NOTE recheck P&L formula (esp. premium)
    [:div (str "$"
               (some->
                pl
                (format/format-number {:max-fraction-digits 5})))]))

(defn- cell-fn
"Return the cell hiccup form for rendering.
 - render-info the specific column from :column-model
 - row the current row
 - row-num the row number
 - col-num the column number in model coordinates"
[render-info row row-num col-num]
(let [{:keys [format attrs key]
       :or   {format identity
              attrs (fn [_] {})}} render-info
      data    (cell-data row render-info)
      content (format data)
      attrs   (attrs data)
      active-option @(subscribe [::home-subs/my-orderbook-option])
      selected? (= row-num (:row-num active-option))]
  (println "selected?" selected? active-option)
  [:a
   (merge-with merge attrs  {:on-click (fn []
                                         (if selected?
                                           (dispatch
                                            [::home-events/set-orderbook-active-option nil nil])

                                           (dispatch
                                            [::home-events/set-orderbook-active-option
                                             row row-num])))
                             :class-name (when selected? "hegexyellow")
                             :style {:padding "10px"
                                              :outline "none"
                                              :cursor "pointer"
                                              :display "flex"
                                              :align-items "center"
                                              :min-height "47px"
                                              :position "relative"}})
   #_(even? row-num) #_(assoc-in [:style :background-color] "#212c35")
#_  [:div "xxxx"]
   (case key
     :p&l [p&l data (:option-type row)]
     :option-type [:div {:style {:margin-left "10px"}} content]
     content)
   #_(case col-num
     ;;NOTE
     ;;moved below the table
     #_0  #_(if (:hegex-id row)
          [exercise-badge (:hegex-id row)]
          [wrap-hegic (:hegic-id row)])
     total-cols (if (:hegex-id row)
                  [nft-badge (:hegex-id row)]
                  [:<>])
     content)]))

(defn date?
  "Returns true if the argument is a date, false otherwise."
  [d]
  (instance? js/Date d))

(defn date-as-sortable
  "Returns something that can be used to order dates."
  [d]
  (.getTime d))

(defn compare-vals
  "A comparator that works for the various types found in table structures.
  This is a limited implementation that expects the arguments to be of
  the same type. The :else case is to call compare, which will throw
  if the arguments are not comparable to each other or give undefined
  results otherwise.
  Both arguments can be a vector, in which case they must be of equal
  length and each element is compared in turn."
  [x y]
  (cond
    (and (vector? x)
         (vector? y)
         (= (count x) (count y)))
    (reduce #(let [r (compare (first %2) (second %2))]
               (if (not= r 0)
                 (reduced r)
                 r))
            0
            (map vector x y))

    (or (and (number? x) (number? y))
        (and (string? x) (string? y))
        (and (boolean? x) (boolean? y)))
    (compare x y)

    (and (date? x) (date? y))
    (compare (date-as-sortable x) (date-as-sortable y))

    :else ;; hope for the best... are there any other possiblities?
    (compare x y)))

(defn- sort-fn
  "Generic sort function for tabular data. Sort rows using data resolved from
  the specified columns in the column model."
  [rows column-model sorting]
  (sort (fn [row-x row-y]
          (reduce
            (fn [_ sort]
              (let [column (column-model (first sort))
                    direction (second sort)
                    cell-x (cell-data row-x column)
                    cell-y (cell-data row-y column)
                    compared (if (= direction :asc)
                               (compare-vals cell-x cell-y)
                               (compare-vals cell-y cell-x))]
                (when-not (zero? compared)
                  (reduced compared))
                ))
            0
            sorting))
        rows))

(def table-props
  {:table-container {:style {:border-radius "5px"
                             :text-align    "center"
                             :padding-top   "15px"}}
   :table-state     table-state
   :table           {:style {:margin "auto"}}
   :column-model    columns
   :row-key         row-key-fn
   :render-cell     cell-fn
   :sort            sort-fn})

(defn controls []
  (let [active-option @(subscribe [::home-subs/my-orderbook-option])]
    (println "active option is" active-option)
    [:div {:style {:max-width "500px"
                   :margin-left "auto"}}
     #_(str active-option)
     [:div.box-grid
      [:div.box.e
       [inputs/text-input
        {:type :number
         :color "yellow"
         :disabled true
         :min 0
         :placeholder (str (-> active-option :option (:eth-price 0)) " ETH")}]]
      [:div.box.e
       [:button.yellow
        {:className (when-not active-option "disabled")
         :on-click #(dispatch [::trading-events/fill-offer (:option active-option)])
         :disabled  (not active-option)}
        "Buy"]]]]))
