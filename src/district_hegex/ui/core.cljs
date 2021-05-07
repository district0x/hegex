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
    ;;NOTE
    ;;active-account-change d0x sub triggers infinite loop
    {:async-flow {:rules [#_{:when :seen-any-of?
                           :events [::web3-accounts-events/active-account-changed]
                             :dispatch [::events/reboot]}
                          #_{:when :seen-all-of?
                           :events [::hegex-nft/my-hegex-options-count
                                    ::hegex-nft/approved-for-exchange?]
                           :dispatch [::hegex-nft/hide-loader]}
                          {:when :seen-any-of?
                           :events [::web3-accounts-events/set-accounts]
                           :dispatch-n [[::events/add-contract-wrappers]
                                        [::trading-events/restore-and-watch-txs]
                                        [::events/load-my-hegic-options]]}]}}))


(re-frame/reg-event-fx
  ::init
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [:db :store]}]
    (println "dbg init")
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
    (re-frame/dispatch-sync [::init])))
