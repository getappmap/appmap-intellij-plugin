import {createApp} from 'vue'
import App from './App.vue'

const app = createApp(App).mount('#app');

window.loadAppLandProjects = function (projects) {
  app.$data.projects = projects
}

/*window.loadAppLandProjects([
      {
        "id": 0,
        "score": 1,
        "name": "Spring Test Project",
        features: {
          "lang": {
            "title": "Language",
            "score": 1,
            "text": "Java"
          },
          "test": {
            "title": "Test Framework",
            "score": 1,
            "text": "JUnit"
          },
          "_web": {
            "title": "Web Framework",
            "score": 1,
            "text": "Spring Boot"
          },
        }
      }
    ]
)*/;