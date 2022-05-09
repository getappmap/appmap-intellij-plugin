<template>
    <section>
        <header>
            <h1>Getting started with AppMap</h1>
        </header>
        <main>
            <article>
                <h2>Select a suitable project</h2>
                <div v-if="rows && rows.length > 0">
                    <p class="body-text">To make sure that your projects are suitable for mapping, we make a couple of
                        quick
                        requirement checks on your workspace to help you find a project to start AppMapping.
                        Select a suitable project from the table below.</p>
                    <table>
                        <thead>
                        <tr>
                            <th scope="col">Project name ({{ rows.length }})</th>
                            <th scope="col">Language</th>
                            <th scope="col">Test framework</th>
                            <th scope="col">Web framework</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr v-for="row in rows" :key="row.id">
                            <td>{{ row.name }}</td>
                            <td>{{ row.language }}</td>
                            <td>{{ row.testFramework }}</td>
                            <td>{{ row.webFramework }}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </article>
            <article class="explain good ok">
                <br/>
                <h2>Install AppMap agent</h2>
                <p class="body-text">AppMap agent records executing code. It creates JSON files as you execute test
                    cases, run sample programs, or perform interactive sessions with your app. This script will guide
                    you through the installation process. Run it in the project's environment so it can correctly detect
                    runtimes and libraries.</p>
                <p class="explain ok note body-text">It appears this project might not be a good choice for your first
                    AppMap.
                    We recommend you pick another project; proceed at your own risk.</p>
                <p class="body-text">If you do not have Node.js installed, or would prefer manual installion of the
                    AppMap agent visit our
                    <a id="docref-step2" href="https://appland.com/docs/quickstart/vscode/step-2">installation
                        documentation.</a></p>
                <p class="command"><code>
                    npx @appland/appmap install <span id="directory"></span>
                </code></p>
                <br/>
                <h2>Record AppMaps</h2>
                <p>To record AppMaps from a running application or from integration tests <a id="docref-step3"
                                                                                             href="https://appland.com/docs/quickstart/vscode/step-3">follow
                    these instructions.</a></p>
            </article>
            <article class="explain bad">
                <p>For your first AppMap, we recommend a project that:</p>
                <ul>
                    <li>is a web application or a web service</li>
                    <li>is written in Python (Django or Flask), Ruby (Rails), Java (Spring), or JavaScript (Node &
                        Express)
                    </li>
                    <li>has reasonably comprehensive integration test suite</li>
                </ul>
                <p><b>Please open a project meeting these recommendations to proceed.</b></p>
                <!-- let's do this later
                <p>Prefer an example? Try this, that or these instead.#TODO</p>
                -->
            </article>
        </main>
    </section>
</template>

<script>
export default {
    name: 'ProjectPicker',
    props: ['rows']
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
:root {
    --appmap-border: #7F6BE6;
}

body {
    color: var(--vscode-foreground);
}

.body-text {
    max-width: 550px;
}

main {
    counter-reset: step;
}

section {
    max-width: 1280px;
    background: var(--vscode-welcomePage-tileBackground);
    margin: 1em auto;
    border-radius: 8px;
    filter: drop-shadow(2px 2px 2px var(--vscode-welcomePage-tileShadow));
    padding: 2em;
}

h1 {
    margin-block-start: 0;
    font-size: 2em;
}

h2 {
    margin-block-end: 0;
    counter-increment: step;
}

h2::before {
    content: counter(step) ". ";
}

header {
    margin-block-end: 2em;
}

table {
    width: 100%;
    text-align: right;
    border: 1px solid var(--appmap-border);
    border-radius: 8px;
    border-spacing: 0;
}

tr :first-child {
    text-align: left;
    padding-left: 6ex;
    position: relative;
}

tr.selected {
    background: var(--vscode-list-inactiveSelectionBackground);
}

td, th {
    padding: 1em 2ex;
}

thead tr th {
    border-bottom: 1px solid var(--appmap-border);
}

tbody tr :first-child:before {
    position: absolute;
    border: 0.2em solid var(--icon-color);
    border-radius: 50%;
    width: 1em;
    height: 1em;
    left: 2ex;
    color: var(--icon-color);
    top: 0.9em;
    text-align: center;
    line-height: 1em;
}

tr.good :first-child:before {
    content: "✔";
    --icon-color: green;
}

tr.ok :first-child:before {
    content: "·";
    --icon-color: var(--vscode-problemsWarningIcon-foreground);
}

tr.bad :first-child:before {
    content: "×";
    --icon-color: var(--vscode-problemsErrorIcon-foreground);
}

td.ok {
    color: var(--vscode-list-warningForeground);
}

td.bad {
    color: var(--vscode-list-errorForeground);
}

tr.ok, tr.bad {
    opacity: 0.6;
}

p.command {
    border-radius: 8px;
    border: thin solid var(--appmap-border);
    padding: 1em;
    position: relative;
    overflow: hidden;
}

p.command button {
    bottom: 0;
    padding: 0 2ex;
    color: inherit;
    right: 0;
    position: absolute;
    text-align: center;
    background: #7f6be677;
    height: 100%;
    top: 0;
    border: none;
    border-left: thin solid var(--appmap-border);
    line-height: 3em;
}

p.command button:hover {
    background: #7f6be6aa;
}

p.command button:active {
    background: #7f6be6ff;
}

p.note {
    font-style: italic;
}

p.note::before {
    content: "Note: ";
    font-size: large;
    opacity: 0.8;
    font-variant-caps: all-small-caps;
    margin-right: 0.8ex;
    font-style: normal;
}
</style>
