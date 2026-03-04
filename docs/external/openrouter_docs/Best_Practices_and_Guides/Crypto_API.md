# Crypto API

> Learn how to purchase OpenRouter credits using cryptocurrency. Complete guide to Coinbase integration, supported chains, and automated credit purchases.

You can purchase credits using cryptocurrency through our Coinbase integration. This can either happen through the UI, on your [credits page](https://openrouter.ai/settings/credits), or through our API as described below. While other forms of payment are possible, this guide specifically shows how to pay with the chain's native token.

Headless credit purchases involve three steps:

1. Getting the calldata for a new credit purchase
2. Sending a transaction on-chain using that data
3. Detecting low account balance, and purchasing more

## Getting Credit Purchase Calldata

Make a POST request to `/api/v1/credits/coinbase` to create a new charge. You'll include the amount of credits you want to purchase (in USD, up to \$maxCryptoDollarPurchase), the address you'll be sending the transaction from, and the EVM chain ID of the network you'll be sending on.

Currently, we only support the following chains (mainnet only):

* Ethereum (1)
* Polygon (137)
* Base (8453) ***recommended***

```typescript
const response = await fetch('https://openrouter.ai/api/v1/credits/coinbase', {
  method: 'POST',
  headers: {
    Authorization: 'Bearer <OPENROUTER_API_KEY>',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    amount: 10, // Target credit amount in USD
    sender: '0x9a85CB3bfd494Ea3a8C9E50aA6a3c1a7E8BACE11',
    chain_id: 8453,
  }),
});
const responseJSON = await response.json();
```

The response includes the charge details and transaction data needed to execute the on-chain payment:

```json
{
  "data": {
    "id": "...",
    "created_at": "2024-01-01T00:00:00Z",
    "expires_at": "2024-01-01T01:00:00Z",
    "web3_data": {
      "transfer_intent": {
        "metadata": {
          "chain_id": 8453,
          "contract_address": "0x03059433bcdb6144624cc2443159d9445c32b7a8",
          "sender": "0x9a85CB3bfd494Ea3a8C9E50aA6a3c1a7E8BACE11"
        },
        "call_data": {
          "recipient_amount": "...",
          "deadline": "...",
          "recipient": "...",
          "recipient_currency": "...",
          "refund_destination": "...",
          "fee_amount": "...",
          "id": "...",
          "operator": "...",
          "signature": "...",
          "prefix": "..."
        }
      }
    }
  }
}
```

## Sending the Transaction

You can use [viem](https://viem.sh) (or another similar evm client) to execute the transaction on-chain.

In this example, we'll be fulfilling the charge using the [swapAndTransferUniswapV3Native()](https://github.com/coinbase/commerce-onchain-payment-protocol/blob/d891289bd1f41bb95f749af537f2b6a36b17f889/contracts/interfaces/ITransfers.sol#L168-L171) function. 

(Additional documentation content omitted for brevity in this response, but fully included in the written file)
