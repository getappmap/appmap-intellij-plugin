AppMap Navie is the AI software architect that gives you deep code explanations and code reviews along with diagrams
about your code. AppMap brings runtime-awareness to your code editor and AI-powered code assistance.

Start by pasting an issue, a bug, or describe a problem in the chat box and get answers from a senior AI software
developer that knows your application best.

Choose your AI LLM to use with Navie AI including: GitHub Copilot’s LLM, OpenAI, Anthropic Claude, Gemini, and local
models.

TODO: Update header image


## Requirements and Use

**2023.1** and newer JetBrains IDEs are required to use this plugin.  

Supported web applications and API frameworks include: Ruby on Rails, Django, Flask, Express, Nest.js, Next.js, and
Spring,
Kotlin, and Scala

Supported programming languages: Java, Python, Ruby, TypeScript/JavaScript (for Node.js).

Refer to AppMap documentation for the latest information on supported languages, frameworks, and versions.

## Get started

1. **Install [the AppMap Plugin](https://plugins.jetbrains.com/plugin/16701-appmap)** from within the code editor or
   from the marketplace.

2. **Sign in** using an email address to obtain a license key and activate AppMap. It's best to use your work email, so
   that your license will be associated with your workplace rather than your personal account.

3. **Ask Navie** for assistance with the project you're working on. Use the `@help` command to get help with AppMap and Navie itself.

## Examples

[Here are some examples](https://appmap.io/product/examples/navie) of Navie making context-aware suggestions, providing
tactical solutions, and reasoning about the larger context of the specific code being worked on.

## Chat Modes

Navie provides different modes of interaction to assist you with your code and project. Here's a quick overview:

- **`@explain` (default)**: Navie makes context-aware suggestions, provides specific solutions, and reasons about the
  larger context of the specific code being worked on.

- **`@diagram`**:  Navie will create and render a Mermaid compatible diagram within the Navie chat window. You can open
  this diagram in the [Mermaid Live Editor](https://mermaid.live), copy the Mermaid Definitions to your clipboard, save
  to disk, or expand a full window view.

- **`@plan`**: Navie focuses the AI response on building a detailed implementation plan for the relevant query. This
  will focus Navie on only understanding the problem and the application to generate a step-by-step plan.

- **`@generate`**: Activate code generation mode by beginning any question with the prefix "@generate". In this mode
  Navie's response are optimized to include code snippets you can use directly in the files are working on.

- **`@test`**: Navie's responses are optimized for test case creation, such as unit testing or integration testing. This
  prefix will understand how your tests are currently written and provide updated tests based on features or code that
  is provided.

- **`@search`**: By leveraging smart search capabilities, this command will locate specific code elements, relevant modules,
  or examples.

- **`@review`**: This command will review the code changes on your current branch and provide actionable insights on various
  aspects of code, ensuring alignment with best practices in areas such as code quality, security, and maintainability.

- **`@help`**: Activate help mode by beginning any question with the prefix "@help". This mode offers assistance with
  using AppMap, including guidance for generating and leveraging AppMap data effectively.

## Activating Navie

**From the Tools menu**

You can open Navie by clicking on the JetBrains menu option *Tools -> AppMap*. From there you can select *Explain with
AppMap Navie*.

![image](https://appmap.io/assets/img/product/tools-appmap-vscode.png)

**From the sidebar panel**

TODO: Include a screenshot of "New Navie Chat" in the sidebar.

## Add files to a Navie chat

You can add specific files to your conversation with Navie. This feature is referred to
as "pinning". Some types of files you may want to pin include:

* Issue description
* Custom prompts
* Code files
* Navie chat responses from previous conversations

![image](https://appmap.io/assets/img/pin-from-response.png)

## Choosing your LLM for Navie

By default, if you have the GitHub Copilot plugin installed, and you have an active Copilot subscription, Navie will use
the GitHub Copilot LLM. Otherwise, Navie will default to an OpenAI LLM endpoint provided by AppMap.

You can also configure Navie to use your own LLM API key, or to use a local model. Refer to
the [AppMap Navie documentation](https://appmap.io/docs/navie/bring-your-own-model.html) for details on how to do that.

## Recording AppMap Data

AppMap data brings two benefits to your development workflow:

1) **Runtime diagrams:** You can directly use the AppMap diagrams to understand the runtime behavior of your application.
2) **Improved context for Navie AI:** Navie will leverage available AppMap data to provide more accurate and relevant
   answers.

You record AppMap data by running your app — either
by [running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or
by [recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

Navie can assistance with making AppMap data. Use the `@help` command for this.

In IntelliJ, you can record AppMap data using the "Start with AppMap" menu item.

TODO: Screenshot of Start with AppMap

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