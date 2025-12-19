# LangChain

> Using OpenRouter with LangChain

LangChain provides a standard interface for working with chat models. You can use OpenRouter with LangChain by setting the `base_url` parameter to point to OpenRouter’s API.

## Resources
- Using [LangChain for Python](https://github.com/langchain-ai/langchain)
- Using [LangChain.js](https://github.com/langchain-ai/langchainjs)

## Example Usage (TypeScript)

```typescript
import { ChatOpenAI } from "@langchain/openai";
import { HumanMessage, SystemMessage } from "@langchain/core/messages";

const chat = new ChatOpenAI(
  {
    model: '<model_name>',
    temperature: 0.8,
    streaming: true,
    apiKey: '<YOUR_API_KEY>',
  },
  {
    baseURL: 'https://openrouter.ai/api/v1',
    defaultHeaders: {
      'HTTP-Referer': '<YOUR_SITE_URL>',
      'X-Title': '<YOUR_SITE_NAME>',
    },
  },
);

const response = await chat.invoke([
  new SystemMessage("You are a helpful assistant."),
  new HumanMessage("Hello, how are you?"),
]);
```

## Example Usage (Python)

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model_name="anthropic/claude-3-sonnet",
    openai_api_key="<YOUR_API_KEY>",
    openai_api_base="https://openrouter.ai/api/v1",
    extra_headers={
        "HTTP-Referer": "<YOUR_SITE_URL>",
        "X-Title": "<YOUR_SITE_NAME>",
    }
)
```
