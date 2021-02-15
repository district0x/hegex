(ns district-hegex.ui.subs
  (:require
    [district-hegex.ui.config :as config]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::vote
  (fn [db [_ reg-entry-address]]
    (get-in db [:district-hegex.ui.core/votes (account-queries/active-account db) reg-entry-address])))

(re-frame/reg-sub
  ::aragon-id-available?
  (fn [db [_ aragon-id]]
    (get-in db [:district-hegex.ui.core/aragon-id->available? aragon-id])))

(re-frame/reg-sub
  ::aragon-url
  (fn [_ [_ aragon-id]]
    (str (format/ensure-trailing-slash (:aragon-url config/config-map)) aragon-id)))

(re-frame/reg-sub
  ::active-account-has-email?
  (fn [db]
    (boolean (seq (get-in db [:district-hegex.ui.my-account (account-queries/active-account db) :encrypted-email])))))

(re-frame/reg-sub
  ::estimated-return-for-stake
  (fn [db [_ stake-bank amount]]
    (get-in db [:district-hegex.ui.contract.district/estimated-return-for-stake stake-bank amount])))

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
