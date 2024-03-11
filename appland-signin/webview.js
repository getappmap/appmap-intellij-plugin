import Vue from 'vue';
import {VSidebarSignIn} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from 'messagePublisher';
import vscode from 'vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', ({page, data: initData}) => {
    const app = new Vue({
      el: '#app',
      render(h) {
        return h(VSidebarSignIn, {
          ref: 'ui',
          props: initData,
        });
      },
      data() {
        return initData;
      }
    });

    app.$on('sign-in', () => {
      vscode.postMessage({command: 'sign-in'})
    });
    app.$on('click-sign-in-link', (linkType) => {
      vscode.postMessage({command: 'click-sign-in-link', linkType})
    });
    app.$on('activate', (apiKey) => {
      vscode.postMessage({command: 'activate', apiKey})
    });
  });

  vscode.postMessage({command: 'ready'});
}
