## Error Codes

When calling DeepSeek API, you may encounter errors. Here is a list of causes and solutions.

| Code | Error Type | Cause | Solution |
| :--- | :--- | :--- | :--- |
| **400** | **Invalid Format** | Invalid request body format. | Please modify your request body according to the hints in the error message. For more API format details, please refer to DeepSeek API Docs. |
| **401** | **Authentication Fails** | Authentication fails due to the wrong API key. | Please check your API key. If you don't have one, please create an API key first. |
| **402** | **Insufficient Balance** | You have run out of balance. | Please check your account's balance, and go to the Top up page to add funds. |
| **422** | **Invalid Parameters** | Your request contains invalid parameters. | Please modify your request parameters according to the hints in the error message. For more API format details, please refer to DeepSeek API Docs. |
| **429** | **Rate Limit Reached** | You are sending requests too quickly. | Please pace your requests reasonably. We also advise users to temporarily switch to the APIs of alternative LLM service providers, like OpenAI. |
| **500** | **Server Error** | Our server encounters an issue. | Please retry your request after a brief wait and contact us if the issue persists. |
| **503** | **Server Overloaded** | The server is overloaded due to high traffic. | Please retry your request after a brief wait. |