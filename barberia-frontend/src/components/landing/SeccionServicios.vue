<script setup>
import RotuloSeccion from '@/components/RotuloSeccion.vue'

/**
 * Catálogo. Cada tarjeta es seleccionable y lo elegido viaja al formulario de
 * reserva, así que esta sección no solo informa: es el primer paso del flujo.
 */
defineProps({
  servicios: { type: Array, default: () => [] },
  /** Id del servicio elegido, o null. */
  seleccionado: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:seleccionado'])

function alternar(id) {
  emit('update:seleccionado', id)
}
</script>

<template>
  <!--
    `bs-light` redefine los tokens de texto y borde para esta sección, que va
    sobre pergamino. Sin ella, los componentes de dentro heredarían el crema
    pensado para el fondo oscuro y quedarían ilegibles.
  -->
  <section id="servicios" class="bs-seccion bs-light bs-fondo-parchment">
    <div class="bs-contenedor">
      <RotuloSeccion numero="01" texto="Servicios" />

      <div class="servicios__cabecera">
        <h2 class="bs-h2">
          Todo lo<br /><em class="bs-em">que necesitas.</em>
        </h2>
        <p class="bs-parrafo servicios__intro">
          Selecciona un servicio para incluirlo en tu reserva. Precios en USD, disponibles de lunes
          a sábado.
        </p>
      </div>

      <div class="servicios__rejilla">
        <article
          v-for="s in servicios"
          :key="s.id"
          class="servicio"
          :class="{ 'servicio--activo': seleccionado === s.id }"
          role="button"
          tabindex="0"
          @click="alternar(seleccionado === s.id ? null : s.id)"
          @keydown.enter="alternar(seleccionado === s.id ? null : s.id)"
          @keydown.space.prevent="alternar(seleccionado === s.id ? null : s.id)"
        >
          <span v-if="s.tag" class="servicio__tag">{{ s.tag }}</span>

          <h3 class="servicio__nombre">{{ s.nombre }}</h3>

          <div class="servicio__precio-fila">
            <span class="servicio__precio">${{ s.precio }}</span>
            <span class="servicio__meta">USD · {{ s.duracionMinutos }} min</span>
          </div>

          <p v-if="s.descripcion" class="servicio__descripcion">{{ s.descripcion }}</p>

          <div v-if="seleccionado === s.id" class="servicio__marca">✓ SELECCIONADO</div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.servicios__cabecera {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 64px;
  align-items: start;
  margin-bottom: 56px;
}

.servicios__intro {
  max-width: 400px;
  padding-top: 8px;
}

/* gap de 1px: las tarjetas comparten borde y forman una retícula continua, en
   vez de flotar separadas. Es lo que da el aire de tabla impresa. */
.servicios__rejilla {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1px;
}

.servicio {
  padding: 28px;
  background: var(--warm);
  border: 1px solid var(--border-light);
  position: relative;
  transition: background 0.22s, border-color 0.22s;
}

.servicio:hover:not(.servicio--activo) {
  background: var(--warm-hover);
}

/* La tarjeta elegida invierte a oscuro. Es el único elemento en negativo dentro
   de una sección clara, y por eso se ve inmediatamente cuál está seleccionada. */
.servicio--activo {
  background: var(--ink);
  border-color: var(--gold-40);
}

.servicio__tag {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 3px 9px;
  background: var(--ink);
  color: var(--gold);
  font-family: var(--fuente-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
.servicio--activo .servicio__tag {
  background: var(--gold);
  color: var(--ink);
}

.servicio__nombre {
  font-family: var(--fuente-display);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
  margin: 0 0 10px;
  padding-right: 76px;
}
.servicio--activo .servicio__nombre {
  color: var(--text);
}

.servicio__precio-fila {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.servicio__precio {
  font-family: var(--fuente-display);
  font-size: 38px;
  font-weight: 300;
  font-style: italic;
  color: var(--ink);
}
.servicio--activo .servicio__precio {
  color: var(--gold);
}

.servicio__meta {
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: #8a8480;
}

.servicio__descripcion {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #6a6460;
}
.servicio--activo .servicio__descripcion {
  color: var(--muted);
}

.servicio__marca {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #1e2028;
  font-family: var(--fuente-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--gold);
}

@media (max-width: 900px) {
  .servicios__cabecera {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>
