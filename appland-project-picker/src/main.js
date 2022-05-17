import {createApp} from 'vue'
import App from './App.vue'

const app = createApp(App).mount('#app');

window.loadAppLandProjects = function (projects) {
  app.$data.projects = projects
}