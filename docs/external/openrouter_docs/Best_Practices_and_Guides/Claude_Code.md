# Claude Code

> Learn how to use Claude Code with OpenRouter to access various models.

<Warning>
  Claude Code is a powerful agentic tool. While you can use any model via OpenRouter, we recommend sticking to highly capable models (like Claude 4.5 Sonnet, GPT 5.2, etc.) for the best experience, as complex coding tasks require strong reasoning.
</Warning>

## Quick Start

This guide will get you running [Claude Code](https://code.claude.com/docs/en/overview) powered by OpenRouter in just a few minutes.

### Step 1: Install Claude Code

**macOS, Linux, WSL:**
```bash
curl -fsSL https://claude.ai/install.sh | bash
```

**Windows PowerShell:**
```powershell
irm https://claude.ai/install.ps1 | iex
```

### Step 2: Connect Claude to OpenRouter

Instead of logging in with Anthropic directly, connect Claude Code to OpenRouter.
This requires setting a few environment variables.

Requirements:
1. Use `https://openrouter.ai/api` for the base url
2. Provide your OpenRouter API key as the auth token
3. **Important:** Explicitly blank out the Anthropic API key to prevent conflicts

```bash
# Set these in your shell (e.g., ~/.bashrc, ~/.zshrc)
export ANTHROPIC_BASE_URL="https://openrouter.ai/api"
export ANTHROPIC_AUTH_TOKEN="$OPENROUTER_API_KEY"
export ANTHROPIC_API_KEY="" # Important: Must be explicitly empty
```

### Step 3: Start your session

Navigate to your project directory and start Claude Code:
```bash
cd /path/to/your/project
claude
```

### Step 4: Verify

You can confirm your connection by running the `/status` command inside Claude Code.

```text
> /status
Auth token: ANTHROPIC_AUTH_TOKEN
Anthropic base URL: https://openrouter.ai/api
```
