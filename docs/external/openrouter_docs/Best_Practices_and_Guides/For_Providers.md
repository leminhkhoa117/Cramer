# Provider Integration

> Learn how to integrate your AI models with OpenRouter. Complete guide for providers to make their models available through OpenRouter's unified API.

## For Providers

If you'd like to be a model provider and sell inference on OpenRouter, fill out our form to get started.

To be eligible to provide inference on OpenRouter you must have the following:

### 1. List Models Endpoint

You must implement an endpoint that returns all models that should be served by OpenRouter. 

```json
{
  "data": [
    {
      "id": "anthropic/claude-sonnet-4",
      "name": "Anthropic: Claude Sonnet 4",
      "created": 1690502400,
      "input_modalities": ["text", "image", "file"],
      "output_modalities": ["text", "image", "file"],
      "pricing": {
        "prompt": "0.000008",
        "completion": "0.000024"
      }
    }
  ]
}
```

### 2. Auto Top Up or Invoicing

### 3. Uptime Monitoring & Traffic Routing

OpenRouter automatically monitors provider reliability and adjusts traffic routing based on uptime metrics. 
* **Normal routing**: 95%+ uptime
* **Degraded status**: 80-94% uptime
* **Down status**: <80% uptime
