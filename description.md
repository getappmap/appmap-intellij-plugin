# AppMap for JetBrains

## Runtime-aware AI starts here

**Live code behavior, for your eyes and your AI tools, in your JetBrains IDE.**

AppMap records how your application actually runs, with zero code changes. Every run becomes interactive sequence diagrams, dependency maps, flame graphs, and trace views. The same runtime data is available to any AI coding agent through the Model Context Protocol (MCP). Your agent can query real execution traces instead of guessing from static code.

People see the map. Agents query the trace. One run, same ground truth.

## Why developers use it

-   **A visual check on your AI's work.** On complex changes the diff outgrows what anyone can hold in their head. AppMap shows what the change actually did at runtime, so your approval rests on evidence, not on the AI's own explanation.
-   **Debug in one query, not fifteen greps.** The request path, the SQL, and the exception are one trace away, for you and your agent.
-   **Give your agent ground truth.** Answers from real execution over MCP, not guesses from static code.

## Works with any AI coding agent via MCP

AppMap includes an MCP server that exposes your recorded runtime data as read-only query tools. Any MCP-capable coding agent can connect and ask how your application actually ran. Which endpoints are slow. Where time is spent. What SQL was issued. What exceptions occurred. How a single request executed end to end.

The plugin keeps the query index up to date automatically as you record. Point your agent at the server and start asking questions.

Agents can rank function and SQL hotspots, inspect call trees with captured parameters and return values, find related recordings, and compare per-route latency between git branches.

Get started:

-   [Configure the AppMap MCP server for any AI coding agent](https://appmap.io/docs/reference/appmap-mcp.html)
-   [AppMap for JetBrains reference](https://appmap.io/docs/reference/jetbrains.html)
-   [Record AppMap Data](https://appmap.io/docs/get-started-with-appmap/making-appmap-data.html)

## See what your code actually does

-   **Sequence diagrams** show the full request path, from HTTP to database, from one recording.
-   **Dependency maps** reveal the running architecture: services, code, SQL, and how they connect.
-   **Flame graphs and trace views** pinpoint performance bottlenecks and logic errors.
-   **Zero effort capture.** AppMap records code execution, data flow, HTTP, SQL, and exceptions automatically as your tests or your app run. No code changes.

## What AppMap does

-   Captures real-time snapshots of code execution, data flow, and behavior with zero effort and no code changes.
-   Renders that behavior as interactive diagrams people can inspect in the IDE.
-   Feeds the same runtime context to any MCP-capable AI assistant, hosted or local.
-   Grounds explanations, reviews, and fixes in what your application just did, not in guesses from static code.

## Get started

1. **Install the AppMap plugin** from the JetBrains Marketplace.
2. **Sign in with an email address, or with GitHub or GitLab.** Guided setup configures the recording agent for your project (Java, Python, Ruby, Node.js).
3. **Run your tests or exercise your app.** AppMap Data is recorded automatically, diagrams appear in the IDE, and the query index stays fresh for your AI agent.
4. **Connect your agent** using the [MCP configuration guide](https://appmap.io/docs/reference/appmap-mcp.html).

## Licensing and Security

Open source | [Terms and conditions](https://appmap.io/community/terms-and-conditions.html) | [Security disclosure](https://appmap.io/security)

This extension is for individual developers working in the code editor. Check out the [pricing page](https://appmap.io/pricing).
