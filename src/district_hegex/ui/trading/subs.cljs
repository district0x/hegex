(ns district-hegex.ui.trading.subs
(:require
    [district-hegex.ui.config :as config]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [district-hegex.ui.trading.events :as trading-events]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))


(re-frame/reg-sub
  ::approved-for-exchange?
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :approved-for-exchange?])))

(re-frame/reg-sub
  ::hegic-book
  (fn [db _]
    (vals (get-in db [::hegex-nft/hegic-options :orderbook :full]))))

(re-frame/reg-sub
  ::hegic-ui-errors
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :ui-errors])))

(re-frame/reg-sub
  ::my-pending-offer?
  (fn [db _]
    (get-in db [::trading-events/my-pending-offer?])))

(re-frame/reg-sub
  ::hegic-pool-liq-eth
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :new :max-liq :eth])))

(re-frame/reg-sub
  ::hegic-pool-liq-btc
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :new :max-liq :btc])))
