import { flowRendererV2, flowStyles } from './chunk-P4BUPSA6.js';
import './chunk-6PWNJG2P.js';
import { parser$1, flowDb } from './chunk-QKI3L6FQ.js';
import './chunk-FQJ7IZ6Z.js';
import './chunk-NJI7Y6RC.js';
import './chunk-ZXYWRSNZ.js';
import './chunk-ARI3AIVW.js';
import { require_dist, require_dayjs_min, require_dist2, require_purify, setConfig } from './chunk-YWHJFWTB.js';
import { init_polyfillShim, __toESM } from './chunk-NBJJPFWB.js';

// node_modules/mermaid/dist/flowDiagram-v2-96b9c2cf.js
init_polyfillShim();
__toESM(require_dist(), 1);
__toESM(require_dayjs_min(), 1);
__toESM(require_dist2(), 1);
__toESM(require_purify(), 1);
var diagram = {
  parser: parser$1,
  db: flowDb,
  renderer: flowRendererV2,
  styles: flowStyles,
  init: (cnf) => {
    if (!cnf.flowchart) {
      cnf.flowchart = {};
    }
    cnf.flowchart.arrowMarkerAbsolute = cnf.arrowMarkerAbsolute;
    setConfig({ flowchart: { arrowMarkerAbsolute: cnf.arrowMarkerAbsolute } });
    flowRendererV2.setConf(cnf.flowchart);
    flowDb.clear();
    flowDb.setGen("gen-2");
  }
};

export { diagram };
//# sourceMappingURL=flowDiagram-v2-96b9c2cf-WLX6EVUZ.js.map
//# sourceMappingURL=flowDiagram-v2-96b9c2cf-WLX6EVUZ.js.map