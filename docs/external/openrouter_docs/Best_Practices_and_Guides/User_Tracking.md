# User Tracking

> Learn how to use the user parameter to track your own user IDs with OpenRouter. Improve caching performance and get detailed reporting on your sub-users.

The OpenRouter API supports **User Tracking** through the optional `user` parameter, allowing you to track your own user IDs and improve your application's performance and reporting capabilities.

## What is User Tracking?

User tracking enables you to specify an arbitrary string identifier for your end-users in API requests. This optional metadata helps OpenRouter understand your sub-users, leading to several benefits:

1. **Improved Caching**: OpenRouter can make caches sticky to your individual users, improving load-balancing and throughput
2. **Enhanced Reporting**: View detailed analytics and activity feeds broken down by your user IDs

## How It Works

Simply include a `user` parameter in your API requests with any string identifier that represents your end-user. This could be a user ID, email hash, session identifier, or any other stable identifier you use in your application.

```json
{
  "model": "openai/gpt-4o",
  "messages": [
    {"role": "user", "content": "Hello, how are you?"}
  ],
  "user": "user_12345"
}
```

## Benefits

### Improved Caching Performance

When you consistently use the same user identifier for a specific user, OpenRouter can optimize caching to be "sticky" to that user. 

### Enhanced Reporting and Analytics

The user parameter is available in the /activity page, in the exports from that page, and in the /generations API.

* **Activity Feed**: View requests broken down by user ID
* **Usage Analytics**: Understand which users are making the most requests
* **Export Data**: Get detailed exports that include user-level breakdowns
