import { vue_runtime_default, rs, MessagePublisher, vsCodeBridge_default, mi } from './chunk-HCS4X2XG.js';
import './chunk-SDHD3UEE.js';
import './chunk-YWHJFWTB.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// signin.js
init_polyfillShim();
function mountWebview() {
  const messages = new MessagePublisher(vsCodeBridge_default);
  messages.on("init", ({ page, data: initData }) => {
    const app = new vue_runtime_default({
      el: "#app",
      render(h) {
        return h(mi, {
          ref: "ui",
          props: initData
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
              setTimeout(() => vsCodeBridge_default.postMessage({ command: "email-input-focused" }), 250);
            }
          });
        }
      }
    });
    app.$on("sign-in", () => {
      vsCodeBridge_default.postMessage({ command: "sign-in" });
    });
    app.$on("click-sign-in-link", (linkType) => {
      vsCodeBridge_default.postMessage({ command: "click-sign-in-link", linkType });
    });
    app.$on("activate", (apiKey) => {
      vsCodeBridge_default.postMessage({ command: "activate", apiKey });
    });
  });
  vsCodeBridge_default.postMessage({ command: "ready" });
}
vue_runtime_default.use(rs);
mountWebview();
//# sourceMappingURL=signin.js.map
//# sourceMappingURL=signin.js.map