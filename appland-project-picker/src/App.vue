<template>
    <ProjectPicker :projects="projects"/>
</template>

<script>
import ProjectPicker from './components/ProjectPicker'

export default {
    name: 'App',
    components: {
        ProjectPicker,
    },
    mounted() {
        // this is a workaround to notify the plugin that the app is ready
        console.log("intellij-plugin-ready");
    },
    data() {
        return {
            projects: []
        }
    }
}
</script>

<style>
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
