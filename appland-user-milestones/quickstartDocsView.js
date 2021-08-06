import Vue from 'vue';
import { VQuickstartDocsInstallAgent, VQuickstartDocsOpenAppmaps } from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountQuickstartInstallAgent() {
  const messages = new MessagePublisher(vscode);

  messages
    .on('init', (event) => {
      const app = new Vue({
        el: '#app',
        render(h) {
          return h(VQuickstartDocsInstallAgent, {
            ref: 'ui',
            props: {
              languages: this.languages,
            },
          });
        },
        data() {
          return {
            languages: event.languages,
          };
        },
      });

      vscode.postMessage({ command: 'postInitialize' });
    })
    .on(undefined, (event) => {
      throw new Error(`unhandled message type: ${event.type}`);
    });

  vscode.postMessage({ command: 'preInitialize' });
}

export function mountQuickstartOpenAppmaps() {
  const messages = new MessagePublisher(vscode);

  messages
    .on('init', (event) => {
      const app = new Vue({
        el: '#app',
        render(h) {
          return h(VQuickstartDocsOpenAppmaps, {
            ref: 'ui',
            props: {
              appmaps: this.appmaps,
            },
          });
        },
        data() {
          return {
            appmaps: event.appmaps,
          };
        },
      });

      app.$on('openAppmap', (file) => {
        vscode.postMessage({ command: 'openFile', file });
      });

      messages.on('appmapSnapshot', ({ appmaps }) => {
        app.appmaps = appmaps;
      });

      vscode.postMessage({ command: 'postInitialize' });
    })
    .on(undefined, (event) => {
      throw new Error(`unhandled message type: ${event.type}`);
    });

  vscode.postMessage({ command: 'preInitialize' });
}
