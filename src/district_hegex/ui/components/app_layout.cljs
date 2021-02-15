(ns district-hegex.ui.components.app-layout
  (:require
   [district-hegex.ui.components.nav :as nav]
   [react-dom :as rdom]
   [district-hegex.ui.subs :as dr-subs]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       gget
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
    [clojure.string :as str]
   [reagent.core :as r]))


(defn header [active-page-name]
  (let [acc-raw (subscribe [::account-subs/active-account])
        acc-short (some-> @acc-raw (subs 0 10) (str "..."))]
    [:header#globalHeader
     [:div.container [:div.hegexlogo [:h1 {:style {:color "#48aff0"}} "HEG" ] [:h1.special "EX"]]
     [:nav.toplinks]
     [:div.dnt-wrap
      [:div.total-dnt]
      [:> (c/c :tag)
       {:intent "success"
        :large true
        :minimal true}
       acc-short]

      #_     [nav/a {:route [:route/home {}]}

              #_[:div.select-menu
                 [:div.select-choice.cta-btn.my-account-btn
                  [:div.select-text "My Account"]]]]]]]))

(defn footer []
  [:footer#globalFooter
   #_[:div.bg-wrap
    [:div.background.sized
     [:img {:src "/images/blobbg-bot@2x.png"}]]]
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
#_(def hegex-theme
  {:fontSizes  [12 14 16 24 32 48 64],
   :backgroundColor 'green',
    :colors  {:primary "hotpink", :gray "#f6f6ff"},
    :buttons
       {:primary  {:color "white", :bg "primary"},
        :outline
           {:color "primary",
            :bg "transparent",
            :boxShadow "inset 0 0 0 2px"}}})
(def hg-theme
   {:colors  {:background "black", :primary "tomato"},
    :space  [0 6 12 24 48],
    :fontSizes  [14 16 18 20 24],
    :radii  {:default 12}})

(defn app-layout [& children]
  [:div {:className "bp3-dark" 
         :id (case :route/home
                    :route/about "page-about"
                    :route/detail "page-details"
                    :route/home "page-registry"
                    :route/submit "page-submit"
                    :route/edit "page-submit"
                    :route/my-account "page-my-account"
                    :route/terms "page-terms"
                    :route/not-found "not-found")}
        [header :route/home]
        (into [:div#page-content]
              children)
        [footer]])

#_(keys (bean styled))
