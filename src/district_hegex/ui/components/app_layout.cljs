(ns district-hegex.ui.components.app-layout
  (:require
   [district-hegex.ui.components.nav :as nav]
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
   ["@emotion/react" :as emotion]
   ["@rebass/preset" :as rebass-preset]
   ["rebass/styled-components" :as rebass-styled]
   ["styled-components" :as styled]
   ["@blueprintjs/core" :as blueprint]
   ["rebass" :as rebass]
   [district-hegex.ui.components.components :as c]
   [district.ui.router.subs :as router-subs]
   [district.ui.router.utils :as router-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))


(defn- logo []
  [:div.header-logo
   [:img.hegexlogo {:src "/images/hegexLogo.png"}]])

(defn- night-mode []
  [:div.nightmode
   [:label.switch
    [:input {:on-click #(dispatch [::home-events/toggle-dark-mode])
             :checked @(subscribe [::home-subs/dark-mode?])
             :type "checkbox"}]
    [:div]]])

;;TODO clear up whether active account belong under "+"
(defn header [active-page-name]
  [:header#globalHeader
   [:div.header-space
    [logo]
    [night-mode]
    [:h4 "About"]
    [:h1 "+"]]])

(defn footer []
  [:footer#globalFooter
   [:div.container
    [:br]
    [:br]
    [:div.row.spaced
     [:div.col
      [:h2 "Largest DeFi NFT option exchange to date"]
      [:p "Brought to you with ❤ ️by " [:code  "district0x"]]
      [:br]]
     [:div.col
      [:nav.footerlinks
       [:ul
        [:li [:a {:href "https://blog.district0x.io" :target :_blank} "Blog"]]
        [:li [:a {:href "https://district0x.io/team/" :target :_blank} "Team"]]
        [:li [:a {:href "https://district0x.io/transparency/" :target :_blank} "Transparency"]]
        [:li [:a {:href "https://district0x.io/faq/" :target :_blank} "FAQ"]]
        [:li [nav/a {:route [:route/terms]} "Terms Of Use"]]]]]
     [:div.col
      [:a.cta-btn.has-icon
       {:href "https://discord.gg/rJvBEyV"
        :target :_blank}
       [:span "Join Us On Discord"]
       [:span.icon-discord]]]
     [:div.col
      [:nav.social
       [:ul
        [:li [:a {:href "https://www.facebook.com/district0x/"
                  :target :_blank}
              [:span.icon-facebook]]]
        [:li [:a {:href "https://www.reddit.com/r/district0x"
                  :target :_blank}
              [:span.icon-reddit-alien]]]
        [:li [:a {:href "https://t.me/district0x"
                  :target :_blank}
              [:span.icon-telegram]]]
        [:li [:a {:href "https://twitter.com/district0x"
                  :target :_blank}
              [:span.icon-twitter]]]
        [:li [:a {:href "https://blog.district0x.io"
                  :target :_blank}
              [:span.icon-medium]]]
        [:li [:a {:href "https://github.com/district0x"
                  :target :_blank}
              [:span.icon-github]]]]]]]]])

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
            dark? (assoc :className "bp3-dark dark-overlay") )
    [header :route/home]
    (into [:div#page-content]
          children)
    [footer]]))

