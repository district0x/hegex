(ns district-hegex.ui.external.events
  (:require
   [cljs.core.async :refer [go]]
   [district.ui.web3.queries :as web3-queries]
   [district.ui.logging.events :as logging]
   [district.ui.smart-contracts.queries :as contract-queries]
   [cljs.core.async.interop :refer-macros [<p!]]
   [oops.core :refer [gcall ocall oget]]
   [re-frame.core :as re-frame :refer [dispatch]])
  (:require-macros [district-hegex.shared.macros :refer [get-environment]]))

(def interceptors [re-frame/trim-v])

(def ^:private base-uri "https://api.coingecko.com/api/v3/coins/")

(def ^:private ropsten-prices {:bitcoin 11610
                               :ethereum 380})

(defn- price-by-env [db asset price]
  (case (get-environment)
    "prod" (assoc-in db [:prices asset] price)
    (assoc-in db [:prices asset] (get ropsten-prices asset))))

(re-frame/reg-event-fx
  ::fetch-asset-prices
  interceptors
  (fn [_ _]
    {:dispatch-n [::eth-price]}))

(re-frame/reg-event-fx
  ::eth-price
  interceptors
  (fn [{:keys [db]} _]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [{:instance (contract-queries/instance db :ethpriceprovider)
             :fn :latestAnswer
             :args []
             :on-success [::fetch-asset-prices-success :eth]
             :on-error [::logging/error [::eth-price]]}]}}))

;; NOTE legacy, remove

(re-frame/reg-event-db
  ::fetch-asset-prices-success
  interceptors
  (fn [db [asset price]]
    (println "dbgprice from provider" asset price)
#_    (price-by-env db asset price)))

(defn fetch-asset-prices [asset]
  (go
    (try
      (dispatch [::fetch-asset-prices-success
                 (keyword asset)
                 (oget (<p! (ocall (<p! (gcall "fetch"
                                           (str base-uri asset))) "json"))
            ".?market_data.?current_price.?usd")])
      (catch js/Error err (js/console.log (ex-cause err))))))

(re-frame/reg-fx
  ::fetch-asset-prices!
  (fn []
    (fetch-asset-prices "bitcoin")
    (fetch-asset-prices "ethereum")))


;; fetch for P&L
;; (await(await fetch("https://api.coingecko.com/api/v3/coins/ethereum")).json()).market_data.current_price.usd
