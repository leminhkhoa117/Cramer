# Response Healing Plugin

> Automatically validate and repair malformed JSON responses from AI models. Ensure your responses match your schema even when models return imperfect formatting.

The Response Healing plugin automatically validates and repairs malformed JSON responses from AI models. When models return imperfect formatting – missing brackets, trailing commas, markdown wrappers, or mixed text – this plugin attempts to repair the response so you receive valid, parseable JSON.

## Overview

Response Healing provides:

* **Automatic JSON repair**: Fixes missing brackets, commas, quotes, and other syntax errors
* **Markdown extraction**: Extracts JSON from markdown code blocks

## How It Works

The plugin activates for non-streaming requests when you use `response_format` with either `type: "json_schema"` or `type: "json_object"`, and include the response-healing plugin in your `plugins` array. 
