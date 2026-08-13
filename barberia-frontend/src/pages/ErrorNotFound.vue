<script setup>
defineOptions({ name: 'ErrorNotFound' })
</script>

<template>
  <div class="perdido">
    <div class="perdido__contenido">
      <div class="bs-rotulo">
        <span class="bs-rotulo__n">404</span>
        <div class="bs-rotulo__linea" />
        <span class="bs-rotulo__texto">Página no encontrada</span>
      </div>

      <h1 class="bs-h2 perdido__titulo">
        Aquí no<br /><em class="bs-em">hay nada.</em>
      </h1>

      <p class="bs-parrafo perdido__texto">
        La dirección que has abierto no existe o ha dejado de estar disponible.
      </p>

      <RouterLink to="/" class="bs-btn bs-btn--oro">
        Volver al inicio
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.perdido {
  min-height: 100vh;
  background: var(--ink);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.perdido__contenido {
  max-width: 460px;
  width: 100%;
}

.perdido__titulo {
  margin: 0 0 20px;
}

.perdido__texto {
  margin: 0 0 32px;
}
</style>
