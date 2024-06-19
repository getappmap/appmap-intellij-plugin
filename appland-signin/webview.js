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
      },
      mounted() {
        let emailInput = document.querySelector("#email-input");
        if (emailInput) {
          let focusEventSent = false;
          emailInput.addEventListener("focus", () => {
            if (!focusEventSent) {
              focusEventSent = true;
              setTimeout(() => vscode.postMessage({"command": "email-input-focused"}), 250);
            }
          });
        }
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
