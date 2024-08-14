import Vue from 'vue';
import {VInstallGuide} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from 'messagePublisher';
import vscode from 'vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', (initialData) => {
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
            findingsEnabled: this.findingsEnabled,
          },
        });
      },
      data() {
        return {
          projects: initialData.projects,
          analysisEnabled: initialData.analysisEnabled,
          userAuthenticated: initialData.userAuthenticated,
          findingsEnabled: initialData.findingsEnabled,
        };
      },
      mounted() {
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
        project: currentProject,
        text,
      });
    });

    app.$on('select-project', (project) => {
      currentProject = project;
    });

    app.$on('open-navie', () => {
      vscode.postMessage({command: 'open-navie'});
    });

    app.$on('open-findings-overview', () => {
      vscode.postMessage({command: 'open-findings-overview'});
    });

    app.$on('openAppmap', (file) => {
      vscode.postMessage({command: 'open-file', file});
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

    app.$on('submit-to-navie', (suggestion) => {
      vscode.postMessage({command: 'submit-to-navie', suggestion});
    });

    // listeners for messages sent by the plugin
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
