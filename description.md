AppMap is a runtime analysis and visualization platform. It analyzes your application's runtime code behavior before your changes go to production, and allows you to understand complex code interactions.

## Runtime behavior visualization
After running your application with the AppMap library, the runtime recordings are instantly visualized in the IDE. These visualizations include:
* **Sequence Diagrams** to follow the runtime flow of calls made by your application
* **Dependency Maps** to see which libraries and frameworks were used at runtime
* **Flame Graphs** to spot performance issues and bottlenecks
* **Trace Views** to perform detailed function call and data flow tracing

## Runtime flaw analysis
After making recordings of how your application behaved at runtime, AppMap analyzes those recordings to automatically detect performance issues like N+1 queries, and security flaws such as faulty authentication logic.

## AppMap in CI
The same features available in this plugin are [also available for CI systems](https://appmap.io/docs/analysis/in-ci.html). AppMap for CI can also analyze each Pull Request and check for flaws, highlight unexpected API changes, provide insights into CI test failures, and display behavioral comparisons of runtime changes.

## How does AppMap work?
AppMap loads an agent library into your application, and it records detailed traces of how your application runs at development time as local JSON files. AppMap analyzes those recordings to uncover performance and security flaws that static analysis tools cannot find. AppMap does this by being aware of how code interacts at runtime with web services, view templates, HTTP requests, caching, authentication, encryption, and SQL databases.

## Requirements and use

**2021.1** and newer JetBrains IDEs are required to use this plugin.

Supported web applications and API frameworks: Ruby on Rails, Django, Flask, Express, and Spring.

Supported programming languages: Java, Python, Ruby, TypeScript/JavaScript (for Node.js applications only).

To start making AppMaps, you’ll need to install and configure the AppMap client agent for your project. Then, you’ll make AppMaps by running your app - either by
[running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or by
[recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording). 

## Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/main/LICENSE)

[Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

Data usage: AppMap runtime recordings and diagrams are created and stored locally on your machine. AppMap does not require any permissions to your web hosted code repo in order to run. For more information, see the AppMap [security disclosure](https://appmap.io/security).

Sign-in via GitHub or GitLab is required to obtain a license key to start using AppMap in your code editor.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap.

## Getting started with AppMap

[Documentation](https://appmap.io/docs/appmap-overview.html) for guides and videos.

[GitHub](https://github.com/getappmap) for our repository and open source project.

[Blog](https://appmap.io/blog/) for user stories and product announcements.

[Slack](https://appmap.io/slack) or email for support and community conversations: [support@appmap.io](mailto:support@appmap.io)
