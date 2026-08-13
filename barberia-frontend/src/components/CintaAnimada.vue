<script setup>
import { computed } from 'vue'

/**
 * Cinta dorada en bucle. Aparece dos veces en la página: bajo la navegación y
 * justo encima del pie, cerrando el recorrido con el mismo gesto con que empieza.
 */
const props = defineProps({
  /** Frases que edita el administrador desde Sitio. */
  mensajes: { type: Array, default: () => [] },
  /** Texto extra que se añade al final, por ejemplo el horario de hoy. */
  sufijo: { type: String, default: '' },
})

const PREDETERMINADOS = [
  'Cortes',
  'Barba',
  'Fade',
  'Diseños',
  'Tratamientos',
  'Bogotá',
  'Est. 2012',
]

/**
 * Termina siempre en separador para que, al encadenarse con su propia copia, no
 * queden dos frases pegadas justo en la costura del bucle.
 */
const texto = computed(() => {
  const frases = props.mensajes.filter(Boolean)
  const base = frases.length ? frases : PREDETERMINADOS
  const completo = props.sufijo ? [...base, props.sufijo] : base
  return completo.join(' · ') + ' · '
})
</script>

<template>
  <!--
    El texto va TRES veces porque la animación desplaza la pista un -33.33%: al
    terminar, la segunda copia está exactamente donde arrancó la primera y el
    salto es invisible. Con dos copias el bucle da un tirón a mitad de recorrido
    en pantallas anchas, donde el texto no llega a llenar el ancho.
  -->
  <div class="bs-cinta" aria-hidden="true">
    <div class="bs-cinta__pista">
      <span v-for="n in 3" :key="n" class="bs-cinta__texto">{{ texto }}</span>
    </div>
  </div>
</template>
