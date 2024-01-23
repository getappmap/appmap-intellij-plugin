import Vue from 'vue';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';
import {VChatSearch} from '@appland/components';
import handleAppMapMessages from "handleAppMapMessages";

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on('init', (initialData) => {
    const app = new Vue({
      el: '#app',
      render(h) {
        return h(VChatSearch, {
          ref: 'ui',
          props: {
            apiKey: initialData.apiKey,
            appmapRpcPort: initialData.appmapRpcPort,
            question: initialData.question,
            savedFilters: initialData.savedFilters,
          },
        });
      },
      methods: {
        getAppMapState() {
          return this.$refs.ui.getAppMapState();
        },
        setAppMapState(state) {
          this.$refs.ui.setAppMapState(state);
        },
        updateFilters(updatedSavedFilters) {
          this.$refs.ui.updateFilters(updatedSavedFilters);
        },
      },
    });

    handleAppMapMessages(app, vscode, messages);
  });

  vscode.postMessage({command: 'ready'});
}