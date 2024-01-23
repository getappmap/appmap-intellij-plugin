/**
 * Installs message handling for the messages supported by the AppMap view.
 */
export default function handleAppMapMessages(app, vscode, messages) {
  // messages emitted by the webview
  app.$on('viewSource', (location) => vscode.postMessage({command: 'viewSource', location}))
  app.$on('clearSelection', () => vscode.postMessage({command: 'clearSelection'}))
  app.$on('uploadAppmap', () => vscode.postMessage({command: 'uploadAppMap'}))
  app.$on('sidebarSearchFocused', () => vscode.postMessage({command: 'sidebarSearchFocused'}))
  app.$on('clickFilterButton', () => vscode.postMessage({command: 'clickFilterButton'}))
  app.$on('clickTab', (tabId) => vscode.postMessage({command: 'clickTab', tabId}))
  app.$on('selectObjectInSidebar', (category) => vscode.postMessage({command: 'selectObjectInSidebar', category}))
  app.$on('resetDiagram', () => vscode.postMessage({command: 'resetDiagram'}))
  app.$on('exportSVG', (svgString) => vscode.postMessage({command: 'exportSVG', svgString}))
  app.$on('request-resolve-location', (location) => {
    app.$emit('response-resolve-location', {location, externalUrl: location});
  });
  app.$on('saveFilter', (filter) => vscode.postMessage({command: 'saveFilter', filter}))
  app.$on('deleteFilter', (filter) => vscode.postMessage({command: 'deleteFilter', filter}))
  app.$on('defaultFilter', (filter) => vscode.postMessage({command: 'defaultFilter', filter}))

  // messages emitted by the Java host
  messages.on('loadAppMap', ({data}) => app.loadAppMap(data))
  messages.on('setAppMapState', (json) => app.setState(json.state))
  messages.on('showAppMapInstructions', () => app.showInstructions())
  messages.on('updateSavedFilters', ({data}) => app.updateSavedFilters(data))
}
