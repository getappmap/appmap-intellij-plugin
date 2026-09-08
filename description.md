> ### Enterprise users
>
> If your company installs AppMap for you, some settings are already set by an administrator. Before you install AppMap or change a setting, ask your AppMap administrator, or read your company's own setup documentation, for example in Confluence.

# Runtime evidence for AI-assisted development

### See how every change behaves before it merges, in your JetBrains IDE

AI tools generate code changes faster than anyone can read them. A diff shows what the code says, not which calls, queries, and side effects occurred when it ran. AppMap records your application while it runs and turns the recording into diagrams for you and data your coding agent queries over MCP. The review is based on what the code did.

<img src="https://raw.githubusercontent.com/getappmap/appmap-intellij-plugin/main/assets/gold-traces-workflow.png" alt="Coding agents work the branches. The Gold Traces live on main. Every change is compared against the baseline, and the baseline advances after the merge." width="420">

## Key benefits of this plugin

### The plugin stores and configures AppMap on your machine

The plugin installs the AppMap command-line tools, keeps them updated, and stores and indexes every recording locally, so the MCP server always has fresh data to serve. You can use AppMap entirely through your coding agent: record by running your tests, then ask questions from your chat. The AppMap skills for coding agents configure your repository:

-   [`appmap-record`](https://github.com/getappmap/skills/tree/main/appmap-record) turns on recording.
-   [`appmap-label`](https://github.com/getappmap/skills/tree/main/appmap-label) tunes what gets recorded.
-   [`appmap-gold-traces`](https://github.com/getappmap/skills/tree/main/appmap-gold-traces) keeps the Gold Trace set.
-   [`appmap-review`](https://github.com/getappmap/skills/tree/main/appmap-review) runs the behavioral review.

### Store behavior with the code

Gold Traces are the runtime behaviors a team has approved, committed in the `gold_traces/` directory alongside the code. A developer or coding agent opening the repository starts with the same Gold Traces: behavior that cannot be inferred from the source, current at every commit, and the baseline every change is verified against. Every trace is sanitized before it is committed. See [Gold Traces on appmap.io](https://appmap.io/architecture).

![Record locally, commit the key traces, agents query over MCP, compare at review, and the baseline advances after the merge](https://raw.githubusercontent.com/getappmap/appmap-intellij-plugin/main/assets/gold-traces-lifecycle.png)

Found a `gold_traces/` directory in a repository? Someone on the team keeps runtime behavior versioned with the code. This plugin reads those traces as diagrams, and your coding agent can query them over MCP.

## The value of AppMap data to AI coding

### Understand AI-generated code before you ship it

An AI assistant can change hundreds of lines in one pull request. AppMap shows you the behavior of the change as diagrams: which functions ran, which SQL queries were made, which HTTP requests were handled, and where exceptions came from. Your coding agent reads the same evidence over MCP. It often has no application environment, database, or credentials, so it works from the recorded behavior, which it can query but does not create. AppMap works with Claude Code, Cursor, GitHub Copilot, Windsurf, and any MCP-capable coding agent.

![Dependency map of a running application: services, code, and SQL, and how they connect](https://raw.githubusercontent.com/getappmap/appmap-intellij-plugin/main/assets/dependency-map.webp)

### Review code changes in a new way

Ask your coding agent for a behavioral review: a review of the change that uses AppMap recordings, not just the diff. It compares the change against the recorded baseline and reports what the change did when it ran: API changes and drift, SQL impact, security-affecting paths, unexpected side effects, and performance changes.

![A behavioral review of the working tree against the baseline: one medium finding, fixed during review, one trace changed](https://raw.githubusercontent.com/getappmap/appmap-intellij-plugin/main/assets/behavioral-review-card.png)

*A real review of one change, from one of our own production applications. The fix was applied and re-recorded during the review.*

### Nothing leaves your machine, or your repository

AppMap has no cloud data plane. Recording and the MCP server run in your development environment, and AppMap data is saved as files in your project. Gold Traces go only where your repository goes. Usage telemetry is separate and can be routed to your own systems or disabled. See the [security disclosure](https://appmap.io/security).

## Set up your coding agent

The AppMap MCP server gives your agent 13 read-only query tools, including `get_call_tree`, `find_calls`, `find_queries`, and `find_requests`. In Claude Code, run `claude mcp add appmap -- appmap query mcp`. For another agent, add `"appmap": { "command": "appmap", "args": ["query", "mcp"] }` to its MCP servers configuration. See the [AppMap MCP server reference](https://appmap.io/docs/reference/appmap-mcp.html). AppMap also includes its own chat, Navie, which answers questions from the same evidence without leaving the IDE ([Navie command reference](https://appmap.io/docs/using-navie-ai/navie-commands.html)).

## Requirements and Use

**2025.1** and newer JetBrains IDEs are required to use this plugin.

AppMap works best\* with the following:

-   **Languages:** Java, Kotlin, Python, Ruby, and Node.js (TypeScript and JavaScript). In alpha: .NET, React, Swift, and Go.
-   **Frameworks:** Spring, Django, Flask, Ruby on Rails, Nest.js, Next.js, and Express.

Looking for support for your language or stack? New languages appear first on [our GitHub](https://github.com/getappmap).

Refer to the [AppMap documentation](https://appmap.io/docs/appmap-docs.html) for the latest information on supported languages, frameworks, and versions.

[*] AppMap trace recording requires a language-specific library.

## Get Started

1. **Install [the AppMap Plugin](https://plugins.jetbrains.com/plugin/16701-appmap)** from within the code editor or from the marketplace.

2. **Sign in** using an email address to obtain a license key and activate AppMap. It's best to use your work email, so that your license can be easily associated with your organization subscription.

3. **Record your app** by running your tests, or use the "Start with AppMap" menu item ([docs](https://appmap.io/docs/get-started-with-appmap/making-appmap-data.html)).

4. **Connect your coding agent** using the setup above.

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE) | [Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

To learn more about the security of AppMap, and how your data is used, see the AppMap [security disclosure](https://appmap.io/security).

There is [no fee](https://appmap.io/pricing) for personal use of AppMap. Pricing for premium features and integrations is listed on [AppMap's Pricing Page](https://appmap.io/pricing).
