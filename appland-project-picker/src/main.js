import {createApp} from 'vue'
import App from './App.vue'

const app = createApp(App).mount('#app');

window.loadAppLandProjects = function (projects) {
  app.$data.projects = projects
}

window.loadAppLandProjects([
      {
        "id": 0,
        "score": 4,
        "name": "Spring Test Project",
        "path": "/home/jansorg/spring dir with spaces/project1",
        features: {
          "lang": {
            "title": "Java",
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
      },
      {
        "id": 0,
        "score": 3,
        "name": "Spring Test Project (okay, score 3)",
        "path": "/home/jansorg/spring/project-okay",
        features: {
          "lang": {
            "title": "Ruby",
            "score": 1,
            "text": "Ruby"
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
      },
      {
        "id": 0,
        "score": 1,
        "name": "Other Spring Test Project",
        "path": "/home/jansorg/spring/project-other-spring with space",
        "lang": "ruby",
        features: {
          "lang": {
            "title": "Ruby",
            "score": 1,
            "text": "Ruby"
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
);