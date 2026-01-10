# Using MCP Servers with OpenRouter

> Learn how to use MCP Servers with OpenRouter

MCP servers are a popular way of providing LLMs with tool calling abilities, and are an alternative to using OpenAI-compatible tool calling.

By converting MCP (Anthropic) tool definitions to OpenAI-compatible tool definitions, you can use MCP servers with OpenRouter.

In this example, we'll use Anthropic's MCP client SDK to interact with the File System MCP, all with OpenRouter under the hood.

<Warning>
  Note that interacting with MCP servers is more complex than calling a REST
  endpoint. The MCP protocol is stateful and requires session management.
</Warning>
