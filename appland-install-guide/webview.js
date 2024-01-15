import Vue from 'vue';
import {VInstallGuide} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from 'messagePublisher';
import vscode from 'vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', (initialData) => {
    let currentPage = initialData.page;
    let currentProject;

    const app = new Vue({
      el: '#app',
      render(h) {
        return h(VInstallGuide, {
          ref: 'ui',
          props: {
            projects: this.projects,
            editor: 'jetbrains',
            analysisEnabled: this.analysisEnabled,
            userAuthenticated: this.userAuthenticated,
            featureFlags: new Set(['disable-record-pending']),
            disabledPages: new Set(this.disabledPages),
            findingsEnabled: this.findingsEnabled,
          },
        });
      },
      data() {
        return {
          projects: initialData.projects,
          analysisEnabled: initialData.analysisEnabled,
          userAuthenticated: initialData.userAuthenticated,
          disabledPages: initialData.disabledPages,
          findingsEnabled: initialData.findingsEnabled,
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
            projects: this.projects,
          });
        });
      },
      mounted() {
        this.$refs.ui.jumpTo(initialData.page);

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

    app.$on('open-findings-overview', () => {
      vscode.postMessage({command: 'open-findings-overview'});
    });

    app.$on('openAppmap', (file) => {
      vscode.postMessage({command: 'open-file', file});
    });

    app.$on('open-instruction', (pageId) => {
      app.$refs.ui.jumpTo(pageId);
    });

    app.$on('perform-install', (path, language) => {
      vscode.postMessage({command: 'perform-install', path, language});
    });

    app.$on('generate-openapi', (projectPath) => {
      messages.rpc('generate-openapi', {projectPath});
    });

    app.$on('perform-auth', () => {
      vscode.postMessage({command: 'perform-auth'});
    });

    // listeners for messages sent by the plugin
    messages.on('page', ({page}) => {
      app.$refs.ui.jumpTo(page);
    });

    messages.on('projects', ({projects}) => {
      app.projects = projects;
      app.$forceUpdate();
    });

    messages.on('settings', ({userAuthenticated, analysisEnabled, findingsEnabled, projects}) => {
      if (userAuthenticated !== undefined) {
        app.userAuthenticated = userAuthenticated;
      }
      if (analysisEnabled !== undefined) {
        app.analysisEnabled = analysisEnabled;
      }
      if (findingsEnabled !== undefined) {
        app.findingsEnabled = findingsEnabled;
      }
      if (projects !== undefined) {
        app.projects = projects;
      }
      app.$forceUpdate();
    });
  });

  vscode.postMessage({command: 'ready'});
}
