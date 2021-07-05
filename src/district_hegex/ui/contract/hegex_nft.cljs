(ns district-hegex.ui.contract.hegex-nft
  (:require
   [bignumber.core :as bn]
   [goog.string :as gstring]
   [district-hegex.ui.external.events :as external-events]
   [district-hegex.ui.weth.events :as weth-events]
   [cljs-time.format :as tf]
    [goog.string.format]
   [reagent.core :as r]
   #_[web3 :as gweb3js]
   #_  ["web3" :as web3new]
   [web3 :as web3webpack]
   [district.ui.smart-contracts.subs :as contracts-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [re-frame.core :refer [subscribe dispatch]]
   [cljs-web3-next.eth :as web3-ethn]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
#_   [web3 :as web3js]
   #_[react :refer [createElement]]
  #_ ["react-dom/server" :as ReactDOMServer :refer [renderToString]]
   [cljs-bean.core :refer [bean ->clj ->js]]
   #_ [cljs-web3.core :as web3]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       gget
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
    [district-hegex.shared.utils :refer [to-simple-time debounce]]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
    [district.format :as format]
    [district.parsers :as parsers]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district.ui.web3.queries :as web3-queries]
    [district.web3-utils :as web3-utils]
    [goog.string :as gstring]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [dispatch reg-event-fx]])
  (:require-macros [district-hegex.shared.macros :refer [get-environment]]))

(defn- hegic-eth-options
  ([asset]
   (case asset

     "0"
     (case (get-environment)
      "dev" :hegicethoptions
      "prod" :brokenethoptions
      "qa" :brokenethoptions
      :brokenethoptions)

     "1"
     :wbtcoptions

     :hegicethoptions))
  ([] (hegic-eth-options "0")))

(def interceptors [re-frame/trim-v])

(def ^:private erc721-0x-proxy
  (case (get-environment)
      "dev" "0xeFc70A1B18C432bdc64b596838B4D138f6bC6cad"
      "prod"  "0xeFc70A1B18C432bdc64b596838B4D138f6bC6cad"
      "qa" "0xe654aac058bfbf9f83fcaee7793311dd82f6ddb4"
      "0xeFc70A1B18C432bdc64b596838B4D138f6bC6cad"))


(defn- ui-options-model [db]
(println "erc721 proxy is" erc721-0x-proxy)
  (assoc-in db [::hegic-options :full-ui]
            (vals (get-in db [::hegic-options :full]))))

(re-frame/reg-event-fx
  ::owner
  interceptors
  (fn [{:keys [db]} _]
    (println "dbg calling" "owner is")
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :hegexoption)
             :fn :owner
             :on-success [::owner-success]
             :on-error [::logging/error [::owner]]}]}}))


(re-frame/reg-event-fx
  ::owner-success
  interceptors
  (fn [{:keys [db]} [owner]]
    (println "dbg" "owner is" owner)
    {:db (assoc-in db [::owner] owner)}))


(re-frame/reg-event-fx
  ::hegic-options
  interceptors
  (fn [{:keys [db]} [opt-ids]]
    {:dispatch-n (conj (mapv (fn [id] [::hegic-option id]) opt-ids)
                       [::my-hegex-options-count]
                       ;;here belongs approved-for-exchange? query
                       [::approved-for-exchange?]
                       [::external-events/fetch-asset-prices]
                       [::weth-events/weth-balance]
                       [::weth-events/exchange-approved?]
                       [::weth-events/staking-approved?])
     :db (assoc-in db [::hegic-options :my :ids] opt-ids)}))

(def deb-owner
  (debounce
   (fn []
     (dispatch [::owner]))
    500))

(defn- ->topic-pad [w3 s]
  ;; (println "->topic-pad" (oget web3js ".?version"))
  (ocall w3 ".?utils.?padLeft" s 64))

(defn- ->from-topic-pad [w3 s]
  ;; (println "->topic-pad" (oget web3js ".?version"))
  (ocall w3 ".?utils.?hexToNumber" s))

;; NOTE topic can remain hardcoded, static for all nets
(def ^:private creation-topic
  "0x9acccf962da4ed9c3db3a1beedb70b0d4c3f6a69c170baca7198a74548b5ef4e")

;; sample address 0xB95Fe51930dDFC546Ff766d59288b50170244B4A
(defn my-hegic-options
  "using up-to-date instance of web3 out of npm [ROPSTEN]"
  [web3-host addr db]
  (let [Web3 (gget ".?Web3x")
        by-chef (contract-queries/contract-address db :optionchef)
        web3js (Web3. (gget ".?web3.?currentProvider"))]
    (println "mho"  (oget web3js ".?version"))
    (js-invoke (oget web3js ".?eth")
               "getPastLogs"
               (clj->js {:address  (contract-queries/contract-address db (hegic-eth-options))
                         :topics [creation-topic,
                                  nil,
                                  ;;NOTE now includes options minted directly +
                                  ;;options minted by optionchef contract (autowrapped ones)
                                  ;;NOTE by-chef will grow too much, switch to querying
                                  ;;optionchef instead
                                  [#_(->topic-pad web3js by-chef)
                                   (->topic-pad web3js addr)]]
                         :fromBlock 0
                         :toBlock "latest"})
               "then"
           (fn [errs evs]
             (let [ids-raw (map (fn [e] (-> e bean :topics second)) evs)]
               (println "ids-raw" ids-raw)
               #_(when (zero? (count ids-raw))
                 (dispatch [::hide-loader]))
               (dispatch [::hegic-options (map (partial ->from-topic-pad web3js)
                                               ids-raw)]))))))

(re-frame/reg-event-fx
  ::hide-loader
  interceptors
  (fn [{:keys [db]} ]
    {:db (assoc-in db [:initial-state-loaded?] true)}))

;; TODO - look into batching for this web3 fx
(re-frame/reg-event-fx
  ::hegic-option
  interceptors
  (fn [{:keys [db]} [id]]
    (println "dbg fetching full data for option id " id "..." )
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db (hegic-eth-options))
             :fn :options
             :args [id]
             :on-success [::hegic-option-success id]
             :on-error [::logging/error [::hegic-option]]}]}}))

(defn- from-decimals-fn [asset]
  (case asset
    1 #(/ % (js/Math.pow 10 8))
    0 web3-utils/wei->eth-number
    web3-utils/wei->eth-number))

(defn- ->hegic-info [[state holder strike amount
                      locked-amount premium expiration
                      option-type asset] id]
  (println "assetbtc" )
  (let [amount-hr (some->> amount bn/number)
        from-decimals (from-decimals-fn (bn/number asset))]
    {:state         (bn/number state)
    ;;data redundancy for ease of access by views
    :hegic-id      id
    :holder        holder
    :strike        (some->> strike
                            bn/number
                            (*  0.00000001)
                            (gstring/format "%.2f"))
     :amount        (some->> amount-hr
                            from-decimals
                            (gstring/format "%.4f" ))
    :locked-amount (bn/number locked-amount)
    :premium       (some->> premium
                            from-decimals
                            (gstring/format "%.6f"))
    :expiration    (to-simple-time expiration)
    :expiration-stamp (some-> expiration bn/number)
    :asset         asset
    ;;NOTE a bit cryptic model, P&L is fetched later via (price+-strike(+-premium*price))
    ;;NOTE P&L with premium is inaccurate since we _can't_ fetch historical price for premium
     :p&l           [premium strike amount-hr asset]
    :option-type   (case (bn/number option-type)
                     1 :put
                     2 :call
                     :invalid)}))

(re-frame/reg-event-fx
  ::hegic-option-success
  interceptors
  (fn [{:keys [db]} [id hegic-info-raw]]
    ;; NOTE move formatting to view, store raw data in re-frame db
    (let [upd-db (assoc-in db [::hegic-options :full id]
                        (->hegic-info hegic-info-raw id))]
      {:db (ui-options-model upd-db)})))

(re-frame/reg-event-fx
  ::wrap!
  interceptors
  (fn [{:keys [db]} [id]]
(println "dbg wrapping option with id.." id)
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :optionchef)
                 :fn :wrapHegic
                 :args [id]
                 :tx-opts {:from (account-queries/active-account db)}
                 ;; :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                 :tx-id {:wrap {:hegic id}}
                 :on-tx-success [::wrap-success]
                 :on-tx-error [::logging/error [::wrap!]]}]}))

(re-frame/reg-event-fx
  ::wrap-success
  ;;actually assoc result (hegex-id) to db to update the UI
  (fn [data]
    (println "dbg wrapped option ::successfully")
    {:dispatch [:district-hegex.ui.events/load-my-hegic-options {:once? true}]}))

(re-frame/reg-event-fx
  ::unwrap!
  interceptors
  (fn [{:keys [db]} [id]]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :optionchef)
                 :fn :unwrapHegic
                 :args [id]
                 :tx-opts {:from (account-queries/active-account db)}
                 :tx-id {:unwrap {:hegic id}}
                 :on-tx-success [::unwrap-success]
                 :on-tx-error [::logging/error [::unwrap!]]}]}))

(re-frame/reg-event-fx
  ::unwrap-success
  (fn [data]
    (println "dbg unwrapped option" data)
    {:dispatch [::clean-hegic]}))

(re-frame/reg-event-fx
  ::my-hegex-options-count
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :hegexoption)
             :fn :balanceOf
             :args [(account-queries/active-account db)]
             :on-success [::my-hegex-options]
             :on-error [::logging/error [::my-hegex-options-count]]}]}}))


(re-frame/reg-event-fx
  ::my-hegex-options
  interceptors
  (fn [_ [hg-count]]
    (when hg-count
      {:dispatch-n (mapv (fn [id] [::my-hegex-option id]) (range (bn/number hg-count)))})))


(re-frame/reg-event-fx
  ::approved-for-exchange?
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :hegexoption)
             :fn :isApprovedForAll
             :args [(account-queries/active-account db) erc721-0x-proxy]
             :on-success [::approved-for-exchange-success]
             :on-error [::logging/error [::approved-for-exchange?]]}]}}))


(re-frame/reg-event-fx
  ::approved-for-exchange-success
  interceptors
  (fn [{:keys [db]} [approved?]]
    (println "dbgexchange evt" approved?)
    {
     :db (assoc-in db [::hegic-options :approved-for-exchange?] approved?)}))


(re-frame/reg-event-fx
  ::my-hegex-option
  interceptors
  (fn [{:keys [db]} [hg-id]]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :hegexoption)
             :fn :tokenOfOwnerByIndex
             :args [(account-queries/active-account db) hg-id]
             :on-success [::my-hegex-option-success]
             :on-error [::logging/error [::my-hegex-option]]}]}}))

(re-frame/reg-event-fx
  ::my-hegex-option-success
  interceptors
  (fn [{:keys [db]} [id-raw]]
    ;;NOTE recheck the logic behind unwrapping to avoid false positives
    (println "dbg found nft " (bn/number id-raw) (get-in db [::hegic-options :full]))
    (when-let [id (bn/number id-raw)]
      {:dispatch [::my-uhegex-option id]
       :db (assoc-in db [::hegic-options :my-nfts  id :delegated] false)})))

;;uHegex-*u*nderlying Hegex option (in this case hegic)
(re-frame/reg-event-fx
  ::my-uhegex-option
  interceptors
  (fn [{:keys [db]} [hg-id]]
    (println "dbghgdata0" hg-id)
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :optionchef)
             :fn :getUnderlyingOptionId
             :args [hg-id]
             :on-success [::my-uhegex-option-type hg-id]
             :on-error [::logging/error [::my-uhegex-option]]}]}}))

(re-frame/reg-event-fx
  ::my-uhegex-option-type
  interceptors
  (fn [{:keys [db]} [hg-id uid-raw]]
    (when-let [uid (bn/number uid-raw)]
    (println "dbghgdata1" uid)
      {:web3/call
      {:web3 (web3-queries/web3 db)
       :fns [{:instance (contract-queries/instance db :optionchefdata)
              :fn :optionType
              :args [uid]
              :on-success [::my-uhegex-option-full hg-id uid-raw]
              :on-error [::logging/error [::my-uhegex-option-type]]}]}})))

(re-frame/reg-event-fx
  ::my-uhegex-option-full
  interceptors
  (fn [{:keys [db]} [hg-id uid-raw option-type-raw]]
    (println "uhegex0" (bn/number uid-raw) (bn/number option-type-raw))
    (when-let [uid (bn/number uid-raw)]
      (let [with-uid (assoc-in db [::hegic-options :full uid :hegex-id] hg-id)
            with-option-type (assoc-in with-uid [::hegic-options :full uid :asset]
                                       (bn/number option-type-raw))
            with-ui (ui-options-model with-option-type)]
        (println "uhegex1 with option-type is" with-option-type)
        (cond->  {:db with-ui}
         ;;query full when full hegic option is not in db (e.g. created by chef)
         (not (get-in db [::hegic-options :full uid :holder]))
         (assoc :web3/call
                {:web3 (web3-queries/web3 db)
                 :fns [{:instance (contract-queries/instance db :optionchef)
                        :fn :getUnderlyingOptionParams
                        :args [(bn/number option-type-raw) hg-id]
                        :on-success [::my-uhegex-option-full-success hg-id uid (bn/number option-type-raw)]
                        :on-error [::logging/error [::my-uhegex-option-full]]}]}))))))

(re-frame/reg-event-fx
  ::my-uhegex-option-full-success
  interceptors
  (fn [{:keys [db]} [hg-id uid option-type hegic-info-raw]]
    (println "dbgbtctype____________" ::my-uhegex-option-full-success hg-id uid
             hegic-info-raw)
    {:db (ui-options-model
          (update-in db [::hegic-options :full uid] merge
                     (->hegic-info  (conj hegic-info-raw option-type) uid)))}))


(re-frame/reg-event-fx
  ::delegate!
  interceptors
  (fn [{:keys [db]} [uid]]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db (hegic-eth-options))
                 :fn :transfer
                 :args [uid (contract-queries/contract-address db :optionchef)]
                 :tx-opts {:from (account-queries/active-account db)}
                 ;; :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                 :tx-id {:delegate {:hegic uid}}
                 :on-tx-success [::delegate-success uid]
                 :on-tx-error [::logging/error [::delegate!]]}]}))

(re-frame/reg-event-fx
  ::delegate-success
  interceptors
  (fn [{:keys [db]} [uid]]
    (println "dbg delegated option" uid " ::successfully")
    {:db (ui-options-model
          (assoc-in db [::hegic-options :full uid :holder]
                    (contract-queries/contract-address db :optionchef)))}))

(re-frame/reg-event-fx
  ::estimate-mint-hegex
  interceptors
  (fn [{:keys [db]} [{:keys [:new-hegex/period
                            :new-hegex/amount
                            :new-hegex/strike-price
                            :new-hegex/hegic-type
                            :new-hegex/option-type]}]]
    (println "dbgx hegic type is" hegic-type)
    (let [opt-dir (case (keyword option-type)
                    :put 1
                    :call 2
                    2)
          period-secs (some-> period (* 86400))
          strike-wei (some-> strike-price (* 100000000))
          option-args [period-secs amount strike-wei opt-dir]]
      (println "dbgest instance is" (contract-queries/instance db (hegic-eth-options hegic-type)))
      (println "dbgest 0")
      {:web3/call
       {:web3 (web3-queries/web3 db)
        :fns [{:instance (contract-queries/instance db (hegic-eth-options hegic-type))
               :fn :fees
               :args option-args
               ;; :tx-opts {:chainId "1337"}
               :on-success [::estimate-mint-hegex-success option-args]
               :on-error [::logging/error [::estimate-mint-hegex]]}]}})))

(defn- hegic-errors [[total settlement-fee strike-fee] [period-secs amount]]
  (println "dbg strike fee" (some-> strike-fee bn/number))
  ;; revert logic from HegicETHOptions.sol/create
  (cond-> []
    (or (not period-secs) (< period-secs 86400)) (conj :period-too-short)
    (not (zero? (mod period-secs 86400))) (conj :period-invalid)
    (> period-secs (* 4 604800)) (conj :period-too-long)
    ;; this is where too-small-a-position err comes from
    (<= amount strike-fee) (conj :price-diff-too-large)))

(re-frame/reg-event-fx
  ::estimate-mint-hegex-success
  interceptors
  ;;NOTE first is total
  (fn [{:keys [db]} [option-params fees]]
    (let [errors (hegic-errors fees option-params)
          with-errors (assoc-in db [::hegic-options :new :errors] errors)]
      (println "dbgest1" )
      (println "dbgfees args" fees option-params)
      (println "dbgfees" (some-> fees first bn/number) errors)
      {:db (assoc-in with-errors
                     [::hegic-options :new :total-cost]
                     (some-> fees first bn/number))})))

(re-frame/reg-event-fx
  ::mint-hegex
  interceptors
  (fn [{:keys [db]} [{:keys [:new-hegex/period
                            :new-hegex/amount
                            :new-hegex/hegic-type
                            :new-hegex/strike-price
                            :new-hegex/option-type]
                     :as form-data}]]
    (println "hegic type during minting is" hegic-type (hegic-eth-options hegic-type))
    (let [opt-dir (case (keyword option-type)
                    :put 1
                    :call 2
                    2)
          period-secs (some-> period (* 86400))
          amount-eth (some-> amount (* 1))
          strike-wei (some-> strike-price (* 100000000))
          option-args [period-secs amount-eth strike-wei opt-dir]]
      (println "dbgmint0" option-args (hegic-eth-options hegic-type))
      (js/console.log "dbgmint01" (contract-queries/instance db (hegic-eth-options hegic-type)))
      {:web3/call
       {:web3 (web3-queries/web3 db)
        :fns [{:instance (contract-queries/instance db (hegic-eth-options hegic-type))
               :fn :fees
               :args option-args
               :on-success [::mint-hegex! (vec (cons hegic-type option-args))]
               :on-error [::logging/error [::mint-hegex]]}]}})))


(re-frame/reg-event-fx
  ::mint-hegex!
  interceptors
  (fn [{:keys [db]} [opt-args fees]]
    (let [extract-fee (if (= "1" (first opt-args)) second first)]
      (println "dbgmint1")
      (println "optargsmint" opt-args)
      {:dispatch [::tx-events/send-tx
                 {:instance (contract-queries/instance db :optionchef)
                  :fn :createHegic
                  :args opt-args
                  :tx-opts {:value (some->> fees extract-fee bn/number)
                            :from (account-queries/active-account db)}
                  :tx-id :mint-hegex!
                  :on-tx-success [::mint-hegex-success]
                  :on-tx-error [::logging/error [::mint-hegex!]]}]})))

(re-frame/reg-event-fx
  ::mint-hegex-success
  interceptors
  (fn [{:keys [db]} _]
    {:dispatch-n [[:district-hegex.ui.trading.events/load-pool-eth]
                  [:district-hegex.ui.trading.events/load-pool-btc]
                  [:district-hegex.ui.events/load-my-hegic-options {:once? true}]]}))



;;approve hegex for 0x erc721 proxy contract to submit orders
(re-frame/reg-event-fx
  ::approve-for-exchange!
  interceptors
  (fn [{:keys [db]} _]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :hegexoption)
                 :fn :setApprovalForAll
                 :args [erc721-0x-proxy true]
                 :tx-opts {:from (account-queries/active-account db)}
                 :tx-id :approve-for-exchange!
                 :on-tx-success [::approve-for-exchange-success]
                 :on-tx-error [::logging/error [::approve-for-exchange!]]}]}))

(re-frame/reg-event-fx
  ::approve-for-exchange-success
  interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [::hegic-options :approved-for-exchange?] true)}))

(re-frame/reg-event-fx
  ::clean-hegic
  interceptors
  (fn [_ _]
    {:dispatch [:district-hegex.ui.events/load-my-hegic-options {:once? true}]}))

;; get underlying info on traded hegex options,
;; unite with my-hegex-option fns
(re-frame/reg-event-fx
  ::uhegex-option
  interceptors
  (fn [{:keys [db]} [hg-id eth-price raw-price order]]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :optionchef)
             :fn :getUnderlyingOptionId
             :args [hg-id]
             :on-success [::uhegex-option-full hg-id eth-price raw-price order]
             :on-error [::logging/error [::uhegex-option]]}]}}))

(re-frame/reg-event-fx
  ::uhegex-option-full
  interceptors
  (fn [{:keys [db]} [hegex-id eth-price raw-price order hegic]]
    (when-let [uid (bn/number hegic)]
      {:db (update-in db [::hegic-options :orderbook :full hegex-id] merge
                      {:hegic-id uid})
       :web3/call
       {:web3 (web3-queries/web3 db)
        :fns [{:instance (contract-queries/instance db :optionchefdata)
               :fn :optionType
               :args [uid]
               :on-success [::uhegex-option-full-fetch hegex-id uid eth-price
                            raw-price order]
               :on-error [::logging/error [::my-uhegex-option-full]]}]}})))

(re-frame/reg-event-fx
  ::uhegex-option-full-fetch
  interceptors
  (fn [{:keys [db]} [hegex-id uid eth-price raw-price order hegic-type]]
    (println "dbguhegex -full db is" uid)
    {:db (update-in db [::hegic-options :orderbook :full hegex-id] merge
                    {:hegic-id uid})
     :web3/call {:web3 (web3-queries/web3 db)
                 :fns [{:instance (contract-queries/instance db :optionchef)
                        :fn :getUnderlyingOptionParams
                        :args [(bn/number hegic-type) hegex-id]
                        :on-success [::uhegex-option-full-success
                                     hegex-id uid eth-price
                                     raw-price order hegic-type]
                        :on-error [::logging/error [::uhegex-option-full-fetch]]}]}}))

(re-frame/reg-event-fx
  ::uhegex-option-full-success
  interceptors
  (fn [{:keys [db]} [hg-id uid eth-price raw-price order hegic-type hegic-info-raw]]
    (println "dbguhegex option full success" [hg-id uid eth-price raw-price order hegic-info-raw])
    {:db (update-in db [::hegic-options :orderbook :full hg-id] merge
                    (merge (->hegic-info (conj hegic-info-raw hegic-type) uid)
                           {:hegex-id hg-id
                            :eth-price eth-price
                            ;; raw info for filling an order
                            :taker-asset-amount raw-price
                            :sra-order order}))}))


(re-frame/reg-event-fx
  ::exercise!
  interceptors
  (fn [{:keys [db]} [option-type hegex-id]]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :optionchef)
                 :fn :exerciseHegic
                 :args [option-type hegex-id]
                 :tx-opts {:from (account-queries/active-account db)}
                 :tx-id :exercise-hegic
                 :on-tx-success [::exercise-success hegex-id]
                 :on-tx-error [::logging/error [::exercise!]]}]}))

(re-frame/reg-event-fx
  ::exercise-success
  interceptors
  (fn [{:keys [db]} [hegex-id data]]
    {:db (update-in db [:district-hegex.ui.contract.hegex-nft/hegic-options :full-ui]
                    (fn [opts]
                      (remove (fn [o] (= hegex-id (:hegex-id o))) opts)))}))
