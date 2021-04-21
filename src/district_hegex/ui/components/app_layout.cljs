(ns district-hegex.ui.components.app-layout
  (:require
   [district-hegex.ui.components.nav :as nav]
   [district-hegex.ui.components.tabs :as tabs]
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
   [:img.hegexlogo {:src (if @(subscribe [::home-subs/dark-mode?])
                           "/images/logo-dark.png"
                           "/images/logo-light.png")}]
   [tabs/tab {:caption "Hegex"}]])

(defn- night-mode []
  [:div.nightmode
   [:label.switch
    [:input {:on-click #(dispatch [::home-events/toggle-dark-mode])
             :checked @(subscribe [::home-subs/dark-mode?])
             :type "checkbox"}]
    [:div]]])

;;TODO clear up whether active account belong under "+"
(defn- header [active-page-name about?]
  (let [open-about #(dispatch [::home-events/toggle-open-about])]
    [:header
     {:style {:position "absolute"
              :top "0px"
              :left "0px"
              :width "100%"
              :max-width "980px"
              :z-index "99999"}}
    [:div.header-space
     [logo]
     [night-mode]
     [:h4.about {:on-click open-about
                 :style {:cursor "pointer"
                         :font-weight "100"}} "About"]
     [:span.about-section {:on-click open-about}
      [:a.bt-about [:span]]]]]))

(defn- footer []
  [:footer#globalFooter
   [:div {:style {:margin-top "5em"}}]])

(defn- about []
  [:section#about
   [:div.containerAbout
    [:div.contentAbout
     [:div.container
      [:header [:h2
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea con sedujal."
                ]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea con sedujal."
        ]
       ]]]]])

(defn app-layout [& children]
  (let [dark? @(subscribe [::home-subs/dark-mode?])
        about? @(subscribe [::home-subs/open-about?])]
    (println "about is" about?)
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
     [:div {:className (cond-> "app-layout" about? (str " openAbout"))}
      [about]
      [header :route/home about?]
      (into [:div#page-content]
            children)
      [footer]]]))
