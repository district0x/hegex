(ns district-hegex.ui.external.events
  (:require
   [re-frame.core :as re-frame]))


(def interceptors [re-frame/trim-v])

(re-frame/reg-event-db
  ::fetch-asset-prices
  interceptors
  (fn [db _]
    (update-in db [::dark-mode?] not)))

;; fetch for P&L
;; (await(await fetch("https://api.coingecko.com/api/v3/coins/ethereum")).json()).market_data.current_price.usd
