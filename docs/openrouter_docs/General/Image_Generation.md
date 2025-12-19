# Image Generation

> Generate images using AI models through the OpenRouter API.

OpenRouter supports image generation through models that have `"image"` in their `output_modalities`. These models can create images from text prompts when you specify the appropriate modalities in your request.

## Model Discovery

You can find image generation models in several ways:

### On the Models Page

Visit the [Models page](/models) and filter by output modalities to find models capable of image generation. Look for models that list `"image"` in their output modalities.

### In the Chatroom

When using the [Chatroom](/chat), click the **Image** button to automatically filter and select models with image generation capabilities. If no image-capable model is active, you'll be prompted to add one.

## API Usage

To generate images, send a request to the `/api/v1/chat/completions` endpoint with the `modalities` parameter set to include both `"image"` and `"text"`.

### Basic Image Generation

<CodeGroup>
    ```typescript title="TypeScript SDK"
    import { OpenRouter } from '@openrouter/sdk';

    const openRouter = new OpenRouter({
      apiKey: '<OPENROUTER_API_KEY>',
    });

    const result = await openRouter.chat.send({
      model: 'google/gemini-2.5-flash-image-preview',
      messages: [
        {
          role: 'user',
          content: 'Generate a beautiful sunset over mountains',
        },
      ],
      modalities: ['image', 'text'],
      stream: false,
    });

    // The generated image will be in the assistant message
    if (result.choices) {
      const message = result.choices[0].message;
      if (message.images) {
        message.images.forEach((image, index) => {
          const imageUrl = image.imageUrl.url; // Base64 data URL
          console.log(`Generated image ${index + 1}: ${imageUrl.substring(0, 50)}...`);
        });
      }
    }
    ```

    ```python
    import requests
    import json

    url = "https://openrouter.ai/api/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer <OPENROUTER_API_KEY>",
        "Content-Type": "application/json"
    }

    payload = {
        "model": "google/gemini-2.5-flash-image-preview",
        "messages": [
            {
                "role": "user",
                "content": "Generate a beautiful sunset over mountains"
            }
        ],
        "modalities": ["image", "text"]
    }

    response = requests.post(url, headers=headers, json=payload)
    result = response.json()
    ```
</CodeGroup>

## Response Format

When generating images, the assistant message includes an `images` field containing the generated images:

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "I've generated a beautiful sunset image for you.",
        "images": [
          {
            "type": "image_url",
            "image_url": {
              "url": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
            }
          }
        ]
      }
    }
  ]
}
```
