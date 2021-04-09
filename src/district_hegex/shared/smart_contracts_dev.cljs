(ns district-hegex.shared.smart-contracts-dev)

(def smart-contracts
  {:optionchef {:name "OptionChef" :address "0x9c6849af85ec10c385728da68be00aa7a9fd1bdd"}
   :hegexoption {:name "Hegexoption" :address "0x7d51ae46716c96ee46bd4be7a9ff65026d1ac08e"}
   :optionchefdata {:name "OptionChefData" :address "0x9a2eafa10a076751e2ad1493a6178dc4876cffd8"}
   ;; NOTE external contracts down below
   :weth {:name "WETH"
          :address "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"}
   :wbtcoptions {:name "HegicWBTCOptions"
                 :address "0x3961245DB602eD7c03eECcda33eA3846bD8723BD"}
   :hegicethoptions {:name "HegicETHOptions"
                     :address "0xEfC0eEAdC1132A12c9487d800112693bf49EcfA2"}})
