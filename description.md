# Runtime‑aware AI starts here

### Live code behavior, surfaced to your AI tools in your JetBrains IDE

AppMap Navie for JetBrains brings the power of real-time execution data and AI-driven insights right to your code editor. No more guessing what your code does under the hood. Navie watches your application run and uses that live context to provide **smarter suggestions**, **faster debugging**, and **runtime-aware code reviews**.

## Key Benefits

### Smarter AI assistance

Navie combines static analysis with live AppMap traces, so you can ask things like _"What just happened?"_ and get answers based on the actual runtime flow, HTTP calls, SQL queries, exceptions, I/O, and more.

### Faster debugging & fewer defects

Pinpoint performance bottlenecks and logic errors through automatically generated sequence diagrams, flame graphs, dependency maps, and trace views.

### Context‑aware code reviews

From security checks to maintainability recommendations, Navie’s `@review` mode analyzes your current branch changes against your base branch with runtime insights.

### Zero fine‑tuning required

Works out-of-the-box with enterprise‑ready LLMs—simply plug in your API key or let Navie default to GitHub Copilot or AppMap’s built‑in endpoint.

## What AppMap Does

-   Captures real‑time snapshots of code execution, data flow, and behavior with zero effort and no code changes.

-   Feeds runtime context to AI assistants like Navie, GitHub Copilot, Anthropic Claude, Google Gemini, OpenAI, and your own local LLMs.

-   Delivers deep code explanations, diagrams, implementation plans, tests, and patch-ready code snippets, all grounded in what your application just did.

## Requirements and Use

**2023.1** and newer JetBrains IDEs are required to use this plugin.

AppMap works best\* with the following:

-   **Languages:** Java, Kotlin, Python, Ruby, and Node.js (TypeScript and JavaScript).
-   **Frameworks:** Spring, Django, Flask, Ruby on Rails, Nest.js, Next.js, and Express.

Refer to AppMap documentation for the latest information on supported languages, frameworks, and versions.

[*] AppMap Navie is designed to work with a wide range of languages and frameworks, but AppMap trace recording requires a language-specific library.

## Get Started

1. **Install [the AppMap Plugin](https://plugins.jetbrains.com/plugin/16701-appmap)** from within the code editor or
   from the marketplace.

2. **Sign in** using an email address to obtain a license key and activate AppMap. It's best to use your work email, so
   that your license can be easily associated with your organization subscription.

3. **Ask Navie** for assistance with the project you're working on. Use the `@help` command to get help with AppMap and
   Navie itself.

## Examples

[Here are some examples](https://appmap.io/product/examples/navie) of Navie providing code explanations, diagrams,
specifications, documentation,
and reasoning about a broad context of mature application code.

## Command Modes

Navie provides different command modes to assist you with your code and project. Here's a quick overview:

-   **`@explain` (default)**: Provides context-aware suggestions, specific solutions, and reasons about the
    larger context of the specific code being worked on.

-   **`@diagram`**: Creates and renders Mermaid compatible diagrams within the Navie UI. You can open
    a diagram in the [Mermaid Live Editor](https://mermaid.live), copy the Mermaid Definitions to your clipboard, save
    to disk, or expand a full window view.

-   **`@plan`**: Builds a detailed implementation plan for an issue or task. This will focus Navie on only understanding
    the problem and the application to generate a step-by-step plan.

-   **`@generate`**: In this mode, responses are optimized to include code snippets and patches that you can apply
    directly to the files you're working on.

-   **`@test`**: Provides output optimized for creating and updating test cases. Supports both unit tests and integration
    tests. Test updates will confirm to the existing structure and patterns of your test cases, and will provide updated
    tests based on features or code that is provided.

-   **`@search`**: Leverages smart search capabilities to locate specific functions, files, modules, and examples.

-   **`@review`**: Performs a review of the code changes on your current branch against the base branch. Provides
    actionable insights on important aspects of the code, ensuring alignment with best practices in areas such as code
    quality, security, and maintainability.

-   **`@help`**: Offers assistance with using AppMap and Navie, including guidance for generating and leveraging AppMap
    data effectively.

## Activating Navie

**Activate Navie from the Tools menu**  
Select 'Explain with AppMap Navie AI' from the Tools/AppMap dropdown

![Open Navie from Tools](https://appmap.io/assets/img/product/tools-appmap-vscode.png)

**Activate Navie from the AppMap sidebar**  
Click the 'New Navie Chat' button

![New Navie Chat](https://appmap.io/assets/img/product/new-navie-chat.png)

## Add Files to a Navie chat

You can add specific files to your conversation with Navie. This feature is referred to
as "pinning". Some types of files you may want to pin include:

-   Issue description
-   Custom prompts
-   Code files
-   Navie chat responses from previous conversations

**Pin files from the file list**  
Right-click on a file to select 'AppMap: Add Files to Navie Context'

![Add context from file](https://appmap.io/assets/img/product/add-context-from-file.png)

**Pin files from a Navie response**  
Click the pin icon in the header of a Navie response

![Pin from Response](https://appmap.io/assets/img/pin-from-response.png)

**Pin files from the Navie's Context Sources Pane**  
Click the 'Add Context' button

![Add context from the context window](https://appmap.io/assets/img/product/add-context-in-context-window.png)

## Choosing your LLM for Navie

By default, if you have the GitHub Copilot plugin installed, and you have an active Copilot subscription, Navie will use
the GitHub Copilot LLM. Otherwise, Navie will default to an OpenAI LLM endpoint provided by AppMap.

You can also configure Navie to use your own LLM API key, or to use a local model. Refer to
the [AppMap Navie documentation](https://appmap.io/docs/navie/bring-your-own-model.html) for details on how to do that.

## Recording AppMap Data

AppMap data brings two benefits to your development workflow:

1. **Runtime diagrams:** You can directly use the AppMap diagrams to understand the runtime behavior of your
   application.
2. **Improved context for Navie AI:** Navie will leverage available AppMap data to provide more accurate and relevant
   answers.

You record AppMap data by running your app — either
by [running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or
by [recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

Navie can assistance with making AppMap data. Use the `@help` command for this.

In IntelliJ, you can record AppMap data using the "Start with AppMap" menu item. [Visit the documentation](https://appmap.io/docs/get-started-with-appmap/making-appmap-data.html) for other languages or more details.

![Start with AppMap](https://appmap.io/assets/img/product/start-with-appmap.png)

#### Requirements for Making AppMap Data

Supported programming languages: Node.js, Java (+ Kotlin), Ruby, and Python.
AppMap works particularly well with web application frameworks such as: Nest.js, Next.js, Spring, Ruby on Rails, Django,
and Flask.

## Using AppMap Data

AppMap diagrams include:

-   **Sequence Diagram** to follow the runtime flow of calls made by your application.
-   **Dependency Map** to see which libraries and frameworks were used at runtime.
-   **Flame Graph** to spot performance issues and bottlenecks.
-   **Trace View** to perform detailed function call and data flow tracing.

Using AppMap data, Navie gains deeper knowledge of runtime aspects of code behavior, such as:

-   HTTP requests and responses
-   SQL queries
-   Timing data
-   Exceptions
-   I/O operations

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE) | [Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

To learn more about security of AppMap, or the use of data with AI when using Navie, see the
AppMap [security disclosure](https://appmap.io/security) for more detailed information and discussion.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap for graphing and limited Navie use. Pricing for
premium features and integrations are listed on [AppMap’s Pricing Page](https://appmap.io/pricing).
