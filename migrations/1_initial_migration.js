const { env, smartContractsPath, contractPathByNet } = require("../truffle.js");
const { encodeContractEDN, writeSmartContracts, getSmartContractAddress, readSmartContractsFile, setSmartContractAddress } = require("./utils.js");



let smartContractsList = [];

const hegicETHFactory = {
  "ropsten": "0x77041D13e0B9587e0062239d083b51cB6d81404D",
  "mainnet": "0xefc0eeadc1132a12c9487d800112693bf49ecfa2",
}


const Migrations = artifacts.require("Migrations");
const chef = artifacts.require('OptionChef');
const token = artifacts.require('Hegexoption.sol');
const METADATA_BASE = "https://stacksideflow.github.io/hegexoption-nft/meta/"

module.exports = async (deployer, network) => {
  console.log("Migrating Hegex to " + network);
  await deployer.deploy(Migrations);
  const migrations = await Migrations.deployed();
  //important - will throw on localnet as hegic contracts are deployed separately
  await deployer.deploy(chef, hegicETHFactory[network]);
  const chefd = await chef.deployed();
  await deployer.deploy(token, chefd.address, METADATA_BASE);
  const tokend = await token.deployed();

  await chefd.updateHegexoption(tokend.address);

  let smartContracts = readSmartContractsFile(smartContractsPath);

  assignContract(chefd, "OptionChef", "optionchef");
  assignContract(tokend, "Hegexoption", "hegexoption");
  writeSmartContracts(contractPathByNet(network), smartContractsList, env)
};

function assignContract(contract_instance, contractName, contract_key, opts) {
  console.log("- Assigning '" + contractName + "' to smart contract listing...");
  opts = opts || {};
  smartContractsList = smartContractsList.concat(
    encodeContractEDN(contract_instance, contractName, contract_key, opts));
}
