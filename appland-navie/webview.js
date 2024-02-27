import Vue from "vue";
import MessagePublisher from "messagePublisher";
import vscode from "vsCodeBridge";
import handleAppMapMessages from "handleAppMapMessages";
import { VChatSearch } from "@appland/components";

import "@appland/diagrams/dist/style.css";
import "highlight.js/styles/base16/snazzy.css";

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  messages.on("init", (initialData) => {
    const app = new Vue({
      el: "#app",
      render(h) {
        return h(VChatSearch, {
          ref: "ui",
          props: {
            apiKey: initialData.apiKey,
            appmapRpcPort: initialData.appmapRpcPort,
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
      mounted() {
        if (initialData.codeSelection) {
          this.$refs.ui.includeCodeSelection(initialData.codeSelection);
        }
      },
    });

    handleAppMapMessages(app, vscode, messages);

    app.$on('show-appmap-tree', () => vscode.postMessage({command: 'show-appmap-tree'}));
  });

  vscode.postMessage({ command: "ready" });
}
