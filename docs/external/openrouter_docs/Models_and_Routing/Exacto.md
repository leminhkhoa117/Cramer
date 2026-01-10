# Exacto Variant

> Learn how to target OpenRouter-selected providers by using the :exacto model variant.

Introducing a new set of endpoints, `:exacto`, focused on higher tool‑calling accuracy by routing to a sub‑group of providers with measurably better tool‑use success rates. It uses the same request payloads as any other variant, but filters endpoints so that only vetted providers for the chosen model are considered. 

## Using the Exacto Variant

Add `:exacto` to the end of any supported model slug. The curated allowlist is enforced before provider sorting, fallback, or load balancing — no extra provider preference config is required.

<CodeGroup>
  ```typescript title="TypeScript SDK"
  import { OpenRouter } from '@openrouter/sdk';

  const openRouter = new OpenRouter({
    apiKey: process.env.OPENROUTER_API_KEY,
  });

  const completion = await openRouter.chat.send({
    model: "moonshotai/kimi-k2-0905:exacto",
    messages: [
      {
        role: "user",
        content: "Draft a concise changelog entry for the Exacto launch.",
      },
    ],
    stream: false,
  });

  console.log(completion.choices[0].message.content);
  ```
</CodeGroup>

## Supported Models

Exacto endpoints are available for:

* Kimi K2 (`moonshotai/kimi-k2-0905:exacto`)
* DeepSeek v3.1 Terminus (`deepseek/deepseek-v3.1-terminus:exacto`)
* GLM 4.6 (`z-ai/glm-4.6:exacto`)
* GPT‑OSS 120B (`openai/gpt-oss-120b:exacto`)
* Qwen3 Coder (`qwen/qwen3-coder:exacto`)
