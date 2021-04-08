import Vue from 'vue';
import {default as plugin, VVsCodeExtension} from '@appland/appmap'; // eslint-disable-line import/no-named-default
import config from "./sample.appmap.json"

Vue.use(plugin);

const app = new Vue(
    {
      el: '#app',
      render: (h) => h(VVsCodeExtension, {ref: 'ui'}),
      methods: {
        async loadData(appmapData) {
          this.$refs.ui.loadData(appmapData);
        },
        mounted() {
        },
      }
    }
)

app.loadData(config);