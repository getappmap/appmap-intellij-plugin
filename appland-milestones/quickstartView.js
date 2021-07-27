import Vue from 'vue';
import { VQuickstart } from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';

const STEPS = {
  1: 'INSTALL_AGENT',
  2: 'CREATE_CONFIGURATION',
  3: 'RECORD_APPMAP',
};

export default function mountApp() {
  const vscode = {
      // fixme
      "postMessage": function(data) {
          if (data) {
              console.warn(JSON.stringify(data));
          }

          if (data && data.command === 'ready') {
              window.postMessage({
                  type:'init',
                  data: {
                      language: 'Ruby',
                      completedSteps: []
                  }
              });
          } else {
              console.warn("POSTING "+ data.command)
              window.postMessage({
                  type: data.command,
                  data: data.data
              })
          }
      }
  };
  const messages = new MessagePublisher(vscode);
  messages
    .on('init', (event) => {
      console.warn("--> INIT")
      const { language: detectedLanguage, completedSteps } = event;
      const app = new Vue({
        el: '#app',
        render: (h) =>
          h(VQuickstart, {
            ref: 'ui',
            props: {
              /*steps: [
                {
                  state:''
                },
                {
                  state:''
                }
              ],*/
              completedSteps,
              language: detectedLanguage,
              async onAction(language, step) {
                const milestone = STEPS[step];
                if (!milestone) {
                  return true;
                }

                await messages.rpc('milestoneAction', { language, milestone });
                return true;
              },
            },
          }),
      });
    })
    .on(undefined, (event) => {
      throw new Error(`unhandled message type: ${event.type}`);
    });

    vscode.postMessage({ command: 'ready' });
}