# Auto Router

> Automatically select the best AI model for your prompts using OpenRouter's Auto Router powered by NotDiamond.

The [Auto Router](https://openrouter.ai/openrouter/auto) (`openrouter/auto`) automatically selects the best model for your prompt, powered by [NotDiamond](https://www.notdiamond.ai/).

## Overview

Instead of manually choosing a model, let the Auto Router analyze your prompt and select the optimal model from a curated set of high-quality options. The router considers factors like prompt complexity, task type, and model capabilities.

## Usage

Set your model to `openrouter/auto`:

<CodeGroup>
  ```typescript title="TypeScript SDK"
  import { OpenRouter } from '@openrouter/sdk';

  const openRouter = new OpenRouter({
    apiKey: '<OPENROUTER_API_KEY>',
  });

  const completion = await openRouter.chat.send({
    model: 'openrouter/auto',
    messages: [
      {
        role: 'user',
        content: 'Explain quantum entanglement in simple terms',
      },
    ],
  });

  console.log(completion.choices[0].message.content);
  // Check which model was selected
  console.log('Model used:', completion.model);
  ```
</CodeGroup>

## Response

The response includes the `model` field showing which model was actually used:

```json
{
  "id": "gen-...",
  "model": "anthropic/claude-sonnet-4.5",  // The model that was selected
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "..."
      }
    }
  ],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 150,
    "total_tokens": 165
  }
}
```

## How It Works

1. **Prompt Analysis**: Your prompt is analyzed by NotDiamond's routing system
2. **Model Selection**: The optimal model is selected based on the task requirements
3. **Request Forwarding**: Your request is forwarded to the selected model
4. **Response Tracking**: The response includes metadata showing which model was used

## Supported Models

The Auto Router selects from a curated set of high-quality models including:
* Claude Sonnet 4.5 (`anthropic/claude-sonnet-4.5`)
* GPT-5.1 (`openai/gpt-5.1`)
* Gemini 3 Pro (`google/gemini-3-pro-preview`)
* DeepSeek 3.2 (`deepseek/deepseek-v3.2`)

## Pricing

You pay the standard rate for whichever model is selected. There is no additional fee for using the Auto Router.
