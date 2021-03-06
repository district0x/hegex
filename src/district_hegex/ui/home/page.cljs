(ns district-hegex.ui.home.page
 (:import [goog.async Debouncer])
  (:require
   [clojure.string :as cs]
   [district-hegex.ui.home.events :as home-events]
   [district-hegex.ui.home.subs :as home-subs]
   [district-hegex.ui.components.inputs :as inputs]
   [district.ui.web3-account-balances.subs :as account-balances-subs]
   [district-hegex.ui.external.subs :as external-subs]
   [district-hegex.ui.weth.subs :as weth-subs]
   [district-hegex.ui.weth.events :as weth-events]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [cljs-bean.core :refer [bean ->clj ->js]]
   [district-hegex.ui.components.components :as c]
   [district-hegex.ui.trading.subs :as trading-subs]
   [district.ui.component.tx-button :refer [tx-button]]
   [district.web3-utils :as web3-utils]
   [reagent.ratom :as ratom]
   [district-hegex.ui.spec :as spec]
   [oops.core :refer [oget]]
    [district.ui.smart-contracts.subs :as contracts-subs]
    [district-hegex.ui.trading.events :as trading-events]
    [district-hegex.ui.home.table :as dt]
    [cljs-web3.core :as web3]
    [cljs-web3-next.eth :as web3-eth]
    [district.ui.web3.subs :as web3-subs]
    [district-hegex.ui.components.app-layout :refer [app-layout]]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [district.format :as format]
    [district.ui.component.page :refer [page]]
    [district-hegex.ui.subs :as subs]
    [district.ui.ipfs.subs :as ipfs-subs]
    [district.ui.now.subs :as now-subs]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn district-image []
  (let [ipfs (subscribe [::ipfs-subs/ipfs])]
    (fn [image-hash]
      (when image-hash
        (when-let [url (:gateway @ipfs)]
          [:img.district-image {:src (str (format/ensure-trailing-slash url) image-hash)}])))))

(defn loader []
  (let [mounted? (r/atom false)]
    (fn []
      (when-not @mounted?
        (js/setTimeout #(swap! mounted? not)))
      [:div#loader-wrapper {:class (str "fade-in" (when @mounted? " visible"))}
       [:div#loader
        [:div.loader-graphic
         ;; [:img.blob.spacer {:src "/images/svg/loader-blob.svg"}]
         [:div.loader-floater
          [:img.bg.spacer {:src "/images/svg/loader-bg.svg"}]
          [:div.turbine
           [:img.base {:src "/images/svg/turbine-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/turbine-blade.svg"}]]
           [:img.cover {:src "/images/svg/turbine-cover.svg"}]]
          [:div.fan
           {:data-num "1"}
           [:img.base {:src "/images/svg/fan-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/fan-spokes.svg"}]]]
          [:div.fan
           {:data-num "2"}
           [:img.base {:src "/images/svg/fan-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/fan-spokes.svg"}]]]]]]])))


(def ^:private table-state (r/atom {:draggable false}))


(def ^:private columns [{:path   [:option-type]
                         :header "Option Type"
                         :attrs  (fn [data] {:style {:text-align     "left"
                                                    :text-transform "capitalize"}})
                         :key    :option-type}
                        {:path   [:asset]
                         :header "Currency"
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
                        {:path   [:premium]
                         :header "Total Cost"
                         :attrs  (fn [data] {:style {:text-align "left"}})
                         :key    :premium}
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
                        #_{:path   [:actions]
                         :header "Actions"
                         :attrs  (fn [data] {:style {:text-align     "left"
                                                    :text-transform "uppercase"}})
                         :key    :actions}
                        {:path   [:hegex-id]
                         :header "NFT"
                         :format (fn [v] (if (pos? (count (str v))) (str "Yes, #" v) "No"))
                         :attrs  (fn [data] {:style {:text-align     "left"}})
                         :key    :hegex-id}])


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

(defn- wrap-hegic [id]
  [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(dispatch [::hegex-nft/wrap! id])}
   "Wrap"])

(defn- exercise-badge [hegex-id]
  [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(dispatch [::hegex-nft/exercise! hegex-id])}
   "Exercise"])

(defn- sell-hegex [open? id]
  (println "open? in sell-hegex is" open?)
  [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(reset! open? true)}
   "Open"])

(defn- unwrap-hegex [open? id]
  (println "open? in sell-hegex is" open?)
  [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(dispatch [::hegex-nft/unwrap! id])}
   "Unwrap"])

(defn- buy-hegex-offer [order]
  [:span
   {:outlined true
    :small true
    :style {:margin-top "17px"
            :margin-bottom "0px"}
    :intent :primary
    :on-click #(dispatch [::trading-events/fill-offer order])}
   "Buy"])

(defn- cancel-hegex-offer [order]
  [:span
   {:outlined true
    :small true
    :style {:margin-top "17px"
            :margin-bottom "0px"}
    :intent :primary
    :on-click #(dispatch [::trading-events/cancel-offer order])}
   "Cancel"])

(defn- approve-weth-exchange []
  [:span
   {:outlined true
    :small true
    :style {:margin-top "17px"
            :margin-bottom "0px"}
    :intent :primary
    :on-click #(dispatch [::weth-events/approve-exchange])}
   "Approve WETH"])

(defn- approve-weth-staking []
  [:span
   {:outlined true
    :small true
    :style {:margin-top "17px"
            :margin-bottom "0px"}
    :intent :primary
    :on-click #(dispatch [::weth-events/approve-staking])}
   "Approve WETH Staking"])

(defn- nft-badge
  "WIP, should be a fun metadata pic"
  [id]
  [:span
   (str "NFT#" id)])

(defn- p&l [[paid strike amount] option-type]
  (let [current-price @(subscribe [::external-subs/eth-price])]
    ;;NOTE recheck P&L formula (esp. premium)
    [:div
     (if (= :call option-type)
       (- (* current-price amount) (* strike amount) paid)
       (- (* strike amount) (* current-price amount) paid))]))

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
      active-option @(subscribe [::home-subs/my-active-option])
      selected? (= row-num (:row-num active-option))]
  (println "selected?" selected? active-option)
  [:a
   (merge-with merge attrs  {:on-click (fn []
                                         (if selected?
                                           (dispatch
                                            [::home-events/set-my-active-option nil nil])

                                           (dispatch
                                            [::home-events/set-my-active-option
                                             row row-num])))
                             :style {:padding "10px"
                                     :outline "none"
                                     :cursor "pointer"
                                     :display "flex"
                                     :align-items "center"
                                     :background-color (if selected? "aqua" "inherit")
                                     :min-height "47px"
                                     :position "relative"}})
   #_(even? row-num) #_(assoc-in [:style :background-color] "#212c35")
   (case key
     :p&l [p&l data (:option-type row)]
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

(defn- unlock-hegex [uid]
  [:div
   [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(dispatch [::hegex-nft/delegate! uid])}
    "Unlock"]
   [:div.danger-space
    [:span.danger-caption "Delegate Hegic to enable trading"]]])

(defn- approve-exchange-hegex []
  [:div
   [:span
   {:outlined true
    :small true
    :intent :primary
    :on-click #(dispatch [::hegex-nft/approve-for-exchange!])}
    "Approve"]
   [:div.danger-space
    [:span.danger-caption "Approve Hegex to enable trading"]]])

(defn my-hegex-option [{:keys [id open? selling?]}]
  (let [chef-address  @(subscribe [::contracts-subs/contract-address :optionchef])
        approved? @(subscribe [::trading-subs/approved-for-exchange?])
        hegic @(subscribe [::subs/hegic-by-hegex id])
        unlocked? (= chef-address (:holder hegic))
        uid (:hegic-id hegic)]
    [:span
     {:elevation 4
      :interactive true
      :class-name "hegex-option"}
     [:div
      [:span
       {:style {:margin-bottom "5px"}
        :minimal true}
       "Hegex NFT#" id]

      ;; TODO query/visalize ITM/ATM/OTM status
      [:b.special.nft-caption " ITM"]
      [:br]
      [:div {:style {:text-align "left"}}
       [:div.price-caption.primary "Hegic "
        [:b {:style {:text-transform "uppercase"}}
         (:option-type hegic)]
        " Option"]
       [:span.nft-caption
        "Hegic ID: " (:hegic-id hegic)]
       [:br]
       [:span.nft-caption "Expiry: "
        (:expiration hegic)]
       [:br]
       [:span.nft-caption "Strike price: "
        (:strike hegic)]]
      [:br]
      (cond (not approved?)
            [approve-exchange-hegex]

            (not unlocked?)
            [unlock-hegex uid]

            (not selling?)
            [:div
             [sell-hegex open? id] " "
             [unwrap-hegex open? id]])]]))

(defn- maker-input []
  (let [form-data (r/atom {:expires 0
                           :total 0})]
    (fn [{:keys [id open?]}]
      (println "form -data is" @form-data)
      [:div.fchild
      [:div
       [:h4 "Sellling NFT#" id]
       [:br]
       [:h2 "For "
        [:span
         {:intent "primary"
          :on-change (fn [e]
                     ((debounce #(swap! form-data assoc
                                        :total
                                        e)
                                500)))
          :placeholder "0"}]]
       [:h4 "WETH"]
       [:br]
       [:h2 "Expires in "
        [:span
         {:intent "primary"
          :on-change (fn [e]
                     ((debounce #(swap! form-data assoc
                                        :expires
                                        e)
                                500)))
          :placeholder "0"}]]
       [:h4 "hours"]
       [:br]
       [:span
        {:outlined true
         :small true
         :on-click #(dispatch [::trading-events/create-offer (assoc @form-data :id id) open?])
         :intent :primary}
        "Place Offer"]]])))

(defn- my-hegex-option-wrapper []
  (let [open? (r/atom false)]
    (fn [{:keys [id]}]
      (println "open? " @open?)
      [:<>
       [my-hegex-option {:id id
                         :open? open?}]
       [:span
        {:on-close #(reset! open? false)
         :portal-class-name "bp3-dark"
         :is-open @open?}
        [:div.fwrap
         [:div.fchild [my-hegex-option {:id id
                                        :selling? true
                                        :open? open?}]]
         [maker-input {:open? open?
                       :id id}]]]])))

(defn orderbook-hegex-option [offer]
  (let [chef-address  @(subscribe [::contracts-subs/contract-address :optionchef])
        weth-approved? @(subscribe [::weth-subs/exchange-approved?])
        staking-approved? @(subscribe [::weth-subs/staking-approved?])
        active-account @(subscribe [::account-subs/active-account])
        my-offer? (= (some-> active-account cs/lower-case)
                     (some-> offer :sra-order :order :makerAddress cs/lower-case))
        hegic offer
        unlocked? (= chef-address (:holder hegic))
        uid (:hegic-id hegic)]
    (println "offer is" offer)
    (println "off keys are" (keys offer))
    [:span
         {:elevation 4
          :interactive true
          :class-name "hegex-option"}
     [:div
      [:span
       {:style {:margin-bottom "5px"}
        :minimal true}
        "Hegex NFT#" (:hegex-id offer)]
      ;; TODO query/visalize ITM/ATM/OTM status
      [:b.special.nft-caption " ITM"]
      [:br]
      [:div {:style {:text-align "left"}}
       [:div.price-caption.primary "Hegic "
        [:b {:style {:text-transform "uppercase"}}
         (:option-type hegic)]
        " Option"]
       [:span.nft-caption
        "Hegic ID: " (:hegic-id hegic)]
       [:br]
       [:span.nft-caption "Expiry: "
        (:expiration hegic)]
       [:br]
       [:span.nft-caption "Strike price: "
        (:strike hegic)]]
      (cond
        my-offer? [cancel-hegex-offer offer]
        (not weth-approved?) [approve-weth-exchange]
        (not staking-approved?) [approve-weth-staking]
        :else [buy-hegex-offer offer])
      [:br]
       [:span.price-caption.primary (:eth-price hegic) " WETH"]
      ]]))

(def ^:private table-props
  {:table-container {:style {:border-radius "5px"
                             :text-align "center"
                             :padding       "15px"}}
   :th              {:style {:color            "#aaa"
                             :border-bottom "1px solid green"
                             :font-size        "12px"
                             :text-align       "left"
                             :padding   "10px"}}
   :table-state     table-state
   :table {:style {:margin "auto"}}
   :column-model    columns
   :row-key         row-key-fn
   :render-cell     cell-fn
   :sort            sort-fn})

(defn- my-hegic-option-controls []
  (let [offer {:price 0}]
    (fn []
     (let [active-option (:option @(subscribe [::home-subs/my-active-option]))]
       [:div
        #_(str active-option)
        [:div.box-grid
         [:div.box.e
          [:button.primary
           {:className (when-not active-option "disabled")
            :disabled  (not active-option)
            :on-click #(dispatch [::hegex-nft/exercise! (:hegex-id active-option)])}
           "Exercise"]]
         [:div.box.d
          [inputs/select
           {:disabled true}
           [:option {:selected true
                     :value :eth}
            "ETH"]
           [:option {:value :wbtc}
            "WBTC"]]]
         [:div.box.e
          [inputs/text-input
           {:type :number
            :min 0
            :placeholder 0
            :on-change  (fn [e]
                          (js/e.persist)
                          (swap! offer assoc :price e))}]]
         [:div.box.e
          [:button.primary
           {:className (when-not active-option "disabled")
            :disabled  (not active-option)
            :on-click #(dispatch [::hegex-nft/exercise! (:hegex-id active-option)])}
           "Offer"]]]]))))

(defn- my-hegic-options []
  (let [opts (subscribe [::subs/hegic-full-options])]
    [:div
     [:div {:style {:display "flex"
                    :align-items "flex-start"
                    :justify-content "flex-start"}}
      [:h1 "My Option Contracts"]]
     [:div.container {:style {:font-size 16
                              :text-align "center"
                              :justify-content "center"
                              :align-items "center"}}
      (if-not (zero? (count @opts))
        [:div {:style {:margin-left "auto"
                      :margin-right "auto"
                      :overflow-x "auto"}}
         [dt/reagent-table opts table-props]]

        [:h5.dim-icon.gap-top
         "You don't own any Hegic options or Hegex NFTs. Mint one now!"])
      [my-hegic-option-controls]]]))

(defn- my-hegex-options []
  (let [ids (subscribe [::subs/my-hegex-ids])]
[:div
     [:div {:style {:display "flex"
                    :align-items "flex-start"
                    :justify-content "flex-start"}}
      [:h1 "My Option Contracts"]]
     [:div.container {:style {:font-size 16
                              :text-align "center"
                              :justify-content "center"
                              :align-items "center"}}
      (if-not (zero? (count @ids))
        [:div {:style {:margin-left "auto"
                      :margin-right "auto"
                      :overflow-x "auto"}}
     #_    [dt/reagent-table @ids table-props]]

        [:h5.dim-icon.gap-top
         "You don't own any Hegic options or Hegex NFTs. Mint one now!"])]]

    #_[:span
     {:elevation 5
      :class-name "my-nfts-bg"}
     [:br]
     [:div {:style {:display "flex"
                    :flex-direction "horizontal"
                    :align-items "center"
                    :justify-content "center"}}
      [c/i {:i "person"
            :size "13"
            :class "special"}]
      [:h3.dim-icon.special {:style {:display "flex"
                             :align-items "baseline"
                             :margin-left "10px"}} "My Hegex NFTs"]]

     [:br]
     [:div.container {:style {:font-size 16
                              :text-align "center"
                              :justify-content "center"
                              :align-items "center"}}
      (when (zero? (count @ids))
        [:h5.dim-icon "You don't own Hegex NFTs yet. Mint one now!"])
      [:div#hegex-wrapper
       [:div#hegex-container (doall (map (fn [id]
                      ^{:key id}
                      [my-hegex-option-wrapper {:id id}])
                    @ids))]
       [:div {:style {:clear "both"}}]]]]))

(defn- upd-new-hegex [form-data e key]
  ((debounce (fn []
               (dispatch [::hegex-nft/estimate-mint-hegex @form-data])
               (swap! form-data
                      assoc
                      key
                      (oget e ".?target.?value")))
             500)))


(defn- new-hegex []
  (let [form-data (r/atom {:new-hegex/currency :eth
                           :new-hegex/option-type :call})]
    (fn []
      (println "form-data inside is" @form-data)
      (let [current-price @(subscribe [::external-subs/eth-price])
            total-cost (or @(subscribe [::subs/new-hegic-cost]) 0)
            break-even (+ total-cost current-price)
            sp (some-> form-data deref :new-hegex/strike-price)]
        [:div
        [:div {:style {:display "flex"
                       :margin-top "30px"
                       :align-items "flex-start"
                       :justify-content "flex-start"}}
         [:h1 "Buy New Option Contract"]]
        [:div.box-grid
         [:div.box.a
          [:div.hover-label "Currency"]
          [inputs/select
           #_ {:on-change #(js/console.log (keyword (oget % ".?target.?value")))}
           [:option {:selected true
                     :value :eth}
            "ETH"]
           #_[:option {:selected true
                       :value :btc}
              "BTC"]]]
         [:div.box.d
          [:div.hover-label "Option type"]
          [inputs/select
           {:on-change (fn [e]
                         (js/e.persist)
                         (upd-new-hegex form-data e :new-hegex/option-type))}
           [:option {:selected true
                     :value :call}
            "Call"]
           [:option {:value :put}
            "Put"]]]
         [:div.box.f
          [:div.hover-label "Option size"]
          [inputs/text-input
           {:type :number
            :placeholder 0
            :label (some-> @form-data :new-hegex/currency name cs/upper-case)
            :on-change (fn [e]
                         (js/e.persist)
                         (upd-new-hegex form-data e :new-hegex/amount))
            :min 0}]]
         [:div.box.e
          [:div.hover-label "Strike price"]
          [inputs/text-input
           {:type :number
            :min 0
            :placeholder 0
            :on-change  (fn [e]
                          (js/e.persist)
                          (upd-new-hegex form-data e :new-hegex/strike-price))}]]
         [:div.box.b
          [:div.hover-label "Days of holding"]
          [inputs/text-input
           {:type :number
            :min 0
            :placeholder 0
            :on-change (fn [e]
                         (js/e.persist)
                         (upd-new-hegex form-data e :new-hegex/period))}]]]
        [:div.box-grid
         [:div.box.a
          [:div.hover-label "Strike price"]
          [:h3.stats "$" (if (pos? (count sp)) sp 0)]]
         [:div.box.d
          [:div.hover-label "Total cost"]
          [:h3.stats "$" total-cost]]
         [:div.box.f
          [:div.hover-label "Break-even"]
          [:h3.stats "$" break-even]]
         [:div.box.e
          [:button.primary
           {:on-click #(dispatch [::hegex-nft/mint-hegex @form-data])}
           "Buy"]]]
        [:div [:br] [:br] [:br]]]))))

(defn- convert-weth []
  (let [form-data (r/atom {:weth/type :wrap})]
    (fn []
    (let [form-res (case (some-> @form-data :weth/type keyword)
                    :wrap {:btn "Wrap"
                           :evt ::weth-events/wrap}
                    :unwrap {:btn "Unwrap"
                             :evt ::weth-events/unwrap}
                    {:btn "Wrap"
                     :evt ::weth-events/wrap})]
     (println "form-data is" @form-data)
     [:div {:style {:max-width "250px"
                    :margin-left "auto"
                    :margin-right "auto"
                    :text-align "center"}}
      [:br]
      [:br]
      [:div {:style {:max-width "250px"}}
       [:span
        {:vertical false}
        [:span
         {:on-change (fn [e]
                       (js/e.persist)
                       ((debounce #(swap! form-data
                                          assoc
                                          :weth/type
                                          (oget e ".?target.?value"))
                                  500)))}
         [:option {:value :wrap}
          "Wrap"]
         [:option {:value :unwrap}
          "Unwrap"]]
        [:span
         {:fill true
          :left-lable "WETH"
          :on-change  (fn [e]
                        (js/e.persist)
                        ((debounce #(swap! form-data assoc
                                           :weth/amount
                                           (oget e ".?target.?value"))
                                   500)))
          :placeholder "Amount"}]
        [:span
         {:outlined true
          :on-click #(dispatch [(:evt form-res) @form-data])}
         (:btn form-res)]]]]))))

(defn- orderbook []
  (let [weth-bal @(subscribe [::weth-subs/balance])
        book @(subscribe [::trading-subs/hegic-book])
        eth-bal (some-> (subscribe
                          [::account-balances-subs/active-account-balance :ETH])
                         deref
                         web3-utils/wei->eth-number
                         (format/format-number {:max-fraction-digits 5}))]
    [:span
     {:elevation 5
      :class-name "trade-nfts-bg"}
     [:br]
     [:div {:style {:display "flex"
                    :flex-direction "horizontal"
                    :align-items "center"
                    :justify-content "center"}}
      [c/i {:i "exchange"
            :size "25"
            :class "primary"}]
      [:h3.dim-icon.primary {:style {:display "flex"
                             :align-items "center"
                             :margin-left "10px"}} "Trade"
       [:h3.primary {:style {:margin-left "5px"}} "Hegex" ]
       [:span {:style {:margin-left "5px"}}"NFTs"]]]

     [:br]
     [:div {:style {:text-align "center"}}
      [:span
       {:outlined true
        :small true
        :on-click #(dispatch [::trading-events/load-orderbook])
        :intent :primary}
       "Force orderbook update"]]
     [:br]
     [:div {:style {:text-align "center"}}
      [:p "You need some WETH to buy Hegex NFTs"]
      [:span
       {:intent "primary"
        :minimal true}
       eth-bal " ETH"]
      " "
      [:span
       {:intent "success"
        :minimal true}
       weth-bal " WETH"]]
     [convert-weth]
     [:br]
     [:br]
     [:br]
     [:div.container {:style {:font-size 16
                              :text-align "center"
                              :justify-content "center"
                              :align-items "center"}}
      [:div#hegex-wrapper
       [:div#hegex-container (doall (map (fn [offer]
                      ^{:key (:hegex-id offer)}
                      [orderbook-hegex-option offer])
                    book))]
       [:div {:style {:clear "both"}}]]]]))

(defmethod page :route/home []
  [app-layout
   [:section#intro
    [:div.container
     [:span
      {:on-change constantly
       :large true
       :selected-tab-id "hegic"}
      [:span
       {:id "hegic"
        :large true
        :title "Hegic"}]]
     [:br]
     [my-hegic-options]
     [:hr]
     [new-hegex]
     [my-hegex-options]
     [orderbook]]]])
