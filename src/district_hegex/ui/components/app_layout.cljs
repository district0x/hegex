(ns district-hegex.ui.components.app-layout
  (:require
   [district-hegex.ui.components.nav :as nav]
   [district-hegex.ui.components.tab :as tabs]
   [react-dom :as rdom]
   [district-hegex.ui.home.events :as home-events]
   [district-hegex.ui.home.subs :as home-subs]
   [district-hegex.ui.subs :as dr-subs]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply! gget
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [cljs-bean.core :refer [bean ->clj ->js]]
   [district.ui.web3-accounts.subs :as account-subs]
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.router.events]
   [district-hegex.ui.components.components :as c]
   [district.ui.router.subs :as router-subs]
   [district.ui.router.utils :as router-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))


(defn- logo []
  [:div.header-logo
   [:img.hegexlogo {:src "/images/hegexLogo.png"}]
   [tabs/tab {:caption "Hegex"}]])

(defn- night-mode []
  [:div.nightmode
   [:label.switch
    [:input {:on-click #(dispatch [::home-events/toggle-dark-mode])
             :checked @(subscribe [::home-subs/dark-mode?])
             :type "checkbox"}]
    [:div]]])

;;TODO clear up whether active account belong under "+"
(defn header [active-page-name]
  [:header
   [:div.header-space
    [logo]
    [night-mode]
    [:h4 "About"]
    [:h1 "+"]]])

(defn footer []
  [:footer#globalFooter
   [:div {:style {:margin-top "5em"}}]])

(defn app-layout [& children]
  (let [dark? @(subscribe [::home-subs/dark-mode?])]
    [:div (cond-> {:id (case :route/home
                    :route/about "page-about"
                    :route/detail "page-details"
                    :route/home "page-registry"
                    :route/submit "page-submit"
                    :route/edit "page-submit"
                    :route/my-account "page-my-account"
                    :route/terms "page-terms"
                    :route/not-found "not-found")}
            (not dark?) (assoc :className "day")
            dark? (assoc :className "night bp3-dark dark-overlay") )
     [:div.app-layout
      [header :route/home]
      (into [:div#page-content]
            children)
      [footer]]]))
