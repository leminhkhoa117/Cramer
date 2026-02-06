# Body Builder

> Use natural language to generate multiple OpenRouter API requests for parallel model execution.

The [Body Builder](https://openrouter.ai/openrouter/bodybuilder) (`openrouter/bodybuilder`) transforms natural language prompts into structured OpenRouter API requests, enabling you to easily run the same task across multiple models in parallel.

## Overview

Body Builder uses AI to understand your intent and generate valid OpenRouter API request bodies. Simply describe what you want to accomplish and which models you want to use, and Body Builder returns ready-to-execute JSON requests.

<Callout intent="info">
  Body Builder is **free to use**. There is no charge for generating the request bodies.
</Callout>

## Usage

<CodeGroup>
  ```typescript title="TypeScript SDK"
  import { OpenRouter } from '@openrouter/sdk';

  const openRouter = new OpenRouter({
    apiKey: '<OPENROUTER_API_KEY>',
  });

  const completion = await openRouter.chat.send({
    model: 'openrouter/bodybuilder',
    messages: [
      {
        role: 'user',
        content: 'Count to 10 using Claude Sonnet and GPT-5',
      },
    ],
  });

  // Parse the generated requests
  const generatedRequests = JSON.parse(completion.choices[0].message.content);
  console.log(generatedRequests);
  ```
</CodeGroup>

## Response Format

Body Builder returns a JSON object containing an array of OpenRouter-compatible request bodies:

```json
{
  "requests": [
    {
      "model": "anthropic/claude-sonnet-4.5",
      "messages": [
        {"role": "user", "content": "Count to 10"}
      ]
    },
    {
      "model": "openai/gpt-5.1",
      "messages": [
        {"role": "user", "content": "Count to 10"}
      ]
    }
  ]
}
```

## Executing Generated Requests

After generating the request bodies, execute them in parallel using standard Promise.all or asyncio.gather.
