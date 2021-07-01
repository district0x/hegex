(ns district-hegex.shared.smart-contracts-prod)

(def smart-contracts
  {:optionchef {:name "OptionChef" :address "0x8cbe7f44d63f95220b82566e0c20463008029830"}
   :hegexoption {:name "Hegexoption" :address "0x96f70638f10b9abb9a133b5685c880fb52156434"}
   :optionchefdata {:name "OptionChefData" :address "0x4238d64e26c45b87c29aa134dd50b5c7b9e213c2"}

   ;; NOTE external contracts down below
   :weth {:name "WETH"
          :address "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"}
   :wbtcoptions {:name "HegicWBTCOptions"
                 :address "0x3961245DB602eD7c03eECcda33eA3846bD8723BD"}
   :brokenethoptions {:name "BrokenETHOptions"
                     :address "0xEfC0eEAdC1132A12c9487d800112693bf49EcfA2"}
   :hegicercpool {:name "HegicERCPool" :address "0x20DD9e22d22dd0a6ef74a520cb08303B5faD5dE7"}
   :hegicethpool {:name "HegicETHPool" :address "0x878F15ffC8b894A1BA7647c7176E4C01f74e140b"}})
