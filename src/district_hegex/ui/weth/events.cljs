(ns district-hegex.ui.weth.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-fx]]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district.format :as format]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3.queries :as web3-queries]
   [web3 :as web3webpack]
    [district.web3-utils :as web3-utils]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       gget
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [cljs.core.async :refer [go]]
   [bignumber.core :as bn]
   [web3 :as web3+]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs-bean.core :refer [bean ->clj ->js]]
   [cljs-0x-connect.http-client :as http-client]))

(def interceptors [re-frame/trim-v])

;;NOTE
;;workaround for outdated ropsten contracts on 0x-api
;;backend
;;will be dynamic for mainnet
(def ^:private erc20-proxy "0xb1408f4c245a23c31b98d2c626777d4c0d766caa")


;;NOTE
;;workaround for outdated ropsten contracts on 0x-api
;;backend
;;will be dynamic for mainnet
(def ^:private staking-0x "0xfaabcee42ab6b9c649794ac6c133711071897ee9")

;;just a fun var for weth allowance
(def ^:private most-expensive-purchase 9999999999)

(re-frame/reg-event-fx
  ::weth-balance
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :weth)
             :fn :balanceOf
             :args [(account-queries/active-account db)]
             :on-success [::weth-balance-success]
             :on-error [::logging/error [::weth-balance]]}]}}))


(re-frame/reg-event-fx
  ::weth-balance-success
  interceptors
  (fn [{:keys [db]} [balance]]
    {:db (assoc-in db [:weth :balance]
                   (some-> balance
                           bn/number
                           web3-utils/wei->eth-number
                           (format/format-number {:max-fraction-digits 5})))}))


(re-frame/reg-event-fx
  ::exchange-approved?
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :weth)
             :fn :allowance
             :args [(account-queries/active-account db) erc20-proxy]
             :on-success [::exchange-approved-success]
             :on-error [::logging/error [::exchange-approved]]}]}}))


(re-frame/reg-event-fx
  ::exchange-approved-success
  interceptors
  (fn [{:keys [db]} [weis]]
    (println "weth approval" weis)
    (let [approved? (some-> weis
                           bn/number
                           web3-utils/wei->eth-number
                           (> most-expensive-purchase))]
      {:db (assoc-in db [:weth :exchange-approved?] approved?)})))

(re-frame/reg-event-fx
  ::staking-approved?
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :weth)
             :fn :allowance
             :args [(account-queries/active-account db) staking-0x]
             :on-success [::staking-approved-success]
             :on-error [::logging/error [::staking-approved]]}]}}))


(re-frame/reg-event-fx
  ::staking-approved-success
  interceptors
  (fn [{:keys [db]} [weis]]
    (let [approved? (some-> weis
                           bn/number
                           web3-utils/wei->eth-number
                           (> most-expensive-purchase))]
      {:db (assoc-in db [:weth :staking-approved?] approved?)})))

(re-frame/reg-event-fx
  ::wrap
  interceptors
  (fn [{:keys [db]} [form]]
    (let [weis (some-> (:weth/amount form) web3-utils/eth->wei-number)]
      {:dispatch [::tx-events/send-tx
                 {:instance (contract-queries/instance db :weth)
                  :fn :deposit
                  :args []
                  :tx-opts {:value weis
                            :from (account-queries/active-account db)}
                  :tx-id :wrap-eth
                  :on-tx-success [::wrap-success]
                  :on-tx-error [::logging/error [::wrap]]}]})))

(re-frame/reg-event-fx
  ::wrap-success
  interceptors
  (fn [_ _]
    {:dispatch [::weth-balance]}))


(re-frame/reg-event-fx
  ::unwrap
  interceptors
  (fn [{:keys [db]} [form]]
    (let [weis (some-> (:weth/amount form) web3-utils/eth->wei-number)]
      {:dispatch [::tx-events/send-tx
                 {:instance (contract-queries/instance db :weth)
                  :fn :withdraw
                  :args [weis]
                  :tx-opts {:from (account-queries/active-account db)}
                  :tx-id :unwrap-eth
                  :on-tx-success [::unwrap-success]
                  :on-tx-error [::logging/error [::unwrap]]}]})))

(re-frame/reg-event-fx
  ::unwrap-success
  interceptors
  (fn [_ _]
    {:dispatch [::weth-balance]}))


(re-frame/reg-event-fx
  ::approve-exchange
  interceptors
  (fn [{:keys [db]} _]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :weth)
                 :fn :approve
                 :args [erc20-proxy (web3-utils/eth->wei-number
                                     (+ most-expensive-purchase 1))]
                 :tx-opts {:from (account-queries/active-account db)}
                 :tx-id :approve-weth-exchange
                 :on-tx-success [::approve-exchange-success]
                 :on-tx-error [::logging/error [::approve-exchange]]}]}))

(re-frame/reg-event-fx
  ::approve-exchange-success
  interceptors
  (fn [_ _]
    {:dispatch [::exchange-approved?]}))

(re-frame/reg-event-fx
  ::approve-staking
  interceptors
  (fn [{:keys [db]} _]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :weth)
                 :fn :approve
                 :args [staking-0x (web3-utils/eth->wei-number
                                     (+ most-expensive-purchase 1))]
                 :tx-opts {:from (account-queries/active-account db)}
                 :tx-id :approve-weth-staking
                 :on-tx-success [::approve-staking-success]
                 :on-tx-error [::logging/error [::approve-staking]]}]}))

(re-frame/reg-event-fx
  ::approve-staking-success
  interceptors
  (fn [_ _]
    {:dispatch [::staking-approved?]}))
