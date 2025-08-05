import Vue from 'vue';
import { default as plugin, VVsCodeExtension } from '@appland/components'; // eslint-disable-line import/no-named-default
import MessagePublisher from 'messagePublisher';
import vscode from 'vsCodeBridge';
import handleAppMapMessages from "handleAppMapMessages";
import '@appland/diagrams/dist/style.css';

function mountWebview() {
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

    handleAppMapMessages(app, vscode, messages);

    app.$on('ask-navie-about-map', filePath => vscode.postMessage({command: 'ask-navie-about-map', mapFsPath: filePath}))

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

Vue.use(plugin);
mountWebview();