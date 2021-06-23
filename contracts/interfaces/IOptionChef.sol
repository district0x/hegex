// SPDX-License-Identifier: UNLICENSED

pragma solidity 0.6.12;

import "./IOptions.sol";

interface IOptionChef {
    function isDelegated(uint _tokenId) external view returns (bool);
    function getUnderlyingOptionParams(uint8 _optionType, uint _tokenId)
        external
        view
        returns (
        IHegicOptions.State state,
        address payable holder,
        uint256 strike,
        uint256 amount,
        uint256 lockedAmount,
        uint256 premium,
        uint256 expiration,
        IHegicOptions.OptionType optionType);
}
