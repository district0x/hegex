'use strict';
const BN = require('bn.js');
const HDWalletProvider = require("@truffle/hdwallet-provider");
require('dotenv').config()  // Store environment-specific variable from '.env' to process.env

const DISTRICT_HEGEX_ENV = process.env.DISTRICT_HEGEX_ENV || "dev";

const smartContractsPaths = {
  "dev": '/src/district_hegex/shared/smart_contracts_dev.cljs',
  "qa": '/src/district_hegex/shared/smart_contracts_qa.cljs',
  "prod": '/src/district_hegex/shared/smart_contracts_prod.cljs'
};


let parameters = {
  "dev": {
    KitDistrict: { includeApps: ["voting", "vault", "finance"] },
    // KitDistrict : {includeApps: ["voting", "vault"]},
    // KitDistrict : {includeApps: ["finance"]},
    // KitDistrict : {includeApps: ["voting"]},
    districtRegistryDb: {
      challengePeriodDuration: 0,
      commitPeriodDuration: 60, // seconds
      revealPeriodDuration: 60, // seconds
      deposit: 1e18, // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
      voteQuorum: 50, // percent
    },
    paramChangeRegistryDb: {
      challengePeriodDuration: 600, // seconds
      commitPeriodDuration: 600, // seconds
      revealPeriodDuration: 600, // seconds
      deposit: 1e18, // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
      voteQuorum: 50 // percent
    }
  },
  "qa": {
    DNT: "0xe450dcde6c059339a35eec0facbe62751cca6e8a",
    ENS: "0x98df287b6c145399aaa709692c8d308357bc085d",
    DAOFactory: "0x2298d27a9b847c681d2b2c2828ab9d79013f5f1d",
    FIFSResolvingRegistrar: "0x3665e7bfd4d3254ae7796779800f5b603c43c60d",
    KitDistrict: { includeApps: ["voting"] },
    districtRegistryDb: {
      challengePeriodDuration: 0,
      commitPeriodDuration: 200, // seconds
      revealPeriodDuration: 200, // seconds
      deposit: 1e18, // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
      voteQuorum: 50, // percent
    },
    paramChangeRegistryDb: {
      challengePeriodDuration: 200, // seconds
      commitPeriodDuration: 200, // seconds
      revealPeriodDuration: 200, // seconds
      deposit: 1e18, // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
      voteQuorum: 50 // percent
    }
  },
  "prod": {
    DNT: "0x0abdace70d3790235af448c88547603b945604ea",
    // MiniMeTokenFactory: "0xa7dd95d9978dde794eae5233889f1ffebcdc9914",
    ENS: "0x314159265dd8dbb310642f98f50c066173c1259b",
    DAOFactory: "0xb9da44c051c6cc9e04b7e0f95e95d69c6a6d8031",
    FIFSResolvingRegistrar: "0x546aa2eae2514494eeadb7bbb35243348983c59d",
    KitDistrict: { includeApps: ["voting", "vault", "finance"] },
    District0xEmails: "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5",
    districtRegistryDb: {
      challengePeriodDuration: 0,
      commitPeriodDuration: 259200, // seconds
      revealPeriodDuration: 259200, // seconds
      deposit: "10000000000000000000000", // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
    },
    paramChangeRegistryDb: {
      challengePeriodDuration: 259200, // seconds
      commitPeriodDuration: 259200, // seconds
      revealPeriodDuration: 259200, // seconds
      deposit: "1000000000000000000000000000", // 1e18 = 1 DNT
      challengeDispensation: 50, // percent
    }
  }
};

const contractPathByNet = (netID) => {
  const netToEnv = {
    "ropsten": "qa",
    "mainnet": "prod",
    "live": "prod",
    "main": "prod"
  };
  return __dirname + smartContractsPaths[netToEnv[netID] || "dev"];
}

module.exports = {
  env: DISTRICT_HEGEX_ENV,
  smartContractsPath: __dirname + smartContractsPaths[DISTRICT_HEGEX_ENV],
  smartContractsPath: __dirname + smartContractsPaths[DISTRICT_HEGEX_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  contractPathByNet: contractPathByNet,
  parameters: parameters[DISTRICT_HEGEX_ENV],

  networks: {
    develop: {
      host: 'localhost',
      port: 9545,
      network_id: '*',
      skipDryRun: true
    },
    ropsten: {
      provider: function() {
        return new HDWalletProvider(`${process.env.MNEMONIC}`, `https://ropsten.infura.io/v3/${process.env.INFURA_API_KEY}`)
      },
      // confirmations: 2,
      network_id: 3,
      skipDryRun: true
    },
    "infura-mainnet": {
      provider: () => new HDWalletProvider(process.env.MNENOMIC || process.env.MAINNET_PRIV_KEY, "https://mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 1,
      gas: 6e6,
      gasPrice: 4e9,
      skipDryRun: true
    }
  },
  plugins: [
    'truffle-plugin-verify'
  ],
  api_keys: {
    etherscan: process.env.ETHERSCAN_API_KEY
  },
  compilers: {
    solc: {
      version: '0.6.12'
    }
  },
}
