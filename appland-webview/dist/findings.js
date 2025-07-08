import { vue_runtime_default, rs, MessagePublisher, vsCodeBridge_default, pi, hi } from './chunk-HCS4X2XG.js';
import './chunk-SDHD3UEE.js';
import './chunk-YWHJFWTB.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// findings.js
init_polyfillShim();
function mountWebview() {
  const messages = new MessagePublisher(vsCodeBridge_default);
  messages.on("init", ({ page, data: initData }) => {
    console.log("received js init: " + page + ", " + JSON.stringify(initData));
    let component;
    if (page === "finding-overview") {
      component = pi;
    } else if (page === "finding-details") {
      component = hi;
    } else {
      throw new Error("unknown page type: " + page);
    }
    const app = new vue_runtime_default({
      el: "#app",
      render(h) {
        return h(component, {
          ref: "ui",
          props: initData
        });
      },
      data() {
        return initData;
      }
    });
    app.$on("open-problems-tab", () => vsCodeBridge_default.postMessage({ command: "open-problems-tab" }));
    app.$on("open-finding-info", (hash) => vsCodeBridge_default.postMessage({ command: "open-finding-info", hash }));
    app.$on("open-in-source-code", (location) => vsCodeBridge_default.postMessage({ command: "open-in-source-code", location }));
    app.$on("open-map", (mapFile, uri) => vsCodeBridge_default.postMessage({ command: "open-map", mapFile, uri }));
    app.$on("open-findings-overview", () => vsCodeBridge_default.postMessage({ command: "open-findings-overview" }));
  });
  vsCodeBridge_default.postMessage({ command: "ready" });
}
vue_runtime_default.use(rs);
mountWebview();
//# sourceMappingURL=findings.js.map
//# sourceMappingURL=findings.js.map