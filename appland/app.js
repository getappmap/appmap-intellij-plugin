import Vue from 'vue';
import {default as plugin, VVsCodeExtension} from '@appland/components'; // eslint-disable-line import/no-named-default
import '@appland/diagrams/dist/style.css';

Vue.use(plugin);

// noinspection JSUnusedLocalSymbols
const app = new Vue(
    {
      el: '#app',
      render: (h) => h(VVsCodeExtension, {ref: 'ui', props: {appMapUploadable: false}}),
      methods: {
        async loadAppMap(jsonString) {
          try {
            const appmapData = JSON.parse(jsonString);
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
      },
      mounted() {
        // this is a workaround to notify the plugin that the app is ready
        console.log("intellij-plugin-ready");
      }
    }
)

app.$on('viewSource', (location) => {
  console.log("calling viewSource callback for " + location);
  window.AppLand.viewSource(location);
});

app.$on('uploadAppmap', () => {
  console.log("calling uploadAppmap callback");
  window.AppLand.uploadAppmap();
});

window.loadAppMap = function (jsonString) {
  console.log("window.loadAppMap");
  app.loadAppMap(jsonString);
};

window.setAppMapState = function (json) {
  console.debug("window.setAppMapState");
  app.setState(json);
};

window.showAppMapInstructions = function () {
  console.log("window.showAppMapInstructions");
  app.showInstructions();
};