<script setup>
defineOptions({ name: 'MainLayout' })

/**
 * Envoltorio de la zona pública.
 *
 * Ya no usa q-layout / q-page-container: la landing coloca sus propias secciones
 * a pantalla completa y el contenedor de Quasar le metía un relleno superior
 * calculado para una q-header que aquí no existe, dejando una franja vacía sobre
 * el héroe.
 */
</script>

<template>
  <div class="capa-publica">
    <RouterView />
  </div>
</template>

<style scoped>
.capa-publica {
  min-height: 100vh;
  background: var(--ink);
}
</style>
