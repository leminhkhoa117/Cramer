# Model Routing

> Route requests dynamically between AI models. Learn how to use OpenRouter's Auto Router and model fallback features for optimal performance and reliability.

OpenRouter provides two options for model routing.

## Auto Router

The [Auto Router](https://openrouter.ai/openrouter/auto), a special model ID that you can use to choose between selected high-quality models based on your prompt, powered by [NotDiamond](https://www.notdiamond.ai/).

## The `models` parameter

The `models` parameter lets you automatically try other models if the primary model's providers are down, rate-limited, or refuse to reply due to content moderation.

<CodeGroup>
  ```typescript title="TypeScript SDK"
  import { OpenRouter } from '@openrouter/sdk';

  const openRouter = new OpenRouter({
    apiKey: '<OPENROUTER_API_KEY>',
  });

  const completion = await openRouter.chat.send({
    models: ['anthropic/claude-3.5-sonnet', 'gryphe/mythomax-l2-13b'],
    messages: [
      {
        role: 'user',
        content: 'What is the meaning of life?',
      },
    ],
  });

  console.log(completion.choices[0].message.content);
  ```
</CodeGroup>

If the model you selected returns an error, OpenRouter will try to use the fallback model instead. 
By default, any error can trigger the use of a fallback model, including context length validation errors, moderation flags for filtered models, rate-limiting, and downtime.
