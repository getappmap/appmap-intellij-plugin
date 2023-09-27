import Vue from 'vue';
import '@appland/diagrams/dist/style.css';
import {VVsCodeExtension} from '@appland/components';
import MessagePublisher from './messagePublisher';
import vscode from './vsCodeBridge';

export function mountWebview() {
  const messages = new MessagePublisher(vscode);

  /**
   * Mount the Vue application with the properties passed to this function.
   * Loads AppMap content if it was passed to this function.
   * @param properties Initial properties for the Vue component
   * @param appMapContent JSON   content of the AppMap file
   */
  function doMount(properties, appMapContent) {
    let app = new Vue(
        {
          el: '#app',
          render: (h) => h(VVsCodeExtension, {ref: 'ui', props: properties}),
          mounted() {
            // tell the Java host to apply the initial state
            vscode.postMessage({command: 'webviewMounted'});
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
            },
          }
        }
    )

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

    if (appMapContent) {
      // queue loadAppMap to avoid applying it immediately (which would turn off the progress spinner)
      setTimeout(() => {
        app.loadAppMap(appMapContent);
      }, 0);
    }
  }

  // called by the Java host after it received "ready", which is sent below
  messages.on('init', ({data: initData, props}) => {
    doMount(props || {}, initData)
  })

  // "ready" activates the Java host, which then sends the init message
  vscode.postMessage({command: 'ready'});
}
