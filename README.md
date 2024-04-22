# AppMap for JetBrains IDEs

AppMap is a developer tools platform powered by runtime analysis and AI.  Navie is the first **AI code architect** with the context to understand how your app works when it runs, like a principal engineer or software architect.

Navie uses runtime application analysis to improve the quality and accuracy of AI-code suggestions.  Navie can answer more challenging questions about your app and help you with more complex tasks like: 

**Troubleshooting and debugging**

**Refactoring & modernizing code**

**Improving performance and stability**

**Designing secure code**


**Navie - AI Code Architect**
Navie AI is powered by a new type of AI code context provider, which combines source code snippets with AppMap’s unique runtime data that you can create each time you run your code. When asked a question, Navie will search for the best context to answer your question including code snippets and AppMap data to provide answers truly specific to your app.

By default Navie uses GPT-4 through a proxy. You can change this to use a local LLM model of your choice, or you can bring your own LLM API key and Navie will communicate directly with your chosen LLM backend.

**AppMap data - runtime traces and interactive visualizations**
AppMap data are displayed as interactive visualizations of your code’s behavior so you can see how your app works when it runs. Visualizations include: sequence diagrams, dependency maps, trace views, flame graphs for performance analysis. AppMap data is regular JSON files that are stored locally on your own filesystem.

### System Requirements

**IntelliJ IDEA 2023.2** or later is required to use this plugin. Only installations, which use the bundled JetBrains Java runtime, support the JCEF engine for rendering.

### Get started
1. **Install [the AppMap plugin](https://plugins.jetbrains.com/plugin/16701-appmap)** from within the code editor or from the marketplace.  

2. **Sign in with an email address, or with GitHub or GitLab** and Navie will be available in `@explain` mode. This enables Navie to respond to general coding and development questions and answer questions about using AppMap data.

3. **Ask Navie** for guidance recording AppMap data specific to interactions or code scenarios you're interested in analyzing.

<Gif>

### Examples
Check out these examples of Navie providing interactive AI coding assistance based on relevant code, HTTP requests, and database queries that it finds automatically.

TODO: we need a great Java example

1. [Find and fix slow API endpoints in a FastAPI app](https://appmap.io/navie/how-to/fix-slow-api-endpoints-in-a-fastapi-app-with-navie/)
2. [Find and fix a database performance issue in Ruby on Rails](https://appmap.io/navie/how-to/find-and-fix-a-database-performance-issue-in-ruby-on-rails/)
3. [Quickly add a new feature to a complex Python app](https://appmap.io/navie/how-to/adding-a-new-feature-to-a-complex-python-application/)
4. [Fixing performance issues with MongoDB in a MERN app](https://appmap.io/navie/how-to/fixing-performance-issues-with-mongodb-in-a-mern-app/)

### Chat Modes

Navie provides different modes of interaction to assist you with your code and project. Here's a quick overview:

- **`@explain` (default)**: Navie makes context-aware suggestions, provides specific solutions, and reasons about the larger context of the specific code being worked on.

- **`@help`**: Activate help mode by beginning any question with the prefix "@help". This mode offers assistance with using AppMap, including guidance for generating and leveraging AppMap data effectively.

- **`@generate`**: Activate code generation mode by beginning any question with the prefix "@generate". In this mode Navie's response are optimized to include code snippets you can use directly in the files are working on.

**Open a Navie chat from the Tools menu**: You can open Navie by navigating to Tools -> AppMap -> Explain with AppMap Navie

**Ask Navie about a specific AppMap visualization**: You can target your question more specifically to an AppMap, representing a test case, API call, or other interaction by clicking the “Ask Navie” box on any AppMap you open.


### Bring your own key or model for Navie

In order to configure Navie for your own LLM, certain environment variables need to be set for AppMap services. Refer to the [AppMap documentation](https://appmap.io/docs/navie/bring-your-own-model.html) for details on how to do that.

### Creating AppMap Data

Ask Navie to guide you through the process of making AppMap data, or navigate to the Record AppMaps screen in your code editor.

You’ll start by configuring the AppMap language library for your project. Then you’ll make a recording of the code you are working on by running your application in your development environment with AppMap enabled. AppMap data files will automatically be generated and stored on your local file system.

Once you’ve recorded AppMap data, Navie's awareness of your application’s behavior and code will be significantly upgraded.

**Using AppMap data Navie can:**
* Explain code or application behavior, including queries, web service requests, and more.
* Make code suggestions like a senior software developer.
* Find the potential performance problems or dynamic security flaws in existing or newly written code.
* Help you document application behavior changes for a PR.
* Navie’s code recommendations span multiple files, functions, APIs, databases, and more.

Naive answers are backed up by references to AppMap data. Naive presents this data alongside the chat discussion, and you can also open and use AppMap diagrams independently of Navie.

AppMap diagrams include:

* **Sequence Diagrams** to follow the runtime flow of calls made by your application.
* **Dependency Maps** to see which libraries and frameworks were used at runtime.
* **Flame Graphs** to spot performance issues and bottlenecks.
* **Trace Views** to perform detailed function call and data flow tracing.

GIF

#### Requirements for making AppMap dat# AppMap for JetBrains IDEs

AppMap is a developer tools platform powered by runtime analysis and AI.  Navie is the first AI code architect with the context to understand how your app works when it runs, like a principal engineer or software architect.

Navie uses runtime application analysis to improve the quality and accuracy of AI-code suggestions.  Navie can answer more challenging questions about your app and help you with more complex tasks like: 

Troubleshooting and debugging
Refactoring & modernizing code
Improving performance and stability
Designing secure code


Navie - AI Code Architect
Navie AI is powered by a new type of AI code context provider, which combines source code snippets with AppMap’s unique runtime data that you can create each time you run your code. When asked a question, Navie will search for the best context to answer your question including code snippets and AppMap data to provide answers truly specific to your app.

By default Navie uses GPT-4 through a proxy. You can change this to use a local LLM model of your choice, or you can bring your own LLM API key and Navie will communicate directly with your chosen LLM backend.

AppMap data - runtime traces and interactive visualizations
AppMap data are displayed as interactive visualizations of your code’s behavior so you can see how your app works when it runs. Visualizations include: sequence diagrams, dependency maps, trace views, flame graphs for performance analysis. AppMap data is regular JSON files that are stored locally on your own filesystem.

### System Requirements

IntelliJ IDEA 2023.2 or later is required to use this plugin.

Only installations, which use the bundled JetBrains Java runtime, support the JCEF engine for rendering.

### Get started
1. **Install [the AppMap plugin](https://plugins.jetbrains.com/plugin/16701-appmap)** from within the code editor or from the marketplace.  

2. **Sign in with an email address, or with GitHub or GitLab** and Navie will be available in `@explain` mode. This enables Navie to respond to general coding and development questions and answer questions about using AppMap data.

3. **Ask Navie** for guidance recording AppMap data specific to interactions or code scenarios you're interested in analyzing.

<Gif>

### Examples
Check out these examples of Navie providing interactive AI coding assistance based on relevant code, HTTP requests, and database queries that it finds automatically.

TODO: we need a great Java example

1. [Find and fix slow API endpoints in a FastAPI app](https://appmap.io/navie/how-to/fix-slow-api-endpoints-in-a-fastapi-app-with-navie/)
2. [Find and fix a database performance issue in Ruby on Rails](https://appmap.io/navie/how-to/find-and-fix-a-database-performance-issue-in-ruby-on-rails/)
3. [Quickly add a new feature to a complex Python app](https://appmap.io/navie/how-to/adding-a-new-feature-to-a-complex-python-application/)
4. [Fixing performance issues with MongoDB in a MERN app](https://appmap.io/navie/how-to/fixing-performance-issues-with-mongodb-in-a-mern-app/)

### Chat Modes

Navie provides different modes of interaction to assist you with your code and project. Here's a quick overview:

- **`@explain` (default)**: Navie makes context-aware suggestions, provides specific solutions, and reasons about the larger context of the specific code being worked on.

- **`@help`**: Activate help mode by beginning any question with the prefix "@help". This mode offers assistance with using AppMap, including guidance for generating and leveraging AppMap data effectively.

- **`@generate`**: Activate code generation mode by beginning any question with the prefix "@generate". In this mode Navie's response are optimized to include code snippets you can use directly in the files are working on.

**Open a Navie chat from the Tools menu**: You can open Navie by navigating to Tools -> AppMap -> Explain with AppMap Navie

**Ask Navie about a specific AppMap visualization**: You can target your question more specifically to an AppMap, representing a test case, API call, or other interaction by clicking the “Ask Navie” box on any AppMap you open.


### Bring your own key or model for Navie

In order to configure Navie for your own LLM, certain environment variables need to be set for AppMap services. Refer to the [AppMap documentation](https://appmap.io/docs/navie/bring-your-own-model.html) for details on how to do that.

### Creating AppMap Data

Ask Navie to guide you through the process of making AppMap data, or navigate to the Record AppMaps screen in your code editor.

You’ll start by configuring the AppMap language library for your project. Then you’ll make a recording of the code you are working on by running your application in your development environment with AppMap enabled. AppMap data files will automatically be generated and stored on your local file system.

Once you’ve recorded AppMap data, Navie's awareness of your application’s behavior and code will be significantly upgraded.

Using AppMap data Navie can:
* Explain code or application behavior, including queries, web service requests, and more.
* Make code suggestions like a senior software developer.
* Find the potential performance problems or dynamic security flaws in existing or newly written code.
* Help you document application behavior changes for a PR.
* Navie’s code recommendations span multiple files, functions, APIs, databases, and more.

Naive answers are backed up by references to AppMap data. Naive presents this data alongside the chat discussion, and you can also open and use AppMap diagrams independently of Navie.

AppMap diagrams include:

* **Sequence Diagrams** to follow the runtime flow of calls made by your application.
* **Dependency Maps** to see which libraries and frameworks were used at runtime.
* **Flame Graphs** to spot performance issues and bottlenecks.
* **Trace Views** to perform detailed function call and data flow tracing.

GIF

#### Requirements for making AppMap data

Supported programming languages: Node.js, Java (+ Kotlin), Ruby, and Python.
AppMap works particularly well with web application frameworks such as: Nest.js, Next.js, Spring, Ruby on Rails, Django, and Flask.

To start making AppMap data, you’ll need to install and configure the AppMap client agent for your project.

Make AppMap data by running your app—either by [running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or by [recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE)  |  [Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

To learn more about security of AppMap, or the use of data with AI when using Navie, see the AppMap [security disclosure](https://appmap.io/security) for more detailed information and discussion.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap for graphing and limited Navie use. Pricing for premium features and integrations are listed on [AppMap’s Pricing Page](https://appmap.io/pricing).

Supported programming languages: Node.js, Java (+ Kotlin), Ruby, and Python.
AppMap works particularly well with web application frameworks such as: Nest.js, Next.js, Spring, Ruby on Rails, Django, and Flask.

To start making AppMap data, you’ll need to install and configure the AppMap client agent for your project.

Make AppMap data by running your app—either by [running test cases](https://appmap.io/docs/recording-methods.html#recording-test-cases), or by [recording a short interaction with your app](https://appmap.io/docs/recording-methods.html#remote-recording).

## Licensing and Security

[Open source MIT license](https://github.com/getappmap/appmap-intellij-plugin/blob/develop/LICENSE)  |  [Terms and conditions](https://appmap.io/community/terms-and-conditions.html)

To learn more about security of AppMap, or the use of data with AI when using Navie, see the AppMap [security disclosure](https://appmap.io/security) for more detailed information and discussion.

There is [no fee](https://appmap.io/pricing) for personal use of AppMap for graphing and limited Navie use. Pricing for premium features and integrations are listed on [AppMap’s Pricing Page](https://appmap.io/pricing).
