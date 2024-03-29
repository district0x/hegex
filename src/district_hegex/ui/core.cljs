(ns district-hegex.ui.core
  (:require
    [akiroz.re-frame.storage :as storage]
    [district-hegex.ui.trading.events :as trading-events]
    [district-hegex.ui.home.events :as home-events]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
    [cljs.spec.alpha :as s]
    [cljsjs.filesaverjs]
    [cljsjs.recharts]
    [day8.re-frame.async-flow-fx]
    [district0x.re-frame.interval-fx]
    [district-hegex.shared.routes :refer [routes]]
    [district-hegex.ui.config :as config]
    [district-hegex.ui.events :as events]
    [district-hegex.ui.home.page]
    [district.cljs-utils :as cljs-utils]
    [district.ui.component.router :refer [router]]
    [district.ui.notification]
    [district.ui.now]
    [district.ui.reagent-render]
    [district.ui.router.effects :as router-effects]
    [district.ui.router]
    [district.ui.smart-contracts.events :as contracts-events]
    [district.ui.smart-contracts]
    [district.ui.web3-account-balances]
    [district.ui.web3-accounts.events :as web3-accounts-events]
    [district.ui.web3-accounts]
    [district.ui.web3-balances]
    [district.ui.web3-tx-id]
    [district.ui.web3-tx-log]
    [district.ui.web3-tx]
    [district.ui.web3]
    [district.ui.window-size]
    [mount.core :as mount]
    [print.foo :include-macros true]
    [re-frame.core :as re-frame]))

(storage/reg-co-fx!
  :district-hegex                                        ;; local storage key
  {:fx :store                                               ;; re-frame fx ID
   :cofx :store})                                           ;; re-frame cofx ID

(defn dev-setup! []
  (when (:debug? config/config-map)
    (s/check-asserts true)
    (enable-console-print!)))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::my-account-route-active
  interceptors
  (fn [{:keys [:db]} arg2]
    {:async-flow {:rules [{:when :seen-all-of?
                           :events [::web3-accounts-events/set-accounts
                                    ::contracts-events/contracts-loaded]
                           :dispatch-n [[::events/add-contract-wrappers]
                                        [::listen-to-account-change]
                                        [::trading-events/restore-and-watch-txs]
                                        [::trading-events/load-pool-eth]
                                        [::trading-events/load-pool-btc]
                                        [::events/load-my-hegic-options]]}]}}))


(re-frame/reg-event-fx
  ::init
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [:db :store]}]
    {:db (cond-> db
           :always
           (assoc :district-hegex.ui.my-account (:district-hegex.ui.my-account store))

           :always
           (assoc :district-hegex.ui.core/votes (:district-hegex.ui.core/votes store))

           (:dark-mode? store)
           (assoc-in [::home-events/dark-mode?] true))
     :dispatch-n (cond-> [[::my-account-route-active]]
                   (:dark-mode? store) (conj [::home-events/set-dark-mode]))}))

(defn ^:export init []
  (dev-setup!)
  (let [full-config (cljs-utils/merge-in
                      config/config-map
                      {:smart-contracts {:request-timeout 120000
                                         :format :truffle-json}
                       :web3-balances {:contracts {:WBTC {:address "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599"}}}
                       :web3-account-balances {:for-contracts [:ETH :WBTC]}
                       :web3-tx-log {:tx-costs-currencies [:USD]
                                     :default-settings {:from-active-address-only? true}}
                       :reagent-render {:id "app"
                                        :component-var #'router}
                       :router {:routes routes
                                :default-route :route/not-found
                                :scroll-top? true}
                       :notification {:default-show-duration 3000
                                      :default-hide-duration 1000}})]

    (println "dbg init0")

    (js/console.log "config:" (clj->js full-config))
    (-> (mount/with-args full-config)
      (mount/start))
    (re-frame/dispatch-sync [::init])))

(defn  reinit []
  (let [full-config (cljs-utils/merge-in
                      config/config-map
                      {:smart-contracts {:format :truffle-json}
                       ;; :web3-account-balances {:for-contracts [:ETH :DNT]}
                       :web3-tx-log {:tx-costs-currencies [:USD]
                                     :default-settings {:from-active-address-only? true}}
                       :reagent-render {:id "app"
                                        :component-var #'router}
                       :router {:routes routes
                                :default-route :route/not-found
                                :scroll-top? true}
                       :notification {:default-show-duration 3000
                                      :default-hide-duration 1000}})]

    (println "dbg init0")

    (js/console.log "config:" (clj->js full-config))
    (-> (mount/with-args full-config)
      (mount/start))
    (re-frame/dispatch [::init])))


(re-frame/reg-event-fx
  ::listen-to-account-change
  (fn []
    (.on
     (.-ethereum js/window)
     "accountsChanged"
     (fn [accounts]
       (js/location.reload)
       #_(re-frame/dispatch [::account-changed])))
    {}))

(re-frame/reg-event-fx
  ::account-changed
  (fn [{:keys [db]}]
    {:db (dissoc db
                 ::hegex-nft/hegic-options
                 ::trading-events/hegic-options)
     ::reboot true}))

(re-frame/reg-fx
  ::reboot
  (fn []
    (reinit)))
