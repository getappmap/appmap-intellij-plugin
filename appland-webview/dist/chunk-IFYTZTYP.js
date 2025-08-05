import { init_polyfillShim } from './chunk-NBJJPFWB.js';

// handleAppMapMessages.js
init_polyfillShim();
function handleAppMapMessages(app, vscode, messages) {
  app.$on("viewSource", (location) => vscode.postMessage({ command: "viewSource", location }));
  app.$on("clearSelection", () => vscode.postMessage({ command: "clearSelection" }));
  app.$on("uploadAppmap", () => vscode.postMessage({ command: "uploadAppMap" }));
  app.$on("sidebarSearchFocused", () => vscode.postMessage({ command: "sidebarSearchFocused" }));
  app.$on("clickFilterButton", () => vscode.postMessage({ command: "clickFilterButton" }));
  app.$on("clickTab", (tabId) => vscode.postMessage({ command: "clickTab", tabId }));
  app.$on("selectObjectInSidebar", (category) => vscode.postMessage({ command: "selectObjectInSidebar", category }));
  app.$on("resetDiagram", () => vscode.postMessage({ command: "resetDiagram" }));
  app.$on("exportSVG", (svgString) => vscode.postMessage({ command: "exportSVG", svgString }));
  app.$on("exportJSON", (appmapData) => vscode.postMessage({ command: "exportJSON", appmapData }));
  app.$on("request-resolve-location", (location) => {
    app.$emit("response-resolve-location", { location, externalUrl: location });
  });
  app.$on("saveFilter", (filter) => vscode.postMessage({ command: "saveFilter", filter }));
  app.$on("deleteFilter", (filter) => vscode.postMessage({ command: "deleteFilter", filter }));
  app.$on("defaultFilter", (filter) => vscode.postMessage({ command: "defaultFilter", filter }));
  messages.on("loadAppMap", ({ data }) => app.loadAppMap(data));
  messages.on("setAppMapState", (json) => app.setState(json.state));
  messages.on("showAppMapInstructions", () => app.showInstructions());
  messages.on("updateSavedFilters", ({ data }) => app.updateSavedFilters(data));
}

export { handleAppMapMessages };
//# sourceMappingURL=chunk-IFYTZTYP.js.map
//# sourceMappingURL=chunk-IFYTZTYP.js.map