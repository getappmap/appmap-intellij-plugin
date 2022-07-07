import Vue from 'vue';
import {VInstallGuide} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountInstallGuide() {
  console.log("mountInstallGuide")
  const messages = new MessagePublisher(vscode);

  messages.on('init', ({projects: startProjects, page: startPage, disabled}) => {
    console.log("app: init");
    let currentPage = startPage;
    let currentProject;

    const app = new Vue({
      el: '#app',
      render(h) {
        console.log("app: rendering: " + JSON.stringify(this.projects));
        return h(VInstallGuide, {
          ref: 'ui',
          props: {
            projects: this.projects,
            disabledPages: new Set(disabled),
            editor: 'jetbrains',
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
          console.log("app: open-page: " + pageId + ", project: " + currentProject);

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
        console.log("mounted: "+ startPage);

        document.querySelectorAll('a[href]').forEach((el) => {
          el.addEventListener('click', (e) => {
            vscode.postMessage({command: 'click-link', uri: e.target.href});
          });
        });
        this.$refs.ui.jumpTo(startPage);
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
      messages.rpc('view-problems', projectPath);
    });

    app.$on('openAppmap', (file) => {
      vscode.postMessage({command: 'open-file', file});
    });

    messages.on('page', ({page}) => {
      app.$refs.ui.jumpTo(page);
    });

    messages.on('projects', ({projects}) => {
      app.projects = projects;
      app.$forceUpdate();
    });
  });

  vscode.postMessage({command: 'preInitialize'});
}
