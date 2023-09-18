AppMap is a free and open-source runtime code analysis tool.

AppMap records code execution traces, collecting information about how your code works and what it does. Then it presents this information as interactive diagrams that you can search and navigate. In the diagrams, you can see exactly how functions, web services, data stores, security, I/O, and dependent services all work together when application code runs.

## Requirements and Use

**2021.3** and newer JetBrains IDEs are required to use this plugin.

Supported web applications and API frameworks: Ruby on Rails, Django, Flask, Express, and Spring.

Supported programming languages: Java, Python, Ruby, TypeScript/JavaScript (for Node.js applications only).

To start making AppMaps, you’ll need to install and configure the AppMap client agent for your project. Then, you’ll make AppMaps by running your app - either by
[running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or by
[recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording). 

## AppMap Features

### Runtime behavior visualization
AppMap for JetBrains includes the following types of interactive diagrams:
* **Sequence Diagrams** to follow the runtime flow of calls made by your application
* **Dependency Maps** to see which libraries and frameworks were used at runtime
* **Flame Graphs** to spot performance issues and bottlenecks
* **Trace Views** to perform detailed function call and data flow tracing

### Runtime analysis
After making recordings of how your application behaved at runtime, AppMap analyzes those recordings to automatically detect performance issues like N+1 queries, and security flaws such as faulty authentication logic.

### AppMap in CI
The same features available in this plugin are [also available for CI systems](https://appmap.io/docs/analysis/in-ci.html).

## Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/main/LICENSE)

[Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

Data usage: AppMap runtime recordings and diagrams are created and stored locally on your machine. AppMap for JetBrains does not require any permissions to your web hosted code repo in order to run. For more information, see the AppMap [security disclosure](https://appmap.io/security).

Sign-in via GitHub or GitLab is required only to obtain a license key to start using AppMap in your code editor.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap.

## Getting started with AppMap

[Documentation](https://appmap.io/docs/appmap-overview.html) for guides and videos.

[GitHub](https://github.com/getappmap) for our repository and open source project.

[Blog](https://appmap.io/blog/) for user stories and product announcements.

[Slack](https://appmap.io/slack) or email for support and community conversations: [support@appmap.io](mailto:support@appmap.io)
