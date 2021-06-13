(ns district-hegex.ui.home.orderbook
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [district.ui.web3-tx.subs :as tx-subs]
   [bignumber.core :as bn]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [clojure.string :as cs]
   [district.ui.web3-accounts.subs :as account-subs]
   [district.web3-utils :as web3-utils]
   [district.ui.web3-account-balances.subs :as account-balances-subs]
   [district-hegex.ui.weth.subs :as weth-subs]

   [oops.core :refer [oget]]
    [district-hegex.ui.trading.events :as trading-events]
   [district-hegex.ui.weth.events :as weth-events]
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
                         :format (fn [v]
                                   (case (some-> v bn/number) 1 "WBTC" 0 "ETH" "ETH"))
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
                         :format (fn [v] (str v " WETH"))
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
                        #_{:path   [:sra-order :metaData :createdAt]
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

(defn p&l [[premium strike amount asset] option-type]
  (let [current-price (or @(subscribe [(case (some-> asset bn/number)
                                      0 ::external-subs/eth-price
                                      1 ::external-subs/btc-price
                                      ::external-subs/eth-price)])
                          0)
        amount-n (some-> amount web3-utils/wei->eth-number)
        premium-n (some->> premium
                         web3-utils/wei->eth-number
                         (* current-price))
        strike-n (some->> strike bn/number (* 0.00000001))
        pl (case option-type
             :call (- current-price (+ premium-n strike-n))
             :put (- strike-n (+ premium-n current-price))
             0)
        pl-total (* amount-n pl)
        pl-round (some-> pl-total
                         (format/format-number {:max-fraction-digits 2}))
        pl-small? (= 0 (some-> pl-round js/Math.abs))]
    [:div (str "$" (if-not pl-small? pl-round 0))]))

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
     :p&l  [p&l data (:option-type row)]
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
   :arrows          :one-row
   :column-model    columns
   :row-key         row-key-fn
   :render-cell     cell-fn
   :sort            sort-fn})

(defn- convert-weth []
  (let [form-data (r/atom {:weth/type :wrap})]
    (fn []
    (let [form-res (case (some-> @form-data :weth/type keyword)
                    :wrap {:btn "To WETH"
                           :evt ::weth-events/wrap}
                    :unwrap {:btn "To ETH"
                             :evt ::weth-events/unwrap}
                    {:btn "To WETH"
                     :evt ::weth-events/wrap})
          wrap-tx-pending? (subscribe [::tx-id-subs/tx-pending? :wrap-eth])
          unwrap-tx-pending? (subscribe [::tx-id-subs/tx-pending? :unwrap-eth])
          any-tx-pending? (or @wrap-tx-pending? @unwrap-tx-pending?)]
      [:div.box-grid-weth
      [:div.box.e
       [:div
        [:div {:style {:display "flex"
                       :justify-content "space-between"}}
          [inputs/select
           {:size :small
            :color :yellow
            :on-change (fn [e]
                         (js/e.persist)
                         ((debounce #(swap! form-data
                                            assoc
                                            :weth/type
                                            (oget e ".?target.?value"))
                                    500)))}
           [:option {:value :wrap
                     :selected true}
            "Wrap"]
           [:option {:value :unwrap}
            "Unwrap"]]
          [inputs/text-input
           {:size :small
            :type :number
            :color "yellow"
            :min 0
            :placeholder 0
            :on-change  (fn [e]
                          (js/e.persist)
                          ((debounce #(swap! form-data assoc
                                             :weth/amount
                                             (oget e ".?target.?value"))
                                     500)))}]]]]
       [:div.box.e
        [:button.yellow.line-btn
         {:disabled any-tx-pending?
          :on-click #(dispatch [(:evt form-res) @form-data])}
         (:btn form-res)
         (when any-tx-pending?
           [inputs/loader {:color :black :on? any-tx-pending?}])]]]))))

(defn- approve-weth-exchange [tx-pending?]
  [:button.yellow
   {:on-click #(dispatch [::weth-events/approve-exchange])
    :disabled @tx-pending?}
   "Approve WETH" (when @tx-pending? [inputs/loader {:color :black :on? @tx-pending?}])])

(defn- approve-weth-staking [tx-pending?]
  [:button.yellow
   {:on-click #(dispatch [::weth-events/approve-staking])
    :disabled @tx-pending?}
   "Approve WETH Staking" (when @tx-pending? [inputs/loader {:color :black :on? @tx-pending?}])])

(defn- buy-nft-button [active-option weth-bal]
  (let [active? (-> active-option :option)
        tx-pending? (subscribe [::external-subs/external-tx-pending? :fill-order])
        enough-weth? (< (-> active-option :option :eth-price) weth-bal)]
    [:div.hover-captioned
     [:button.yellow.line-btn
      {:className (when-not active? "disabled")
       :on-click #(dispatch [::trading-events/fill-offer (:option active-option)])
       :disabled  (or @tx-pending?
                      (not active?)
                      (not enough-weth?))}
      "Buy"
      (when @tx-pending? [inputs/loader {:color :black :on? @tx-pending?}])]
     (when-not enough-weth? [:div.hover-caption "Insufficient WETH balance"])]))

(defn- cancel-nft-button [active-option]
  (let [active? (-> active-option :option)
        tx-pending? (subscribe [::external-subs/external-tx-pending? :cancel-order])]
    (println "cancel tx pending?" @tx-pending?)
    [:div.hover-captioned.padded
     [:button.yellow.line-btn
      {:className (when-not active? "disabled")
       :on-click #(dispatch [::trading-events/cancel-offer (:option active-option)])
       :disabled  (or @tx-pending?
                      (not active?))}
      "Cancel"
      (when @tx-pending? [inputs/loader {:color :black :on? @tx-pending?}])]]))

(defn- buy-nft [active-option]
  (let [weth-approved? @(subscribe [::weth-subs/exchange-approved?])
        staking-approved? @(subscribe [::weth-subs/staking-approved?])
        active-account @(subscribe [::account-subs/active-account])
        approving-staking? (subscribe [::tx-id-subs/tx-pending? :approve-weth-staking])
        approving-exchange? (subscribe [::tx-id-subs/tx-pending? :approve-weth-exchange])
        my-offer? (= (some-> active-account cs/lower-case)
                     (some-> active-option :option :sra-order :order :makerAddress
                      str
                      cs/lower-case))
        weth-bal @(subscribe [::weth-subs/balance])]
    (cond
        my-offer? [cancel-nft-button active-option]
        (not weth-approved?) [approve-weth-exchange approving-exchange?]
        (not staking-approved?) [approve-weth-staking approving-staking?]
        :else [buy-nft-button active-option weth-bal])))

(defn controls []
  (let [active-option @(subscribe [::home-subs/my-orderbook-option])
        weth-bal @(subscribe [::weth-subs/balance])
        eth-bal (some-> (subscribe
                          [::account-balances-subs/active-account-balance :ETH])
                         deref
                         web3-utils/wei->eth-number
                         (format/format-number {:max-fraction-digits 5}))]
    [:div
     [:div {:style {:margin-left "auto"}}
      [:div#buy-order
       [:div.box.e
        [:div {:style {:margin-right "20px"}}
         [inputs/text-input
          {:type :number
           :color "yellow"
           :disabled true
           :min 0
           :placeholder (str (-> active-option :option (:eth-price 0)) " WETH")}]]]
       [:div.box.e
        [buy-nft active-option]]]]

     [:hr]
     [:div {:style {:max-width "500px"
                    :margin-left "auto"
                    :margin-right "auto"}}
      [:div.weth-wrapper
       [:br]
       [:br]
       [:div
        [:h3  [:b.hyellow " WETH "] "Station"]
        [:span.xs {:style {:opacity "0.6"}} "You need some "  " WETH " " to buy Hegex NFTs"]]

        [:br]
       [:div
        [:div
         [:span.caption "Current Balances"]
         [:br]
         [:span.caption
          [:b eth-bal] " ETH" " |  "]
         [:span.caption
          [:b weth-bal] " WETH"]]]]
      [convert-weth]]]))
