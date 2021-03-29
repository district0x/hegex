(ns district-hegex.ui.trading.subs
(:require
    [district-hegex.ui.config :as config]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
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
