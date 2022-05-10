<template>
    <ProjectPicker :projects="projects"/>
</template>

<script>
import ProjectPicker from './components/ProjectPicker'

export default {
    name: 'App',
    components: {
        ProjectPicker,
    },
    mounted() {
        // this is a workaround to notify the plugin that the app is ready
        console.log("intellij-plugin-ready");
    },
    data() {
        return {
            projects: []
        }
    }
}
</script>

<style>
#app {

}
</style>
