import { handleAppMapMessages } from './chunk-IFYTZTYP.js';
import './chunk-TNT5476E.js';
import { vue_runtime_default, Zs, MessagePublisher, vsCodeBridge_default, Xn } from './chunk-BIQ4AVON.js';
import './chunk-SDHD3UEE.js';
import './chunk-IKWFAN4T.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// navie.js
init_polyfillShim();
function mountWebview() {
  const messages = new MessagePublisher(vsCodeBridge_default);
  messages.on("init", (initialData) => {
    const app = new vue_runtime_default({
      el: "#app",
      render(h) {
        return h(Xn, {
          ref: "ui",
          props: {
            appmapRpcPort: initialData.appmapRpcPort,
            savedFilters: initialData.savedFilters,
            apiKey: initialData.apiKey,
            mostRecentAppMaps: this.mostRecentAppMaps,
            appmapYmlPresent: this.appmapYmlPresent,
            targetAppmapData: initialData.targetAppmapData,
            targetAppmapFsPath: initialData.targetAppmapFsPath,
            editorType: "intellij",
            useAnimation: initialData.useAnimation,
            preselectedModelId: initialData.preselectedModelId,
            threadId: initialData.threadId,
            openNewChat() {
              vsCodeBridge_default.postMessage({ command: "open-new-chat" });
            }
          }
        });
      },
      data: {
        mostRecentAppMaps: initialData.mostRecentAppMaps || [],
        appmapYmlPresent: initialData.appmapYmlPresent
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
        }
      },
      mounted() {
        this.$root.$once("on-thread-subscription", () => {
          if (initialData.codeSelection)
            this.$refs.ui.includeCodeSelection(initialData.codeSelection);
          if (initialData.suggestion) this.$refs.ui.sendMessage(initialData.suggestion.prompt);
        });
      }
    });
    handleAppMapMessages(app, vsCodeBridge_default, messages);
    messages.on("pin-files", (props) => {
      const { requests } = props;
      app.$root.$emit("pin-files", requests);
    });
    messages.on("update", (props) => {
      Object.entries(props).filter(([key]) => key !== "type").forEach(([key, value]) => {
        if (key in app.$data && app[key] !== value) {
          app[key] = value;
        }
      });
    });
    messages.on("navie-restarting", () => {
      app.$refs.ui.onNavieRestarting();
    }).on("navie-restarted", async () => {
      app.$refs.ui.loadNavieConfig().catch((e) => console.error(e));
      app.$refs.ui.loadModelConfig().catch((e) => console.error(e));
      for (let i = 0; i < 5; i += 1) {
        app.$refs.ui.initializeModels().catch((e) => console.error(e));
        await new Promise((resolve) => setTimeout(resolve, (i + 1) * 2e3));
      }
    });
    app.$on("choose-files-to-pin", () => vsCodeBridge_default.postMessage({ command: "choose-files-to-pin" }));
    app.$on("click-link", (link) => vsCodeBridge_default.postMessage({ command: "click-link", link }));
    app.$on("open-install-instructions", () => vsCodeBridge_default.postMessage({ command: "open-install-instructions" }));
    app.$on("open-record-instructions", () => vsCodeBridge_default.postMessage({ command: "open-record-instructions" }));
    app.$on("open-appmap", (path) => vsCodeBridge_default.postMessage({ command: "open-appmap", path }));
    app.$on("open-location", (path, directory) => vsCodeBridge_default.postMessage({ command: "open-location", path, directory }));
    app.$on("save-message", (message) => vsCodeBridge_default.postMessage({ command: "save-message", ...message }));
    app.$on("select-llm-option", (choice) => vsCodeBridge_default.postMessage({ command: "select-llm-option", choice }));
    app.$on("show-appmap-tree", () => vsCodeBridge_default.postMessage({ command: "show-appmap-tree" }));
    app.$on(
      "change-model-config",
      ({ key, value, secret }) => vsCodeBridge_default.postMessage({ command: "change-model-config", key, value, secret })
    );
    app.$on(
      "select-model",
      ({ provider, id }) => vsCodeBridge_default.postMessage({ command: "select-model", id: `${provider}:${id}` })
    );
  });
  vsCodeBridge_default.postMessage({ command: "ready" });
}
vue_runtime_default.use(Zs);
mountWebview();

export { mountWebview };
//# sourceMappingURL=navie.js.map
//# sourceMappingURL=navie.js.map