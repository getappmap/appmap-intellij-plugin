<template>
    <ProjectPicker :rows="rows"/>
</template>

<script>
import ProjectPicker from './components/ProjectPicker'

export default {
    name: 'App',
    components: {
        ProjectPicker,
    },
    data() {
        return {
            rows: [
                {
                    "id": 0,
                    "name": "Spring Test Project",
                    "testFramework": "JUnit",
                    "webFramework": "Spring Boot",
                },
                {
                    "id": 1,
                    "name": "Spring Pet Clinic",
                    "testFramework": "NGUnit",
                    "webFramework": "Tomcat",
                }
            ]
        }
    }
}
</script>

<style>
#app {

}
</style>
