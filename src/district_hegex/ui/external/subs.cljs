(ns district-hegex.ui.external.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::eth-price
 (fn [db _]
   (println "pricedbg is" (get-in db [:prices :ethereum]))
    (get-in db [:prices :ethereum])))

(re-frame/reg-sub
 ::btc-price
  (fn [db _]
    (get-in db [:prices :bitcoin])))

(re-frame/reg-sub
 ::external-tx-pending?
  (fn [db [_ id]]
    ;; explicit hegex-id to bool
    (some? (get-in db [:pending-external-txs id]))))
