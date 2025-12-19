# App Attribution

> Learn how to attribute your API usage to your app and appear in OpenRouter's app rankings and model analytics.

App attribution allows developers to associate their API usage with their application, enabling visibility in OpenRouter's public rankings and detailed analytics. By including simple headers in your requests, your app can appear in our leaderboards and gain insights into your model usage patterns.

## Attribution Headers

OpenRouter tracks app attribution through two optional HTTP headers:

### HTTP-Referer
The `HTTP-Referer` header identifies your app's URL and is used as the primary identifier for rankings.

### X-Title
The `X-Title` header sets or modifies your app's display name in rankings and analytics.

## Implementation Examples

<CodeGroup>
  ```typescript title="TypeScript SDK"
  import { OpenRouter } from '@openrouter/sdk';

  const openRouter = new OpenRouter({
    apiKey: '<OPENROUTER_API_KEY>',
    defaultHeaders: {
      'HTTP-Referer': 'https://myapp.com', // Your app's URL
      'X-Title': 'My AI Assistant', // Your app's display name
    },
  });

  const completion = await openRouter.chat.send({
    model: 'openai/gpt-4o',
    messages: [{ role: 'user', content: 'Hello, world!' }],
  });
  ```
</CodeGroup>

## Where Your App Appears

### App Rankings
Your attributed app will appear in OpenRouter's main rankings page at [openrouter.ai/rankings](https://openrouter.ai/rankings).

### Individual App Analytics
Once your app is tracked, you can access detailed analytics at `openrouter.ai/apps?url=<your-app-url>`.
