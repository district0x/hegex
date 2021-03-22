(ns district-hegex.ui.external.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::eth-price
  (fn [db _]
    (get-in db [:prices :ethereum])))

(re-frame/reg-sub
 ::btc-price
  (fn [db _]
    (get-in db [:prices :bitcoin])))
