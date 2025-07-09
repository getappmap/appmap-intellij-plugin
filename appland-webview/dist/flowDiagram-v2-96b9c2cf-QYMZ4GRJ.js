import { flowRendererV2, flowStyles } from './chunk-WMZVKIY3.js';
import { parser$1, flowDb } from './chunk-ARHK3NQL.js';
import './chunk-XJ243IZL.js';
import './chunk-UG4MSKF7.js';
import './chunk-JCOYGTRG.js';
import './chunk-3TO7K2MM.js';
import './chunk-O2SMC75L.js';
import { require_dist, require_dayjs_min, require_dist2, require_purify, setConfig } from './chunk-IKWFAN4T.js';
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
//# sourceMappingURL=flowDiagram-v2-96b9c2cf-QYMZ4GRJ.js.map
//# sourceMappingURL=flowDiagram-v2-96b9c2cf-QYMZ4GRJ.js.map