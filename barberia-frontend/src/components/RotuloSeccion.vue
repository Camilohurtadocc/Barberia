<script setup>
/**
 * Rótulo numerado que encabeza cada sección: «01 ——————— SERVICIOS».
 *
 * La numeración no es decorativa: da al visitante una idea de cuánto queda por
 * recorrer en una página que es un scroll largo y sin paginación.
 */
defineProps({
  numero: { type: String, required: true },
  texto: { type: String, required: true },
})
</script>

<template>
  <div class="bs-rotulo">
    <span class="bs-rotulo__n">{{ numero }}</span>
    <div class="bs-rotulo__linea" />
    <span class="bs-rotulo__texto">{{ texto }}</span>
  </div>
</template>
