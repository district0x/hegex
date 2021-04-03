# Hegex Options DEX

##  [DEV] Deployable Hegic Contracts
https://github.com/stacksideflow/contracts-v888


### Deps

- node `15.4.0`, likely any v15

## Development

Install npm deps and compile webpack:
```bash
npm i && npx webpack 
```

Compile contracts (assumes you have `solc` installed):
```bash
truffle compile
```
Migrate (ropsten):
```bash
truffle migrate --network ropsten
```

After migration is completed ABIs will be written to `/resources/public/contracts/build`, 
addresses will be written to shared `clj` file; ready to be consumed by frontend.

If you wish to skip `truffle migrate` and start UI without migrating the contracts:

``` bash
cp resources/external-abi/* resources/public/contracts/build/

```

Start UI:
```bash
lein build-css
lein repl
# alternatively, start from clean slate w/ webpack
rm -rf node_modules && lein clean && npm i && npx webpack --mode development && (echo "(start-ui\!)"; cat <&0) | lein repl
# go to http://localhost:4177/ 
```

### NB external ABIs

external ABIs (generated for contracts outside of the scope of this project) is kept under VC in `resources/external-abi` dir

### NB - security guarantees
- no double-minting
- beforeTokenTransfer hook locks NFT unless it's owned by Chef

### NB - current testnet deployments
-  https://ropsten.etherscan.io/address/0xf14f5073b4cf04231ebd71eb02c2072eee3b2f07 

-  https://ropsten.etherscan.io/address/0xc369dd7700c6d9a29038c129ec65b6a776b1aca1

### NB - current mainnet deployments

- OptionChef: https://etherscan.io/address/0x078705F1FeaF96735b17c213E14eD3756D0a6110

- NFT: https://etherscan.io/address/0xc08E1110a7b1E505148975dCB0014078B4B1Afa0


## NB - 0x relay for ropsten 

http://138.68.106.185:3000/sra/v3/
