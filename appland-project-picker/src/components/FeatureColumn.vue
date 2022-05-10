<template>
    <td v-if="feature" :class="feature.score" :title="feature.text">{{ feature.title || '-' }}</td>
    <td v-else class="bad">—</td>
</template>

<script>
export default {
    name: 'FeatureColumn',
    props: ['feature']
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
