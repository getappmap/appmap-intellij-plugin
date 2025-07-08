import { vue_runtime_default, rs, MessagePublisher, vsCodeBridge_default, is } from './chunk-HCS4X2XG.js';
import './chunk-SDHD3UEE.js';
import './chunk-YWHJFWTB.js';
import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// install-guide.js
init_polyfillShim();
function mountWebview() {
  const messages = new MessagePublisher(vsCodeBridge_default);
  messages.on("init", (initialData) => {
    let currentProject;
    const app = new vue_runtime_default({
      el: "#app",
      render(h) {
        return h(is, {
          ref: "ui",
          props: {
            projects: this.projects,
            editor: "jetbrains",
            analysisEnabled: this.analysisEnabled,
            userAuthenticated: this.userAuthenticated,
            featureFlags: /* @__PURE__ */ new Set(["disable-record-pending"]),
            findingsEnabled: this.findingsEnabled
          }
        });
      },
      data() {
        return {
          projects: initialData.projects,
          analysisEnabled: initialData.analysisEnabled,
          userAuthenticated: initialData.userAuthenticated,
          findingsEnabled: initialData.findingsEnabled
        };
      },
      mounted() {
        document.addEventListener("click", (e) => {
          if (e && e.target) {
            const href = e.target.href;
            if (href && (href.startsWith("http://") || href.startsWith("https://"))) {
              e.preventDefault();
              vsCodeBridge_default.postMessage({ command: "click-link", uri: e.target.href });
              return false;
            }
          }
        });
      }
    });
    app.$on("clipboard", (text) => {
      vsCodeBridge_default.postMessage({
        command: "clipboard",
        project: currentProject,
        text
      });
    });
    app.$on("select-project", (project) => {
      currentProject = project;
    });
    app.$on("open-navie", () => {
      vsCodeBridge_default.postMessage({ command: "open-navie" });
    });
    app.$on("open-findings-overview", () => {
      vsCodeBridge_default.postMessage({ command: "open-findings-overview" });
    });
    app.$on("openAppmap", (file) => {
      vsCodeBridge_default.postMessage({ command: "open-file", file });
    });
    app.$on("perform-install", (path, language) => {
      vsCodeBridge_default.postMessage({ command: "perform-install", path, language });
    });
    app.$on("generate-openapi", (projectPath) => {
      messages.rpc("generate-openapi", { projectPath });
    });
    app.$on("perform-auth", () => {
      vsCodeBridge_default.postMessage({ command: "perform-auth" });
    });
    app.$on("submit-to-navie", (suggestion) => {
      vsCodeBridge_default.postMessage({ command: "submit-to-navie", suggestion });
    });
    messages.on("projects", ({ projects }) => {
      app.projects = projects;
      app.$forceUpdate();
    });
    messages.on("settings", ({ userAuthenticated, analysisEnabled, findingsEnabled, projects }) => {
      if (userAuthenticated !== void 0) {
        app.userAuthenticated = userAuthenticated;
      }
      if (analysisEnabled !== void 0) {
        app.analysisEnabled = analysisEnabled;
      }
      if (findingsEnabled !== void 0) {
        app.findingsEnabled = findingsEnabled;
      }
      if (projects !== void 0) {
        app.projects = projects;
      }
      app.$forceUpdate();
    });
  });
  vsCodeBridge_default.postMessage({ command: "ready" });
}
vue_runtime_default.use(rs);
mountWebview();
//# sourceMappingURL=install-guide.js.map
//# sourceMappingURL=install-guide.js.map