# Zero Data Retention

> Learn how OpenRouter gives you control over your data

Zero Data Retention (ZDR) means that a provider will not store your data for any period of time.

OpenRouter has a [setting](/settings/privacy) that, when enabled, only allows you to route to endpoints that have a Zero Data Retention policy.

Providers that do not retain your data are also unable to train on your data. However we do have some endpoints & providers who do not train on your data but *do* retain it.

## Per-Request ZDR Enforcement

In addition to the global ZDR setting in your [privacy settings](/settings/privacy), you can enforce Zero Data Retention on a per-request basis using the `zdr` parameter in your API calls.

The request-level `zdr` parameter operates as an "OR" with your account-wide ZDR setting - if either is enabled, ZDR enforcement will be applied. 

### Usage

Include the `zdr` parameter in your provider preferences:

```json
{
  "model": "gpt-4",
  "messages": [...],
  "provider": {
    "zdr": true
  }
}
```

## OpenRouter's Retention Policy

OpenRouter itself has a ZDR policy; your prompts are not retained unless you specifically opt in to prompt logging.
