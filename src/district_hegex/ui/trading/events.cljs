(ns district-hegex.ui.trading.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-fx]]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
    [district.ui.web3-tx-id.events :as tx-id-events]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3.queries :as web3-queries]
   [web3 :as web3webpack]
    [district.web3-utils :as web3-utils]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       gget
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
    [district-hegex.ui.contract.hegex-nft :as hegex-nft]
   [cljs.core.async :refer [go]]
   [bignumber.core :as bn]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs-bean.core :refer [bean ->clj ->js]]
   ["@0x/connect" :as connect0x]
   ["@0x/web3-wrapper" :as web3-wrapper]
   ["@0x/contract-wrappers" :as contract-wrappers]
   ["@0x/contract-addresses" :as contract-addresses]
   ["@0x/utils" :as utils0x]
   ["@0x/order-utils" :as order-utils0x]
   ["@0x/subproviders" :as subproviders0x]
   [cljs-0x-connect.http-client :as http-client]))

(def interceptors [re-frame/trim-v])

(def ^:private null-address "0x0000000000000000000000000000000000000000")

(defn- get-0x-addresses [chain-id]
  (println  "calling 0x addresses " chain-id contract-addresses)
  (ocall contract-addresses "getContractAddressesForChainOrThrow" chain-id))


;; biggest relay, no ropsten
#_(def ^:private apiclient
  (http-client/create-http-client "https://api.radarrelay.com/0x/v3/"))

#_(def ^:private apiclient
  (http-client/create-http-client "https://sra.bamboorelay.com/ropsten/0x/v3/"))


;; Uncaught (in promise) SyntaxError: JSON.parse: unexpected character at line 1 column 1 of the JSON data
#_(defn- orders-legacy []
;; get-token-pairs-async
  (js-invoke
   (http-client/get-orderbook-async
          apiclient
          #_{:request token-pair}) "then"
         (fn [r]
           (println "orders are" r))))

#_(orders)

;; REWRITE USING js 0x/connect and up-to-date 0x lifecycle

(def ^:private relayer-client
  (let [HttpClient  (oget connect0x "HttpClient")]
    (HttpClient. "https://0xapi.qa.district0x.io/sra/v3/")))


;;hegex dedicated ropsten relay
;;

;; only ropsten relays
;; https://sra.bamboorelay.com/ropsten/0x/v3/
;; https://api-v2.ledgerdex.com/sra/v2/
;; https://api.openrelay.xyz/v2/

;; biggest relay, no ropsten:
;; https://api.radarrelay.com/0x/v3/

(def ^:private decimals 18)

;; const zrxTokenAddress = contractWrappers.contractAddresses.zrxToken;
;; const etherTokenAddress = contractWrappers.contractAddresses.etherToken;

;;L0L
;; 0x requires its own BigNumber see:
;; https://github.com/0xProject/0x-monorepo/issues/92#issuecomment-414601020
(defn ->0x-bn [n]
  (new (oget utils0x "BigNumber") n))


;;NOTE
;;won't work until approval to proxy
;;submits a new 0x order in async code golf
(defn order! [{:keys [id total expires db]} form-open?]
  ;;placing an order for 1 Hegex
  ;; TODO
  ;; to-big-number *NFT ID*, swap for dynamic
  ;; from user input
  ;; parse and rebind params
  (let [eth-price (or (some-> total js/parseFloat) 0)
        expires-secs (or (some-> expires js/parseInt (* 3600)))
        hegex-id id]
    (println "sending in an offer with params: " eth-price expires-secs hegex-id)
    (go
     (let [nft-id (->0x-bn hegex-id)
           Wrapper (oget web3-wrapper "Web3Wrapper")
           ContractWrapper (oget contract-wrappers "ContractWrappers")
           wrapper    (new Wrapper
                           (gget  "web3" ".?currentProvider"))
           contract-wrapper (new ContractWrapper
                                 (gget  "web3" ".?currentProvider")
                                 (->js {:chainId 3}))
           weth-address (oget contract-wrapper ".?contractAddresses.?etherToken")
           _ (println "weth is " weth-address)
           ;; produces the wrong value on ropsten, swap for literal for the time being
           exchange-address (or  "0xFb2DD2A1366dE37f7241C83d47DA58fd503E2C64"
                                  #_(oget contract-wrapper ".?contractAddresses.?exchange"))
           maker-asset-data (<p! (ocall
                                  (ocall
                                   (oget contract-wrapper "devUtils")
                                   "encodeERC721AssetData"

                                   ;;to-bignumber not working here, type mimatch
                                   (contract-queries/contract-address db :hegexoption)
                                   nft-id)
                                  "callAsync"))
           taker-asset-data (<p! (ocall
                                  (ocall
                                   (oget contract-wrapper "devUtils")
                                   "encodeERC20AssetData"
                                   weth-address)
                                  "callAsync"))
           maker-asset-amount  (ocall Wrapper
                                       "toBaseUnitAmount"
                                       (->0x-bn 1)
                                       0)
           taker-asset-amount (ocall Wrapper
                                      "toBaseUnitAmount"
                                      (->0x-bn eth-price)
                                      decimals)
           ;;order expiration stamp of 500 secs from now
           maker-address (first (<p! (ocall wrapper "getAvailableAddressesAsync")))
           expired-at (str (+ expires-secs (js/Math.floor (/ (js/Date.now) 1000))))
           order-config-request
           ;;kebab ok too
           (->js {:exchangeAddress exchange-address
                  :makerAddress maker-address
                  :takerAddress null-address
                  :expirationTimeSeconds expired-at
                  :makerAssetAmount maker-asset-amount
                  :takerAssetAmount taker-asset-amount
                  :makerAssetData maker-asset-data
                  :takerAssetData taker-asset-data})
           order-config (<p! (ocall relayer-client "getOrderConfigAsync"
                                    order-config-request))
           order (->js (merge
                        {:salt (ocall order-utils0x "generatePseudoRandomSalt")
                         :chainId 3}
                        (->clj order-config-request)
                        (->clj order-config)))
           _ (println "signing order"  order "nft id is" nft-id)
           signed-order (<p! (ocall order-utils0x ".signatureUtils.ecSignOrderAsync"
                                    ;;NOTE
                                    ;;MM/0x subprovider bug workaround
                                    ;;https://forum.0x.org/t/release-0x-js-2-0-0-things-you-need-to-know/207
                                    (new (oget subproviders0x "MetamaskSubprovider")
                                         (gget  "web3" ".?currentProvider"))
                                    order
                                    maker-address))
           ;;DEV
           ;;order ok?
           #_order-ok? #_( println "is order ok?"   (<p! (.callAsync
                                                      (.getOrderRelevantState
                                                       (.-devUtils contract-wrapper)
                                                       signed-order
                                                       (.-signature signed-order)))))]
       (try
         (println "submitted order..."
                  (<p! (ocall relayer-client "submitOrderAsync" signed-order)))
         (reset! form-open? false)
         (catch js/Error err (js/console.log (ex-cause err))))))))

(defn- clean-hegic []
  (dispatch [::hegex-nft/clean-hegic]))

(defn fill! [{:keys [hegex-id sra-order taker-asset-amount]}]
  (println "filling in an order with params..."  hegex-id sra-order taker-asset-amount)
  (println "dbgorderload" "start")
  (let [order-obj (->js sra-order)]
    (go
     (let [Wrapper (oget web3-wrapper "Web3Wrapper")
           ContractWrapper (oget contract-wrappers "ContractWrappers")
           Web3 web3webpack
           web3js (Web3. (gget ".?web3.?currentProvider"))
           ;; _ (oset! js/window "web3" web3js)
           ;; _ (js/window.ethereum.enable)
           wrapper    (new Wrapper
                           (gget  "web3" ".?currentProvider")
                           ;; js/window.ethereum
                           #_(gget  "web3"))
           taker-address (first (<p! (ocall wrapper "getAvailableAddressesAsync")))
           ;; _ (println "taker address is" taker-address)
           ;; _ (println "current prov is")
           ;;  _ (js/console.log (gget  "web3" ".?currentProvider"))
           ;;  _ (js/console.log web3js)
           contract-wrapper (new ContractWrapper
                                 (gget  "web3" ".?currentProvider")
                                 (->js {:chainId 3
                                        :from taker-address
                                        :contractAddresses (-> (get-0x-addresses 3)
                                                               bean
                                                               (assoc :exchange "0xFb2DD2A1366dE37f7241C83d47DA58fd503E2C64")
                                                               ->js)}))]
       (println "fill dbg order is" (oget order-obj ".?order"))
       (println "fill dbg asset amount is" taker-asset-amount)
       (println "fill dbg signature is" (oget order-obj ".?order.?signature"))
       (println "_________________________________________________--")
       (js/console.log contract-wrapper)
       (println "addresses are" (get-0x-addresses 1))
#_0xFb2DD2A1366dE37f7241C83d47DA58fd503E2C64
       (println "kovan addresses are" (oget (get-0x-addresses 42) ".?exchange"))
;; (oget (get-0x-addresses 1) ".?exchange")
       (println "new ropsten addresses are"
                (-> (get-0x-addresses 3)
                    bean
                    (assoc :exchange "0xFb2DD2A1366dE37f7241C83d47DA58fd503E2C64")
                    ->js))
       (println "tx is.....")
       (js/console.log contract-wrapper)
       (js/console.log (ocall contract-wrapper
                              ".exchange.fillOrder"
                              (->js (oget order-obj ".?order"))
                              (->js taker-asset-amount)
                              (->js (oget order-obj ".?order.?signature"))))

       (println "owner::::")
       (js/console.log (<p! (ocall  (ocall contract-wrapper
                                       ".exchange.owner"
                                       (->js (oget order-obj ".?order"))
                                       (->js taker-asset-amount)
                                       (->js (oget order-obj ".?order.?signature")))
                                ".callAsync"
                                )))
       (println "over--------------")

       ;; (js/console.log (<p! (js/window.ethereum.enable)))
       ;; (js/console.log js/window.ethereum)
       (try
         (let [pre-tx (ocall contract-wrapper
                             ".exchange.fillOrder"
                             (->js (oget order-obj ".?order"))
                             (->js taker-asset-amount)
                             (->js (oget order-obj ".?order.?signature")))
               _ (println "pre-tx is" pre-tx)
               tx (<p! (ocall pre-tx
                              ".sendTransactionAsync"
                              (->js  {:from taker-address
                                      :gasPrice "600000"})
                              (->js {:shouldValidate false})))]
           (println "dbgfillorder..." "obj" tx)
           (dispatch [::watch-transaction tx])
           #_(dispatch [::tx-events/add-tx tx])
          #_ (dispatch [::tx-events/tx-hash tx])
          #_(dispatch [::tx-id-events/add-tx-hash :fill-0x-order tx {:fn :fillOrder
                                                                   :from "0x4E406a4B31b3C42D9c183eA1c5bACf355E055577"}])
           (js/console.log tx)
           (println "dbgfillorder added tx" tx (type tx) "string?" (string? tx))
           )
         #_(clean-hegic)
         (catch js/Error err (js/console.log (ex-cause err))))))))


(reg-event-fx
  ::tx-success
  interceptors
  (fn [{:keys [:db]} _]
    (println "dbgtx mined")))

(reg-event-fx
  ::error
  interceptors
  (fn [{:keys [:db]} _]
    (println "dbgtx error")))

(reg-event-fx
  ::watch-transaction
  interceptors
  (fn [{:keys [:db]} [tx-hash]]
    {:web3/watch-transactions {:web3 (:web3 db)
                               :transactions [{:id :my-watcher
                                               :tx-hash tx-hash
                                               :on-tx-success [::tx-success]
                                               :on-tx-error [::error]
                                               :on-tx-receipt [::tx-receipt]}]}}))

(defn cancel! [{:keys [sra-order taker-asset-amount]}]
  (let [order-obj (->js sra-order)]
    (go
     (let [Wrapper (oget web3-wrapper "Web3Wrapper")
           ContractWrapper (oget contract-wrappers "ContractWrappers")
           wrapper    (new Wrapper (gget  "web3" ".?currentProvider"))
           taker-address (first (<p! (ocall wrapper "getAvailableAddressesAsync")))
           contract-wrapper (new ContractWrapper
                                 (gget  "web3" ".?currentProvider")
                                 (->js {:chainId 3
                                        :from taker-address
                                        :contractAddresses (-> (get-0x-addresses 3)
                                                               bean
                                                               (assoc :exchange "0xFb2DD2A1366dE37f7241C83d47DA58fd503E2C64")
                                                               ->js)}))]
       #_(js/console.log (oget contract-wrapper ".cancelOrder"))
       (try
         (println "filling order..." (<p! (ocall (ocall contract-wrapper
                                                        ".exchange.cancelOrder"
                                                        (->js (oget order-obj ".?order"))
                                                        (->js taker-asset-amount)
                                                        (->js (oget order-obj ".?order.?signature")))
                                                 ".awaitTransactionSuccessAsync"
                                                 (->js  {:from taker-address
                                                         ;; :value "60000000000000000"
                                                         :gas "5000000"
                                                         :gasPrice "600000"})
                                                 (->js {:shouldValidate false}))))
         (dispatch [::hegex-nft/clean-hegic])
         (catch js/Error err (js/console.log (ex-cause err))))))))



;; request stuff

(defn- parse-order! [contract-wrapper order-obj]
  (let [order (->clj order-obj)
        order-hash (-> order :metaData :orderHash)
        asset-data (-> order :order :makerAssetData)
        eth-price (-> order :order :takerAssetAmount)]
    (go
      (dispatch [::get-order-nft
                order-hash
                (last
                 (bean
                  (<p! (ocall!
                        (ocall! contract-wrapper
                                ".?devUtils.?decodeERC721AssetData" asset-data)
                        "callAsync"))))
                (some-> eth-price web3-utils/wei->eth bn/number)
                eth-price
                 order]))
    nil))

(re-frame/reg-event-fx
  ::parse-order
  interceptors
  (fn [{:keys [db]} [order-obj]]
    (when-let [contract-wrapper (get db :contract-wrapper-0x)]
      (parse-order! contract-wrapper order-obj))))

;; side-effectful, turn into doseq re-frame dispatch-n
;; double check here to prevent overloading web3 on polling
;; NOTE look into 0x ws as an alternative to book polling
(re-frame/reg-event-fx
  ::parse-orderbook
  interceptors
  (fn [_ [book]]
    (when book
      {:dispatch-n (mapv (fn [o] [::parse-order o]) book)})))


(defn load-orderbook []
  (go
    (try
      (let [r (oget (<p! (ocall relayer-client "getOrdersAsync")) ".?records")
            _ (println "--------------- records in enclosing func received" )]
        (dispatch [::parse-orderbook r]))
      (catch js/Error err (js/console.log (ex-cause err))))))

(re-frame/reg-fx
  ::load-orderbook!
  (fn []
    ;;arg is irrelevant
    (println "-------------------------------loading-orderbook")
    (load-orderbook)
    (println "-------------------------------loaded-orderbook")))

(re-frame/reg-event-fx
  ::load-orderbook
  (fn []
    {::load-orderbook! true}))


;; callasync res is #js
;; [0x02571792
;; 0x3ea0eab5fc002c0b02842996cbed4ce2e20ee7c5
;; #object[BigNumber 8]]


(re-frame/reg-event-fx
  ::get-order-nft
  interceptors
  (fn [{:keys [db]}  [order-hash nft eth-price raw-price order]]
    (let [hash-path [::hegic-options :orderbook :hashes]]
      ;; this is a naive filter, hegex option params can change while
      ;;  an order is active
      (when (and (not (some #{order-hash} (get-in db hash-path)))
                 (bn/number (val nft)))
        {:db (update-in db hash-path conj order-hash)
         :dispatch [::hegex-nft/uhegex-option
                    (bn/number (val nft))
                    eth-price
                    raw-price
                    order]}))))

(re-frame/reg-event-fx
  ::assoc-errors
  interceptors
  (fn [{:keys [db]}  [errs]]
    ;;TODO
    ;;dispatch snackbar effect
    {:db (assoc-in db [::hegex-nft/hegic-options :ui-errors] errs)}))

(defn- validate-offer [params]
  (if-not (> (:total params) 0)
    ["Price can't be 0"]
    []))

(re-frame/reg-fx
  ::create-offer!
  (fn [params]
    (let [errs (validate-offer (dissoc params :db))]
      (if (pos? (count errs))
        (dispatch [::assoc-errors errs])

        (do
          (dispatch [::assoc-errors []])
          (order! params (:open? params)))))))

(re-frame/reg-event-fx
  ::create-offer
  interceptors
  (fn [{:keys [db]} [params open?]]
    {::create-offer! (assoc params :open? open? :db db)}))


(re-frame/reg-fx
  ::fill-offer!
  (fn [params]
    (fill! params)))

(re-frame/reg-event-fx
  ::fill-offer
  interceptors
  (fn [_ [params]]
    {::fill-offer! params}))

(re-frame/reg-fx
  ::cancel-offer!
  (fn [params]
    (cancel! params)))

(re-frame/reg-event-fx
  ::cancel-offer
  interceptors
  (fn [_ [params]]
    {::cancel-offer! params}))

;;REPL functions

;; NOTE
;; approve ropsten relayer proxy to spend NFTs (todo in UI)
#_(order! 7 0.2)



#_(load-orderbook 7)


;;verify orders @ https://0xapi.qa.district0x.io/sra/v3/orders


#_[{:order {:signature "0x1cdbc53a31c61413cd53693e4ded51017e1d145421510b79beff5a5c1881475e4c35de128c5f1a7ea6554e470356a9065a33a7b61a7926863adbca67f959a78a2702",
            :senderAddress "0x0000000000000000000000000000000000000000",
            :makerAddress "0xea65e6b51a320d96ed4cd01cfec9d91bcc442f45",
            :takerAddress "0x0000000000000000000000000000000000000000",
            :makerFee [BigNumber 0], :takerFee [BigNumber 0],
            :makerAssetAmount [BigNumber 1],
            :takerAssetAmount [BigNumber 100000000000000000],
            :makerAssetData "0x025717920000000000000000000000003ea0eab5fc002c0b02842996cbed4ce2e20ee7c50000000000000000000000000000000000000000000000000000000000000007",
            :takerAssetData "0xf47261b0000000000000000000000000c778417e063141139fce010982780140aa0cd5ab",
            :salt [BigNumber 97363250720891692899156508445369015752196185603545364700017471450561148163785],
            :exchangeAddress "0xfb2dd2a1366de37f7241c83d47da58fd503e2c64",
            :feeRecipientAddress "0x0000000000000000000000000000000000000000",
            :expirationTimeSeconds [BigNumber 1610906374],
            :makerFeeAssetData "0x", :chainId 3, :takerFeeAssetData "0x"},
    :metaData {:orderHash "0xff8c106d9ae0bca45615d5c6398dfe83c8aee50b456f9ab6450d5a7e463e3040",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             :remainingFillableTakerAssetAmount "100000000000000000",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             :createdAt "2021-01-17T17:51:17.952Z"}}]
#_ (get-0x-addresses 3)


;; NOTES
;;
;;account 2 is mintr
;;account 3 is buyer
