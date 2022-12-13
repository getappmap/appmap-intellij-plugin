import Vue from 'vue';
import {VAnalysisFindings, VFindingDetails} from '@appland/components';
import '@appland/diagrams/dist/style.css';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', ({page, data: initData}) => {
    console.log("received js init: " + page + ", " + JSON.stringify(initData));

    let component;
    if (page === "finding-overview") {
      component = VAnalysisFindings
    } else if (page === "finding-details") {
      component = VFindingDetails;
    } else {
      throw new Error("unknown page type: " + page);
    }

    const app = new Vue({
      el: '#app',
      render(h) {
        return h(component, {
          ref: 'ui',
          props: initData,
        });
      },
      data() {
        return initData;
      }
    });

    // findings overview
    app.$on('open-problems-tab', () => vscode.postMessage({command: 'open-problems-tab'}));
    app.$on('open-finding-info', (hash) => vscode.postMessage({command: 'open-finding-info', hash}));

    // finding details
    app.$on('open-in-source-code', (location) => vscode.postMessage({command: 'open-in-source-code', location}));
    app.$on('open-map', (mapFile, uri) => vscode.postMessage({command: 'open-map', mapFile, uri}));
    app.$on('open-findings-overview', () => vscode.postMessage({command: 'open-findings-overview'}));
  });

  vscode.postMessage({command: 'ready'});
}
