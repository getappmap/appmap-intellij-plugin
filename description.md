AppMap is a free and open-source runtime code analysis tool.

AppMap records your running code in your development environment, collecting information about how your code works and what it does.

Then it uses this information to create AppMaps—interactive diagrams that describe your code’s behavior in sequence diagrams, dependency maps, trace views and flame graphs.

Once you've created AppMaps, you can chat with AppMap new AI assistant, Navie to understand your code. Navie uses your local AppMaps files to provide you with explanations and better code suggestions.

Navie is the AI Coding Assistant for Senior Developers. Navie’s suggestions are better for complex code changes compared to other AI code assistants because Navie includes runtime context. Navie’s code recommendations span files, functions, APIs, databases and more.

## Requirements and Use

**2021.3** and newer JetBrains IDEs are required to use this plugin.

Supported web applications and API frameworks: Ruby on Rails, Django, Flask, Express, Nest.js, Next.js, and Spring, Kotlin, and Scala

Supported programming languages: Java, Python, Ruby, TypeScript/JavaScript (for Node.js).

To start making AppMaps, you’ll need to install and configure the AppMap client agent for your project. Then, you’ll make AppMaps by running your app - either by
[running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or by
[recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

## AppMap Features

### AppMap Navie AI

AppMap Navie AI is a powerful chat interface that will help you gain insight about your project.
Navie uses your AppMaps and code snippets to provide you with helpful explanations about your software and specific code suggestions that are more relevant to your codebase than typical generative AI coding assistants.

When you ask Navie a question, it will retrieve and display relevant AppMaps of your code so you can see how the AI arrived at the code suggestions. The AppMaps are visualizations of the code that you're discussing with Navie, helping you understand how your code works and how you can improve it. Navie can also help you improve the runtime security and performance of your code by using AppMap findings and AppMap data in the response. It is like Pair Programming with a Principal Engineer who knows your code inside and out.

### Runtime behavior visualization

AppMap for JetBrains includes the following types of interactive diagrams:

-   **Sequence Diagrams** to follow the runtime flow of calls made by your application
-   **Dependency Maps** to see which libraries and frameworks were used at runtime
-   **Flame Graphs** to spot performance issues and bottlenecks
-   **Trace Views** to perform detailed function call and data flow tracing

### Runtime analysis

After making recordings of how your application behaved at runtime, AppMap analyzes those recordings to automatically detect performance issues like N+1 queries, and security flaws such as faulty authentication logic.

### AppMap in CI

The same features available in this plugin are [also available for CI systems](https://appmap.io/docs/analysis/in-ci.html).

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE)

[Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

AppMap graphs, runtime recordings, and diagrams and data are created and stored locally on your
Machine in a directory you choose.

AppMap for JetBrains does not require any permissions to your web hosted code repo
in order to run.

Using AppMap’s integrations with Confluence, GitHub Actions, and Chat AI integration features requires access to code snippets and AppMap data either within your own accounts or via AppMap’s accounts; see the AppMap [security disclosure](https://appmap.io/security) for detailed information about each integration

Sign-in via GitHub or GitLab is required only to obtain a license key to start using AppMap in your
code editor, or you can request a trial license on getappmap.com.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap, pricing for premium features and integrations are listed on [AppMap’s Pricing Page](https://appmap.io/pricing).

## Getting started with AppMap

[Documentation](https://appmap.io/docs/appmap-overview.html) for guides and videos.

[GitHub](https://github.com/getappmap) for our repository and open source project.

[Blog](https://appmap.io/blog/) for user stories and product announcements.

[Slack](https://appmap.io/slack) or email for support and community conversations: [support@appmap.io](mailto:support@appmap.io)
