(ns district-hegex.ui.home.events
  (:require
    [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-db
  ::toggle-dark-mode
  interceptors
  (fn [db _]
    (update-in db [::dark-mode?] not)))

(re-frame/reg-event-db
  ::toggle-open-about
  interceptors
  (fn [db _]
    (update-in db [::open-about?] not)))

(re-frame/reg-event-db
  ::set-my-active-option
  interceptors
  (fn [db [option row-num]]
    (assoc-in db [:hegic-options/active-option] {:option option :row-num row-num})))


(re-frame/reg-event-db
  ::set-orderbook-active-option
  interceptors
  (fn [db [option row-num]]
    (assoc-in db [:hegic-options/orderbook-option] {:option option :row-num row-num})))


(re-frame/reg-event-db
  ::set-my-option-sorting
  interceptors
  (fn [db [sorting]]
    (assoc-in db [:hegic-options/my-sorting] (some-> sorting first))))
