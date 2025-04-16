import Vue from "vue";
import MessagePublisher from "messagePublisher";
import vscode from "vsCodeBridge";
import handleAppMapMessages from "handleAppMapMessages";
import {VChatSearch} from "@appland/components";

import "@appland/diagrams/dist/style.css";
import "highlight.js/styles/base16/snazzy.css";

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on("init", (initialData) => {
    const app = new Vue({
      el: '#app',
      render(h) {
        // follows https://github.com/getappmap/vscode-appland/blob/develop/web/src/chatSearchView.js
        return h(VChatSearch, {
          ref: 'ui',
          props: {
            appmapRpcPort: initialData.appmapRpcPort,
            savedFilters: initialData.savedFilters,
            apiKey: initialData.apiKey,
            mostRecentAppMaps: this.mostRecentAppMaps,
            appmapYmlPresent: this.appmapYmlPresent,
            targetAppmapData: initialData.targetAppmapData,
            targetAppmapFsPath: initialData.targetAppmapFsPath,
            editorType: 'intellij',
            useAnimation: initialData.useAnimation,
            preselectedModelId: initialData.preselectedModelId,
            threadId: initialData.threadId,
            openNewChat() {
              vscode.postMessage({command: "open-new-chat"});
            },
          },
        });
      },
      data: {
        mostRecentAppMaps: initialData.mostRecentAppMaps || [],
        appmapYmlPresent: initialData.appmapYmlPresent,
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
      mounted() {
        if (initialData.codeSelection) {
          this.$refs.ui.includeCodeSelection(initialData.codeSelection);
        }
        if (initialData.suggestion) {
          this.$refs.ui.$refs.vchat.addUserMessage(initialData.suggestion.label);
          this.$refs.ui.sendMessage(initialData.suggestion.prompt);
        }
      },
    });

    handleAppMapMessages(app, vscode, messages);

    messages.on('pin-files', (props) => {
      const {requests} = props;
      app.$root.$emit('pin-files', requests);
    });

    messages.on('update', (props) => {
      Object.entries(props)
          .filter(([key]) => key !== 'type')
          .forEach(([key, value]) => {
            if (key in app.$data && app[key] !== value) {
              app[key] = value;
            }
          });
    });

    messages
        .on('navie-restarting', () => {
          app.$refs.ui.onNavieRestarting();
        })
        .on('navie-restarted', async () => {
          /* eslint-disable no-console */
          app.$refs.ui.loadNavieConfig().catch((e) => console.error(e));
          app.$refs.ui.loadModelConfig().catch((e) => console.error(e));

          // Request the model list a few times to make sure it has time to fully load
          for (let i = 0; i < 5; i += 1) {
            app.$refs.ui.initializeModels().catch((e) => console.error(e));

            // eslint-disable-next-line no-await-in-loop
            await new Promise((resolve) => setTimeout(resolve, (i + 1) * 2000));
          }
          /* eslint-enable no-console */
        });

    app.$on('choose-files-to-pin', () => vscode.postMessage({ command: 'choose-files-to-pin' }));
    app.$on('click-link', (link) => vscode.postMessage({command: 'click-link', link}))
    app.$on('open-install-instructions', () => vscode.postMessage({command: 'open-install-instructions'}))
    app.$on('open-record-instructions', () => vscode.postMessage({command: 'open-record-instructions'}))
    app.$on('open-appmap', (path) => vscode.postMessage({command: 'open-appmap', path}))
    app.$on('open-location', (path, directory) => vscode.postMessage({command: 'open-location', path, directory}))
    app.$on('save-message', (message) => vscode.postMessage({command: 'save-message', ...message}))
    app.$on('select-llm-option', (choice) => vscode.postMessage({command: 'select-llm-option', choice}));
    app.$on('show-appmap-tree', () => vscode.postMessage({command: 'show-appmap-tree'}));
    app.$on('change-model-config', ({key, value, secret}) => vscode.postMessage({command: 'change-model-config', key, value, secret}));
    app.$on("select-model", ({provider,id}) => vscode.postMessage({command: "select-model", "id": `${provider}:${id}`}));
  });

  vscode.postMessage({ command: "ready" });
}
