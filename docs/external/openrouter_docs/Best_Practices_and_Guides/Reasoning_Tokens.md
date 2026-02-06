# Reasoning Tokens

> Learn how to use reasoning tokens to enhance AI model outputs. Implement step-by-step reasoning traces for better decision making and transparency.

For models that support it, the OpenRouter API can return **Reasoning Tokens**, also known as thinking tokens. OpenRouter normalizes the different ways of customizing the amount of reasoning tokens that the model will use, providing a unified interface across different providers.

Reasoning tokens are included in the response by default if the model decides to output them. Reasoning tokens will appear in the `reasoning` field of each message.

## Controlling Reasoning Tokens

You can control reasoning tokens in your requests using the `reasoning` parameter:

```json
{
  "model": "your-model",
  "messages": [],
  "reasoning": {
    "effort": "high",
    "max_tokens": 2000,
    "exclude": false,
    "enabled": true
  }
}
```

### Reasoning Effort Level

* `"effort": "xhigh"` - Approximately 95% of max\_tokens
* `"effort": "high"` - Approximately 80% of max\_tokens
* `"effort": "medium"` - Approximately 50% of max\_tokens
* `"effort": "low"` - Approximately 20% of max\_tokens
* `"effort": "minimal"` - Approximately 10% of max\_tokens
* `"effort": "none"` - Disables reasoning entirely
