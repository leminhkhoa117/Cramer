# Organization Management

> Learn how to create and manage organizations on OpenRouter for team collaboration, shared credits, and centralized API management.

OpenRouter organizations enable teams and companies to collaborate effectively by sharing credits, managing API keys centrally, and tracking usage across all team members. Organizations are ideal for companies that want to pool resources, manage inference costs centrally, and maintain oversight of AI usage across their team.

## Getting Started with Organizations

### Creating an Organization

To create an organization:

1. Navigate to [Settings > Preferences](https://openrouter.ai/settings/preferences)
2. In the Organization section, click **Create Organization**
3. Follow the setup process to configure your organization details
4. Invite team members to join your organization

<Tip>
  You must have a verified email address to create an organization.
</Tip>

### Switching Between Personal and Organization Accounts

Once you're part of an organization, you can easily switch between your personal account and organization context:

* Use the **organization switcher** at the top of the web application
* When in organization mode, all actions (API usage, credit purchases, key management) are performed on behalf of the organization
* When in personal mode, you're working with your individual account resources

## Credit Management

### Shared Credit Pool

Organizations maintain a shared credit pool that offers several advantages:

* **Centralized Billing**: All credits purchased in the organization account can be used by any organization member
* **Simplified Accounting**: Track all AI inference costs in one place
* **Budget Control**: Administrators can manage spending and monitor usage across the entire team

### Admin-Only Credit Management

Only organization administrators can:

* Purchase credits for the organization
* View detailed billing information
* Manage payment methods and invoicing settings

## API Key Management

Organizations provide flexible API key management with role-based permissions:

### Member Permissions

* **Create API Keys**: All organization members can create API keys
* **View Own Keys**: Members can only view and manage API keys they created
* **Use Organization Keys**: Keys created by any organization member can be used by all members
* **Shared Usage**: API usage from any organization key is billed to the organization's credit pool

### Administrator Permissions

* **View All Keys**: Administrators can view all API keys created within the organization
* **Manage All Keys**: Full access to edit, disable, or delete any organization API key
* **Monitor Usage**: Access to detailed usage analytics for all organization keys

## Activity and Usage Tracking

### Organization-Wide Activity Feed

When viewing your activity feed while in organization context, you'll see:

* **All Member Activity**: Usage data from all organization members appears in the activity feed
* **Metadata Only**: Activity shows model usage, costs, and request metadata
* **Key Filtering**: Activity can be filtered by a specific API key to view usage for that key only

## Administrative Controls

### Admin-Only Settings

Organization administrators have exclusive access to:

* **Provider Settings**: Configure preferred model providers and routing preferences
* **Privacy Settings**: Manage data retention and privacy policies for the organization
* **Member Management**: Add, remove, and manage member roles
* **Billing Configuration**: Set up invoicing, payment methods, and billing contacts

## Use Cases and Benefits

### For Development Teams

* **Shared Resources**: Pool credits across multiple developers and projects
* **Centralized Management**: Manage all API keys and usage from a single dashboard
* **Cost Tracking**: Monitor spending per project or team member

### For Companies

* **Budget Control**: Administrators control spending and resource allocation
* **Compliance**: Centralized logging and usage tracking for audit purposes
* **Scalability**: Easy to add new team members and projects
* **Cost Optimization**: Identify usage patterns and optimize model selection

## Frequently Asked Questions

<AccordionGroup>
  <Accordion title="Can I convert my personal account to an organization?">
    No, organizations are separate entities. You'll need to create a new organization and transfer resources as needed.
  </Accordion>

  <Accordion title="How many members can an organization have?">
    An organization can only have 10 members. Contact support if you need more.
  </Accordion>

  <Accordion title="Can organization members see each other's usage data?">
    Organization members can see usage metadata (model used, cost, timing) for all organization activity in the activity feed. OpenRouter does not store prompts or responses.
  </Accordion>
</AccordionGroup>
