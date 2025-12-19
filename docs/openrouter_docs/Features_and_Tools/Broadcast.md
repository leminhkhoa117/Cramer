# Broadcast

> Connect your LLM observability platforms to automatically receive traces from your OpenRouter requests. Supports Langfuse, Datadog, Braintrust, and more.

Broadcast allows you to automatically send traces from your OpenRouter requests to external observability and analytics platforms. 

## Enabling Broadcast

1. Navigate to [Settings > Broadcast](https://openrouter.ai/settings/broadcast)
2. Toggle the "Enable Broadcast" switch
3. Add one or more destinations where you want to send your traces

## Supported Destinations

The following destinations are currently available:
* [Braintrust](/docs/guides/features/broadcast/braintrust)
* [Datadog](/docs/guides/features/broadcast/datadog)
* [Langfuse](/docs/guides/features/broadcast/langfuse)
* [LangSmith](/docs/guides/features/broadcast/langsmith)
* [Weave](/docs/guides/features/broadcast/weave)

## Trace Data

Each broadcast trace includes:
* Request & Response Data
* Token Usage
* Cost Information
* Timing & Latency
* Model & Provider Information
* Tool Usage
