(ns district-hegex.ui.weth.subs
(:require
    [district-hegex.ui.config :as config]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))


(re-frame/reg-sub
 ::balance
  (fn [db _]
    (get-in db [:weth :balance])))

(re-frame/reg-sub
 ::exchange-approved?
  (fn [db _]
    (get-in db [:weth :exchange-approved?])))


(re-frame/reg-sub
 ::staking-approved?
  (fn [db _]
    (get-in db [:weth :staking-approved?])))
