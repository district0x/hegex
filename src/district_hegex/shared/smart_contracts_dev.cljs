(ns district-hegex.shared.smart-contracts-dev)

(def smart-contracts
  {:optionchef {:name "OptionChef" :address "0xe3c55a753f9de0bfe42010d48bbe34010d7766d9"}
   :hegexoption {:name "Hegexoption" :address "0x591b42d573c293c58e22242239f58c251f19dc9f"}
   :optionchefdata {:name "OptionChefData" :address "0x8c586cf3ca4e9aa114cb60b3512ffe8ec019426e"}
   ;; NOTE external contracts down below
   :weth {:name "WETH" :address "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"}
   :wbtcoptions {:name "HegicWBTCOptions" :address "0x3961245DB602eD7c03eECcda33eA3846bD8723BD"}
   :hegicethoptions {:name "HegicETHOptions" :address "0xEfC0eEAdC1132A12c9487d800112693bf49EcfA2"}})
