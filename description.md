AppMap Navie is an AI software architect that gives you deep code explanations and code reviews along with diagrams
about your code. AppMap brings runtime-awareness to your code editor and AI-powered code assistance.

Start by entering a question, issue description, or bug in the Navie chat, and get answers from a "senior AI software
developer" that has a deep understanding of your application.

Choose your AI LLM to use with Navie AI including: GitHub Copilot’s LLM, OpenAI, Anthropic Claude, Gemini, and local
models.

## Requirements and Use

**2023.1** and newer JetBrains IDEs are required to use this plugin.

AppMap works best* with the following:

* **Languages:** Java, Kotlin, Python, Ruby, and Node.js (TypeScript and JavaScript).
* **Frameworks:** Spring, Django, Flask, Ruby on Rails, Nest.js, Next.js, and Express.

Refer to AppMap documentation for the latest information on supported languages, frameworks, and versions.

[*] AppMap Navie is designed to work with a wide range of languages and frameworks, but AppMap data recordings require a
language-specific library.

## Get started

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

## Chat Modes

Navie provides different modes of interaction to assist you with your code and project. Here's a quick overview:

- **`@explain` (default)**: Provides context-aware suggestions, specific solutions, and reasons about the
  larger context of the specific code being worked on.

- **`@diagram`**:  Creates and renders Mermaid compatible diagrams within the Navie UI. You can open
  a diagram in the [Mermaid Live Editor](https://mermaid.live), copy the Mermaid Definitions to your clipboard, save
  to disk, or expand a full window view.

- **`@plan`**: Builds a detailed implementation plan for an issue or task. This will focus Navie on only understanding
  the problem and the application to generate a step-by-step plan.

- **`@generate`**: In this mode, responses are optimized to include code snippets and patches that you can apply
  directly to the files you're working on.

- **`@test`**: Provides output optimized for creating and updating test cases. Supports both unit tests and integration
  tests. Test updates will confirm to the existing structure and patterns of your test cases, and will provide updated
  tests based on features or code that is provided.

- **`@search`**: Leverages smart search capabilities to locate specific functions, files, modules, and examples.

- **`@review`**: Performs a review of the code changes on your current branch against the base branch. Provides
  actionable insights on important aspects of the code, ensuring alignment with best practices in areas such as code
  quality, security, and maintainability.

- **`@help`**: Offers assistance with using AppMap and Navie, including guidance for generating and leveraging AppMap
  data effectively.

## Activating Navie

**From the Tools menu**

You can open Navie by clicking on the JetBrains menu option *Tools -> AppMap*. From there you can select *Explain with
AppMap Navie*.

![Open Navie from Tools](https://appmap.io/assets/img/product/tools-appmap-vscode.png)

**From the sidebar panel**

![New Navie Chat](https://appmap.io/assets/img/product/new-navie-chat.png)

## Add files to a Navie chat

You can add specific files to your conversation with Navie. This feature is referred to
as "pinning". Some types of files you may want to pin include:

* Issue description
* Custom prompts
* Code files
* Navie chat responses from previous conversations

![Pin from Response](https://appmap.io/assets/img/pin-from-response.png)

## Choosing your LLM for Navie

By default, if you have the GitHub Copilot plugin installed, and you have an active Copilot subscription, Navie will use
the GitHub Copilot LLM. Otherwise, Navie will default to an OpenAI LLM endpoint provided by AppMap.

You can also configure Navie to use your own LLM API key, or to use a local model. Refer to
the [AppMap Navie documentation](https://appmap.io/docs/navie/bring-your-own-model.html) for details on how to do that.

## Recording AppMap Data

AppMap data brings two benefits to your development workflow:

1) **Runtime diagrams:** You can directly use the AppMap diagrams to understand the runtime behavior of your
   application.
2) **Improved context for Navie AI:** Navie will leverage available AppMap data to provide more accurate and relevant
   answers.

You record AppMap data by running your app — either
by [running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or
by [recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

Navie can assistance with making AppMap data. Use the `@help` command for this.

In IntelliJ, you can record AppMap data using the "Start with AppMap" menu item.

![Start with AppMap](https://appmap.io/assets/img/product/start-with-appmap.png)

#### Requirements for making AppMap data

Supported programming languages: Node.js, Java (+ Kotlin), Ruby, and Python.
AppMap works particularly well with web application frameworks such as: Nest.js, Next.js, Spring, Ruby on Rails, Django,
and Flask.

## Using AppMap data

AppMap diagrams include:

* **Sequence Diagram** to follow the runtime flow of calls made by your application.
* **Dependency Map** to see which libraries and frameworks were used at runtime.
* **Flame Graph** to spot performance issues and bottlenecks.
* **Trace View** to perform detailed function call and data flow tracing.

Using AppMap data, Navie gains deeper knowledge of runtime aspects of code behavior, such as:

* HTTP requests and responses
* SQL queries
* Timing data
* Exceptions
* I/O operations

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE)  |  [Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

To learn more about security of AppMap, or the use of data with AI when using Navie, see the
AppMap [security disclosure](https://appmap.io/security) for more detailed information and discussion.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap for graphing and limited Navie use. Pricing for
premium features and integrations are listed on [AppMap’s Pricing Page](https://appmap.io/pricing).