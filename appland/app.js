import Vue from 'vue';
import {default as plugin, VVsCodeExtension} from '@appland/appmap'; // eslint-disable-line import/no-named-default

Vue.use(plugin);

// noinspection JSUnusedLocalSymbols
const app = new Vue(
    {
      el: '#app',
      render: (h) => h(VVsCodeExtension, {ref: 'ui'}),
      methods: {
        async loadAppMap(jsonString) {
          try {
            const appmapData = JSON.parse(jsonString);
            this.$refs.ui.loadData(appmapData);
          }
          catch (e) {
            console.error("error parsing JSON", e);
          }
        }
      },
      mounted() {
        // this is a workaround to notify the plugin that the app is ready
        console.log("intellij-plugin-ready");
      }
    }
)

window.loadAppMap = function (jsonString) {
  app.loadAppMap(jsonString);
};