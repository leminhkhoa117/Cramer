# Distillation

> Learn how to use the distillable filter to ensure compliance with provider and model creator policies when using model outputs for training or distillation.

Model distillation is the process of training a smaller, more efficient model using outputs from a larger model. While this technique is powerful for creating specialized models, it's important to respect the terms of service set by model providers and creators.

Some model providers and creators explicitly prohibit using their model outputs to train other models, while others allow it. OpenRouter makes it easy to filter for models that permit distillation, helping you stay compliant with these policies.

## Finding Distillable Models on the Model Page

The easiest way to find models that allow distillation is to use the **Distillable** filter on the [Models page](https://openrouter.ai/models?distillable=true).

1. Navigate to the [Models page with the distillable filter enabled](https://openrouter.ai/models?distillable=true)
2. The **Distillable** filter in the filter panel will be set to **Yes**, showing only models that allow distillation

## Using the Routing Parameter

For programmatic control, you can use the `enforce_distillable_text` parameter in your API requests. When set to `true`, OpenRouter will only route your request to models that allow text distillation.

| Field                      | Type    | Default | Description                                                   |
| -------------------------- | ------- | ------- | ------------------------------------------------------------- |
| `enforce_distillable_text` | boolean | -       | Restrict routing to only models that allow text distillation. |

When `enforce_distillable_text` is set to `true`, the request will only be routed to models where the author has explicitly enabled text distillation. 
