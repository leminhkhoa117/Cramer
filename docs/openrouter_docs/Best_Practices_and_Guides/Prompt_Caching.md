# Prompt Caching

> Reduce your AI model costs with OpenRouter's prompt caching feature. Learn how to cache and reuse responses across OpenAI, Anthropic Claude, and DeepSeek models.

To save on inference costs, you can enable prompt caching on supported providers and models.

Most providers automatically enable prompt caching, but note that some (see Anthropic below) require you to enable it on a per-message basis.

When using caching, OpenRouter will make a best-effort to continue routing to the same provider to make use of the warm cache. 

## Inspecting cache usage

To see how much caching saved on each generation, you can:

1. Click the detail button on the Activity page
2. Use the `/api/v1/generation` API
3. Use `usage: {include: true}` in your request

## OpenAI

Prompt caching with OpenAI is automated and does not require any additional configuration. There is a minimum prompt size of 1024 tokens.

## Anthropic Claude

Prompt caching with Anthropic requires the use of `cache_control` breakpoints. There is a limit of four breakpoints, and the cache will expire within five minutes. 

System message caching example:

```json
{
  "messages": [
    {
      "role": "system",
      "content": [
        {
          "type": "text",
          "text": "You are a historian..."
        },
        {
          "type": "text",
          "text": "HUGE TEXT BODY",
          "cache_control": {
            "type": "ephemeral"
          }
        }
      ]
    }
  ]
}
```
