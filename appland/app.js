import Vue from 'vue';
import {default as plugin, VVsCodeExtension} from '@appland/components'; // eslint-disable-line import/no-named-default
import '@appland/diagrams/dist/style.css';

Vue.use(plugin);

// noinspection JSUnusedLocalSymbols
const app = new Vue(
    {
      el: '#app',
      render: (h) => h(VVsCodeExtension, {ref: 'ui', props: {appMapUploadable: true}}),
      methods: {
        async loadAppMap(jsonString) {
          try {
            const appmapData = JSON.parse(jsonString);
            this.$refs.ui.loadData(appmapData);
          } catch (e) {
            console.error("error parsing JSON", e);
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

window.showAppMapInstructions = function () {
  console.log("window.showAppMapInstructions");
  app.showInstructions();
};