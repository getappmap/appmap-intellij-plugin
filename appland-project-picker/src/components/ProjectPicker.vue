<template>
    <section>
        <header>
            <h1>Getting started with AppMap</h1>
        </header>
        <main>
            <article>
                <h2>Select a suitable project</h2>
                <div v-if="projects && projects.length > 0">
                    <p class="body-text">To make sure that your projects are suitable for mapping, we make a couple of
                        quick
                        requirement checks on your workspace to help you find a project to start AppMapping.
                        Select a suitable project from the table below.</p>
                    <table>
                        <thead>
                        <tr>
                            <th scope="col">Project name ({{ projects.length }})</th>
                            <th scope="col">Language</th>
                            <th scope="col">Test framework</th>
                            <th scope="col">Web framework</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr v-for="project in projects" :key="project.id"
                            v-bind:class="{ 'selected': currentProject === project, 'good': isGood(project), 'ok': isOk(project), 'bad': isBad(project) }"
                            @click="selectProject(project)">
                            <td>{{ project.name }}</td>
                            <FeatureColumn :feature="project.features.lang"/>
                            <FeatureColumn :feature="project.features.test"/>
                            <FeatureColumn :feature="project.features.web"/>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </article>
            <article class="explain good ok" v-if="currentProject && (isGood(currentProject) || isOk(currentProject))">
                <br/>
                <h2>Install AppMap agent</h2>
                <p class="body-text">AppMap agent records executing code. It creates JSON files as you execute test
                    cases, run sample programs, or perform interactive sessions with your app. This script will guide
                    you through the installation process. Run it in the project's environment so it can correctly detect
                    runtimes and libraries.</p>
                <p class="explain ok note body-text" v-bind:class="{ 'hidden': !isOk(currentProject) }">It appears this
                    project might not be a good choice for your first
                    AppMap.
                    We recommend you pick another project; proceed at your own risk.</p>
                <p class="body-text">If you do not have Node.js installed, or would prefer manual installation of the
                    AppMap agent visit our
                    <a id="docref-step2"
                       :href="'https://appland.com/docs/quickstart/vscode/' + lang(currentProject) + '-step-2.html'">installation
                        documentation.</a></p>
                <p class="command"><code>
                    npx @appland/appmap install <span id="directory">{{ quote(currentProject.path) }}</span>
                </code></p>
                <br/>
                <h2>Record AppMaps</h2>
                <p>To record AppMaps from a running application or from integration tests <a id="docref-step3"
                                                                                             :href="'https://appland.com/docs/quickstart/vscode/' + lang(currentProject) + '-step-3.html'">follow
                    these instructions.</a></p>
            </article>
            <article class="explain bad" v-if="!currentProject || isBad(currentProject)">
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
import FeatureColumn from './FeatureColumn'

export default {
    name: 'ProjectPicker',
    props: ['projects'],
    data: function () {
        return {
            currentProject: undefined
        }
    },
    components: {FeatureColumn},
    expose: ['classOfScore', 'selectProject'],
    methods: {
        isBad: function (project) {
            return project.score < 3
        },
        isOk: function (project) {
            return project.score === 3
        },
        isGood: function (project) {
            return project.score > 3
        },
        selectProject: function (project) {
            this.currentProject = project;
        },
        lang: function (project) {
            const lang = project?.features?.lang?.title;
            return lang ? lang?.toLowerCase() : "";
        },
        // Quote path for the command line
        quote: function (path) {
            // Don't try to be too smart, shell quoting is an ugly can of worms.
            // Just quote spaces if needed; this is pretty common on some platforms.
            // If the user has funnier characters in paths, they should be smart
            // enough to deal with them.
            if (path.includes(' ')) return '"' + path + '"';
            return path;
        }
    }
}
</script>

<style>
:root {
    --appmap-border: #7F6BE6;
    --vscode-foreground: #c6c6c6;
    --vscode-welcomePage-tileBackground: #252526;
    --vscode-welcomePage-tileShadow: rgba(0, 0, 0, 0.36);
    --vscode-list-inactiveSelectionBackground: rgba(255, 255, 255, 0.1);
    --vscode-problemsWarningIcon-foreground: #ecc30b;
    --vscode-problemsErrorIcon-foreground: #cd1414;
    --vscode-list-warningForeground: #ecc30b;
    --vscode-list-errorForeground: #cd1414;

    --appland-text-font-family: 'IBM Plex Sans', 'Helvetica Neue', Helvetica, Arial, sans-serif;
    --vs-code-gray1: #1E1E1E;
}

body {
    color: var(--vscode-foreground);
    background-color: var(--vs-code-gray1);
    font-family: var(--appland-text-font-family);
}

a, a:visited, a:focus {
    color: var(--vscode-foreground);
}
</style>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.hidden {
    display: none;
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
