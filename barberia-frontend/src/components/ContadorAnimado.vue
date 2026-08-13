<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Cifra que cuenta desde cero cuando entra en pantalla.
 *
 * Arranca con IntersectionObserver y no en `onMounted` a propósito: las
 * estadísticas del héroe están al pie de la primera pantalla y, si el contador
 * corriera al cargar, el visitante llegaría cuando ya ha terminado y solo vería
 * un número quieto.
 */
const props = defineProps({
  valor: { type: Number, required: true },
  duracion: { type: Number, default: 1600 },
  sufijo: { type: String, default: '' },
  /** Divide entre mil y añade una decimal (3200 -> "3.2"). */
  enMiles: { type: Boolean, default: false },
})

const mostrado = ref(0)
const raiz = ref(null)
let observador = null
let animacion = 0

function animar() {
  const inicio = performance.now()
  const paso = (ahora) => {
    const avance = Math.min((ahora - inicio) / props.duracion, 1)
    // Cúbica de salida: rápida al principio y frenando al final, que es como se
    // lee un marcador. Lineal parece una cuenta de máquina.
    const suave = 1 - Math.pow(1 - avance, 3)
    mostrado.value = Math.round(suave * props.valor)
    if (avance < 1) animacion = requestAnimationFrame(paso)
  }
  animacion = requestAnimationFrame(paso)
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    mostrado.value = props.valor
    return
  }
  observador = new IntersectionObserver(
    (entradas) => {
      if (entradas[0].isIntersecting) {
        animar()
        // Una sola vez: repetir la cuenta en cada scroll de vuelta distrae.
        observador.disconnect()
      }
    },
    { threshold: 0.3 },
  )
  if (raiz.value) observador.observe(raiz.value)
})

onBeforeUnmount(() => {
  observador?.disconnect()
  cancelAnimationFrame(animacion)
})

function formatear(n) {
  if (!props.enMiles) return String(n)
  return n >= 1000 ? (n / 1000).toFixed(1) : String(n)
}
</script>

<template>
  <span ref="raiz" class="contador">{{ formatear(mostrado) }}{{ sufijo }}</span>
</template>

<style scoped>
.contador {
  font-family: var(--fuente-display);
  font-size: 40px;
  font-weight: 700;
  color: var(--gold);
  line-height: 1;
  /* Cifras de ancho fijo: sin esto el número baila mientras cuenta. */
  font-variant-numeric: tabular-nums;
}
</style>
