import Vue from 'vue';
import {VInstallGuide} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountInstallGuide() {
    const messages = new MessagePublisher(vscode);

    messages.on('init', ({projects: startProjects, page: startPage, disabled, findingsEnabled}) => {
        let currentPage = startPage;
        let currentProject;

        const app = new Vue({
            el: '#app',
            render(h) {
                return h(VInstallGuide, {
                    ref: 'ui',
                    props: {
                        projects: this.projects,
                        disabledPages: new Set(disabled),
                        editor: 'jetbrains',
                        findingsEnabled,
                    },
                });
            },
            data() {
                return {
                    projects: startProjects,
                };
            },
            beforeCreate() {
                this.$on('open-page', async (pageId) => {
                    // Wait until next frame if there's no current project. It may take some time for the
                    // view to catch up.
                    if (!currentProject) await new Promise((resolve) => requestAnimationFrame(resolve));

                    currentPage = pageId;
                    vscode.postMessage({
                        command: 'open-page',
                        page: currentPage,
                        project: currentProject,
                    });
                });
            },
            mounted() {
                this.$refs.ui.jumpTo(startPage);

                document.addEventListener('click', (e) => {
                    if (e && e.target) {
                        const href = e.target.href;
                        if (href && (href.startsWith("http://") || href.startsWith("https://"))) {
                            e.preventDefault();
                            vscode.postMessage({command: 'click-link', uri: e.target.href});
                            return false;
                        }
                    }
                });
            },
        });

        app.$on('clipboard', (text) => {
            vscode.postMessage({
                command: 'clipboard',
                page: currentPage,
                project: currentProject,
                text,
            });
        });

        app.$on('select-project', (project) => {
            currentProject = project;
        });

        app.$on('view-problems', (projectPath) => {
            vscode.postMessage({command: 'view-problems', projectPath});
        });

        app.$on('openAppmap', (file) => {
            vscode.postMessage({command: 'open-file', file});
        });

        app.$on('open-instruction', (pageId) => {
            app.$refs.ui.jumpTo(pageId);
        });

        app.$on('generate-openapi', (projectPath) => {
            messages.rpc('generate-openapi', {projectPath});
        });

        messages.on('page', ({page}) => {
            app.$refs.ui.jumpTo(page);
        });

        messages.on('projects', ({projects}) => {
            app.projects = projects;
            app.$forceUpdate();
        });
    });

    vscode.postMessage({command: 'ready'});
}
