import { vue_runtime_default, Zs, MessagePublisher, vsCodeBridge_default, Ks, Gs } from './chunk-BIQ4AVON.js';
import './chunk-SDHD3UEE.js';
import './chunk-IKWFAN4T.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// review.ts
init_polyfillShim();
var REVIEW = void 0;
vue_runtime_default.use(Zs);
var EVENTS = ["open-location", "show-navie-thread"];
var messages = new MessagePublisher(vsCodeBridge_default);
messages.on("init", ({ rpcPort, baseRef }) => {
  const app = new vue_runtime_default({
    el: "#app",
    render: (h) => h(Ks, {
      ref: "review"
    })
  });
  const reviewComponent = app.$refs.review;
  for (const event of EVENTS) app.$on(event, (...args) => vsCodeBridge_default.postMessage({ command: event, args }));
  const backend = new Gs(reviewComponent, { rpcPort, ...REVIEW });
  backend.startReview(baseRef);
});
vsCodeBridge_default.postMessage({ command: "ready" });
//# sourceMappingURL=review.js.map
//# sourceMappingURL=review.js.map