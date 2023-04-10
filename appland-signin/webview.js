import Vue from 'vue';
import {VSignIn} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', ({page, data: initData}) => {
    const app = new Vue({
      el: '#app',
      render(h) {
        return h(VSignIn, {
          ref: 'ui',
          props: initData,
        });
      },
      data() {
        return initData;
      }
    });

    app.$on('sign-in', () => vscode.postMessage({command: 'sign-in'}));
  });

  vscode.postMessage({command: 'ready'});
}
