# Message Transforms

> Transform and optimize messages before sending them to AI models. Learn about middle-out compression and context window optimization with OpenRouter.

To help with prompts that exceed the maximum context size of a model, OpenRouter supports a custom parameter called `transforms`:

```typescript
{
  transforms: ["middle-out"], // Compress prompts that are > context size.
  messages: [...],
  model // Works with any model
}
```

This can be useful for situations where perfect recall is not required. The transform works by removing or truncating messages from the middle of the prompt, until the prompt fits within the model's context window.

When middle-out compression is enabled, OpenRouter will first try to find models whose context length is at least half of your total required tokens (input + completion). 

<Note>
  All OpenRouter endpoints with 8k (8,192 tokens) or less context
  length will default to using `middle-out`. To disable this, set `transforms: []` in the request body.
</Note>

The middle of the prompt is compressed because LLMs pay less attention to the middle of sequences.
