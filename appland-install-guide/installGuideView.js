import Vue from 'vue';
import {VInstallGuide} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountInstallGuide() {
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
            disabledPages: new Set(this.disabledPages),
            featureFlags: new Set(['disable-record-pending']),
            analysisEnabled: this.analysisEnabled,
            findingsEnabled: this.findingsEnabled,
            userAuthenticated: this.userAuthenticated,
          },
        });
      },
      data() {
        return {
          projects: initialData.projects,
          disabledPages: initialData.disabledPages,
          analysisEnabled: initialData.analysisEnabled,
          findingsEnabled: initialData.findingsEnabled,
          userAuthenticated: initialData.userAuthenticated,
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

    app.$on('view-problems', (projectPath) => {
      vscode.postMessage({command: 'view-problems', projectPath});
    });

    app.$on('openAppmap', (file) => {
      vscode.postMessage({command: 'open-file', file});
    });

    app.$on('perform-install', (path, language) => {
      vscode.postMessage({command: 'perform-install', path, language});
    });

    app.$on('open-instruction', (pageId) => {
      app.$refs.ui.jumpTo(pageId);
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

    messages.on('settings', ({userAuthenticated, analysisEnabled, findingsEnabled}) => {
      if (userAuthenticated !== undefined) {
        app.userAuthenticated = userAuthenticated;
      }
      if (analysisEnabled !== undefined) {
        app.analysisEnabled = analysisEnabled;
      }
      if (findingsEnabled !== undefined) {
        app.findingsEnabled = findingsEnabled;
      }
      app.$forceUpdate();
    });
  });

  vscode.postMessage({command: 'ready'});
}
