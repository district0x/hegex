(ns district-hegex.ui.subs
  (:require
    [district-hegex.ui.config :as config]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::hegex-nft-owner
  (fn [db _]
    (get-in db [::hegex-nft/owner])))

(re-frame/reg-sub
  ::hegic-options
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :my :ids])))

(re-frame/reg-sub
  ::hegic-full-options
  (fn [db _]
    (vals (get-in db [::hegex-nft/hegic-options :full]))))

(re-frame/reg-sub
  ::my-hegex-ids
  (fn [db _]
    (keys (get-in db [::hegex-nft/hegic-options :my-nfts]))))

(re-frame/reg-sub
  ::hegic-by-hegex
  (fn [db [_ h-id]]
    (first (filter
            (fn [h] (= (:hegex-id h) h-id))
            (vals (get-in db [::hegex-nft/hegic-options :full]))))))

(re-frame/reg-sub
  ::new-hegic-cost
  (fn [db _]
    (get-in db [::hegex-nft/hegic-options :new :total-cost])))
