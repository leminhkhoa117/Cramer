# Models & Pricing

The prices listed below are in units of per 1M tokens. A token, the smallest unit of text that the model recognizes, can be a word, a number, or even a punctuation mark. We will bill based on the total number of input and output tokens by the model.

## Model Details

| Feature | DeepSeek-V3.2<br>(Non-thinking Mode) | DeepSeek-V3.2<br>(Thinking Mode) | DeepSeek-V3.2-Speciale<br>(Thinking Mode Only) |
| :--- | :--- | :--- | :--- |
| **Model ID** | `deepseek-chat` | `deepseek-reasoner` | `deepseek-reasoner` |
| **Base URL** | `https://api.deepseek.com` | `https://api.deepseek.com` | `https://api.deepseek.com/v3.2_speciale_expires_on_20251215` |
| **Context Length** | 128K | 128K | 128K |
| **Max Output** | **Default:** 4K<br>**Maximum:** 8K | **Default:** 32K<br>**Maximum:** 64K | **Default:** 128K<br>**Maximum:** 128K |
| **Json Output** | ✓ | ✓ | ✗ |
| **Tool Calls** | ✓ | ✓ | ✗ |
| **Chat Prefix** | ✓ | ✓ | ✗ |
| **FIM Completion** | ✓ | ✗ | ✗ |

## Pricing (Per 1M Tokens)

| Type | Price |
| :--- | :--- |
| **Input Tokens (Cache Hit)** | $0.028 |
| **Input Tokens (Cache Miss)** | $0.28 |
| **Output Tokens** | $0.42 |

> **Note on Speciale Model:** Users can access the `DeepSeek-V3.2-Speciale` model by setting the base_url to `https://api.deepseek.com/v3.2_speciale_expires_on_20251215`. This model only supports thinking mode and will be available until **December 15, 2025, 15:59 UTC**.

---

## Deduction Rules

* **Calculation:** The expense = number of tokens × price.
* **Billing:** The corresponding fees will be directly deducted from your topped-up balance or granted balance, with a preference for using the granted balance first when both balances are available.
* **Disclaimer:** Product prices may vary and DeepSeek reserves the right to adjust them. We recommend topping up based on your actual usage and regularly checking this page for the most recent pricing information.