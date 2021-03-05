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
  ::set-my-active-option
  interceptors
  (fn [db [option row-num]]
    (update-in db [:hegic-options/active-option] {:option option :row-num row-num})))
