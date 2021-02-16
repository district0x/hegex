(defproject district0x/hegex "1.0.0"
  :description "District0x-powered synthetic options DEX"
  :url "https://github.com/district0x/hegex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xss2m" "-XX:+TieredCompilation" "-XX:TieredStopAtLevel=1"]
  :dependencies [[camel-snake-kebab "0.4.0"]
                 [org.clojure/core.async "1.3.610"]
                 [district0x/re-frame-interval-fx "1.0.2"]
                 [binaryage/oops "0.7.0"]
                 [cljs-bean "1.6.0"]
                 [cljs-web3 "0.19.0-0-10"]
                 [cljs-web3-next "0.1.3"]
                 [cljsjs/bignumber "4.1.0-0"]
                 [cljsjs/buffer "5.1.0-1"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [cljsjs/react "17.0.1-0"]
                 [cljsjs/react-dom "17.0.1-0"]
                 [cljsjs/recharts "1.6.2-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [district0x/async-helpers "0.1.3"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/cljs-ipfs-native "1.0.1"]
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 [district0x/district-cljs-utils "1.0.3"]
                 [district0x/district-encryption "1.0.0"]
                 [district0x/district-format "1.0.8"]
                 [district0x/district-parsers "1.0.0"]
                 [district0x/district-sendgrid "1.0.0"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 [district0x/district-server-logging "1.0.6"]
                 [district0x/district-server-middleware-logging "1.0.0"]
                 [district0x/district-server-smart-contracts "1.2.4"]
                 [district0x/district-server-web3 "1.2.6"]
                 [district0x/district-server-web3-events "1.1.9"]
                 [district0x/district-ui-component-active-account "1.0.0"]
                 [district0x/district-ui-component-active-account-balance "1.0.1"]
                 [district0x/district-ui-component-form "0.2.13"]
                 [district0x/district-ui-component-notification "1.0.0"]
                 [district0x/district-ui-component-tx-button "1.0.0"]
                 [district0x/district-ui-conversion-rates "1.0.1"]
                 [district0x/district-ui-ipfs "1.0.0"]
                 [district0x/district-ui-logging "1.1.0"]
                 [district0x/district-ui-notification "1.0.1"]
                 [district0x/district-ui-now "1.0.1"]
                 [stacksideflow/district-ui-reagent-render "1.0.1"]
                 [district0x/district-ui-router "1.0.5"]
                 [district0x/district-ui-router-google-analytics "1.0.1"]
                 [district0x/district-ui-smart-contracts "1.0.8"]
                 [district0x/district-ui-web3 "1.3.2"]
                 [district0x/district-ui-web3-account-balances "1.0.2"]
                 [district0x/district-ui-web3-accounts "1.0.7"]
                 [district0x/district-ui-web3-balances "1.0.2"]
                 [district0x/district-ui-web3-tx "1.0.12"]
                 [district0x/district-ui-web3-tx-id "1.0.1"]
                 [district0x/district-ui-web3-tx-log "1.0.13"]
                 [district0x/district-ui-window-size "1.0.1"]
                 [district0x/district-web3-utils "1.0.3"]
                 [district0x/eip55 "0.0.1"]
                 [district0x/error-handling "1.0.4"]
                 [district0x/re-frame-ipfs-fx "0.0.2"]
                 [funcool/bide "1.6.1-SNAPSHOT"]
                 [jamesmacaulay/cljs-promises "0.1.0"]
                 [medley "1.0.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojurescript "1.10.764"]
                 [org.clojure/core.async "1.3.610"]
                 [print-foo-cljs "2.0.3"]
                 [ivan0x/cljs-0x-connect "1.0.0"]
                 [re-frame "1.1.2"]
                 [reagent "1.0.0"]
                 [org.clojars.frozenlock/reagent-table "0.1.5"]]

  :exclusions [cljsjs/react-with-addons
               org.clojure/core.async
               district0x/async-helpers]
  :repositories [["public-github" {:url "git://github.com"}]]
  :plugins [[deraen/lein-less4clj "0.7.4"]
            [cider/cider-nrepl "0.25.2"]
            [lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.20"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-pdo "0.1.1"]]

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :less4clj {:target-path "resources/public/css-compiled"
             :source-paths ["resources/public/css"]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4177
             :css-dirs ["resources/public/css" "resources/public/css-compiled"]
             :repl-eval-timeout 60000}

  :aliases {"clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-css" ["less4clj" "auto"]
            "build-css" ["less4clj" "once"]
            ;; "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "ui"] ["build-css"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]
            ;; "build-tests" ["cljsbuild" "once" "server-tests"]
            ;; "test" ["do" ["build-tests"] ["shell" "node" "server-tests/server-tests.js"]]
            ;; "test-doo" ["doo" "node" "server-tests"]
            ;; "test-doo-once" ["doo" "node" "server-tests" "once"]
            }

  :clean-targets ^{:protect false} [[:solc :build-path]
                                    ".cljs_node_repl"
                                    "dev-server/"
                                    "resources/public/css-compiled/"
                                    "resources/public/js/compiled/"
                                    "server-tests/"
                                    "server/"
                                    "target/"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.439"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.5.2"]
                                  [figwheel-sidecar "0.5.20"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [lein-doo "0.1.8"]
                                  [org.clojure/clojure "1.9.0"]
                                  [org.clojure/tools.reader "1.3.0"]
                                  [re-frisk "1.3.5"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/district_hegex/ui"
                                       "src/district_hegex/shared"]
                        :figwheel {:on-jsload "district.ui.reagent-render/rerender"}
                        :compiler {:main "district-hegex.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "/js/compiled/out"
                                   :source-map-timestamp true
                                   :optimizations :none
                                   :npm-deps false
                                   :infer-externs true
                                   :foreign-libs
                                   [{:file "dist/index_bundle.js"
                                     :provides ["react"
                                                "react-dom"
                                                "web3"
                                                "stacked-snackbars"
                                                "with-stacked-snackbars"
                                                "@0x/connect"
                                                "@0x/contract-wrappers"
                                                "@0x/order-utils"
                                                "@0x/utils"
                                                "@0x/subproviders"
                                                "@0x/contract-addresses"
                                                ;;might end up unused
                                                "@0x/web3-wrapper"
                                                "rebass"
                                                "rebass/styled-components"
                                                "@rebass/preset"
                                                "@emotion/react"
                                                "@rebass/forms"
                                                "styled-components"
                                                "@blueprintjs/core"
                                                "@blueprintjs/icons"]
                                     :global-exports {react React
                                                      react-dom ReactDOM
                                                      web3 Web3x
                                                      stacked-snackbars StackedSnackbars
                                                      with-stacked-snackbars withSnackbar
                                                      "styled-components" Styled
                                                      "rebass/styled-components" RebassStyled
                                                      "@0x/connect" Connect0x
                                                      "@0x/contract-wrappers" Contract0x
                                                      "@0x/order-utils" OrderUtils0x
                                                      "@0x/utils" Utils0x
                                                      "@0x/subproviders" Subproviders0x
                                                      "@0x/contract-addresses" Addresses0x
                                                      ;;might end up unused
                                                      "@0x/web3-wrapper" Web3Wrapper0x
                                                      "rebass" Rebass
                                                      "@rebass/preset" RebassPreset
                                                      "@emotion/react" Emotion
                                                      "@rebass/forms" RebassForms
                                                      "@blueprintjs/core" Blueprint
                                                      "@blueprintjs/icons" BlueprintIcons}}]
                                   :preloads [print.foo.preloads.devtools
                                              re-frisk.preload]
                                   #_:external-config #_{:devtools/config {:features-to-install :all}}}}
                       {:id "ui"
                        ;; :source-paths ["src"]
                        :source-paths ["src/district_hegex/ui"
                                       "src/district_hegex/shared"]
                        :compiler {:main "district-hegex.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :pseudo-names false
                                   :npm-deps false
                                   :infer-externs true
                                   :foreign-libs
[{:file "dist/index_bundle.js"
                                     :provides ["react"
                                                "react-dom"
                                                "web3"
                                                "stacked-snackbars"
                                                "with-stacked-snackbars"
                                                "@0x/connect"
                                                "@0x/contract-wrappers"
                                                "@0x/order-utils"
                                                "@0x/utils"
                                                "@0x/subproviders"
                                                "@0x/contract-addresses"
                                                ;;might end up unused
                                                "@0x/web3-wrapper"
                                                "rebass"
                                                "rebass/styled-components"
                                                "@rebass/preset"
                                                "@emotion/react"
                                                "@rebass/forms"
                                                "styled-components"
                                                "@blueprintjs/core"
                                                "@blueprintjs/icons"]
                                     :global-exports {react React
                                                      react-dom ReactDOM
                                                      web3 Web3x
                                                      stacked-snackbars StackedSnackbars
                                                      with-stacked-snackbars withSnackbar
                                                      "styled-components" Styled
                                                      "rebass/styled-components" RebassStyled
                                                      "@0x/connect" Connect0x
                                                      "@0x/contract-wrappers" Contract0x
                                                      "@0x/order-utils" OrderUtils0x
                                                      "@0x/utils" Utils0x
                                                      "@0x/subproviders" Subproviders0x
                                                      "@0x/contract-addresses" Addresses0x
                                                      ;;might end up unused
                                                      "@0x/web3-wrapper" Web3Wrapper0x
                                                      "rebass" Rebass
                                                      "@rebass/preset" RebassPreset
                                                      "@emotion/react" Emotion
                                                      "@rebass/forms" RebassForms
                                                      "@blueprintjs/core" Blueprint
                                                      "@blueprintjs/icons" BlueprintIcons}}]}}]})
