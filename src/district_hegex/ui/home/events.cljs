(ns district-hegex.ui.home.events
  (:require
    [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::toggle-dark-mode
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [db store]} _]
    (let [dark-mode? (not (get db ::dark-mode?))]
      {:db (assoc-in db [::dark-mode?] dark-mode?)
       :store (assoc-in store [:dark-mode?] dark-mode?)})))

(re-frame/reg-event-fx
  ::set-dark-mode
  interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [::dark-mode?] true)}))

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
