import { handleAppMapMessages } from './chunk-IFYTZTYP.js';
import './chunk-TNT5476E.js';
import { vue_runtime_default, Zs, MessagePublisher, vsCodeBridge_default, Bi } from './chunk-BIQ4AVON.js';
import './chunk-SDHD3UEE.js';
import './chunk-IKWFAN4T.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// appmap.js
init_polyfillShim();
function mountWebview() {
  const messages = new MessagePublisher(vsCodeBridge_default);
  function doMount(properties, appMapContent) {
    let app = new vue_runtime_default(
      {
        el: "#app",
        render: (h) => h(Bi, { ref: "ui", props: properties }),
        mounted() {
          vsCodeBridge_default.postMessage({ command: "webviewMounted" });
        },
        methods: {
          async loadAppMap(appmapData) {
            try {
              this.$refs.ui.loadData(appmapData);
            } catch (e) {
              console.error("error parsing JSON", e);
            }
          },
          async setState(jsonString) {
            try {
              this.$refs.ui.setState(jsonString);
            } catch (e) {
              console.error("error setting state: " + jsonString, e);
            }
          },
          showInstructions() {
            this.$refs.ui.showInstructions();
          },
          updateSavedFilters(filters) {
            this.$refs.ui.updateFilters(filters);
          }
        }
      }
    );
    handleAppMapMessages(app, vsCodeBridge_default, messages);
    app.$on("ask-navie-about-map", (filePath) => vsCodeBridge_default.postMessage({ command: "ask-navie-about-map", mapFsPath: filePath }));
    if (appMapContent) {
      setTimeout(() => {
        app.loadAppMap(appMapContent);
      }, 0);
    }
  }
  messages.on("init", ({ data: initData, props }) => {
    doMount(props || {}, initData);
  });
  vsCodeBridge_default.postMessage({ command: "ready" });
}
vue_runtime_default.use(Zs);
mountWebview();
//# sourceMappingURL=appmap.js.map
//# sourceMappingURL=appmap.js.map