<script setup>
import RotuloSeccion from '@/components/RotuloSeccion.vue'
import { urlImagen } from '@/services/api'

/**
 * Portafolio en rejilla asimétrica: la primera pieza ocupa dos filas y el resto
 * se reparte alrededor. Esa irregularidad es la que evita que parezca un catálogo
 * y la que hace que la vista se detenga.
 */
defineProps({
  items: { type: Array, default: () => [] },
})

const IMAGEN_POR_DEFECTO =
  'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=600&h=800&fit=crop&auto=format'
</script>

<template>
  <section id="portafolio" class="bs-seccion bs-light bs-fondo-parchment">
    <div class="bs-contenedor">
      <RotuloSeccion numero="03" texto="Portafolio" />

      <div class="portafolio__cabecera">
        <h2 class="bs-h2">
          El trabajo<br /><em class="bs-em">habla solo.</em>
        </h2>
        <p class="portafolio__nota">
          Más de 1.200 cortes documentados en nuestras redes sociales.
        </p>
      </div>

      <div class="portafolio__rejilla">
        <figure
          v-for="(item, i) in items"
          :key="item.id ?? i"
          class="pieza"
          :class="{ 'pieza--destacada': i === 0 }"
        >
          <img :src="item.imagenUrl ? urlImagen(item.imagenUrl) : IMAGEN_POR_DEFECTO" :alt="item.titulo" />
          <div class="pieza__tinte" />
          <figcaption class="pieza__pie">
            <span class="pieza__titulo">{{ item.titulo }}</span>
            <span class="pieza__categoria">{{ item.categoria }}</span>
          </figcaption>
        </figure>
      </div>
    </div>
  </section>
</template>

<style scoped>
.portafolio__cabecera {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 48px;
  flex-wrap: wrap;
  gap: 16px;
}

.portafolio__nota {
  font-size: 13px;
  color: var(--muted);
  max-width: 220px;
  line-height: 1.7;
  margin: 0;
}

.portafolio__rejilla {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 3px;
}

.pieza {
  position: relative;
  overflow: hidden;
  background: var(--warm);
  min-height: 240px;
  margin: 0;
}

/* La primera ocupa las dos filas de la izquierda: es el ancla de la composición. */
.pieza--destacada {
  grid-row: 1 / 3;
  min-height: 500px;
}

.pieza img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: contrast(1.04);
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
.pieza:hover img {
  transform: scale(1.05);
}

.pieza__tinte {
  position: absolute;
  inset: 0;
  background: transparent;
  transition: background 0.3s;
}
.pieza:hover .pieza__tinte {
  background: var(--gold-15);
}

/* El pie pasa de negro translúcido a dorado macizo al posarse encima: es el
   mismo gesto que el botón principal, para que la página tenga un solo idioma. */
.pieza__pie {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 18px;
  background: rgba(7, 8, 13, 0.7);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  transition: background 0.3s;
}
.pieza:hover .pieza__pie {
  background: var(--gold);
}

.pieza__titulo {
  font-family: var(--fuente-display);
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
}
.pieza:hover .pieza__titulo {
  color: var(--ink);
}

.pieza__categoria {
  font-family: var(--fuente-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--muted);
}
.pieza:hover .pieza__categoria {
  color: #3a3030;
}

@media (max-width: 900px) {
  .portafolio__rejilla {
    grid-template-columns: 1fr 1fr;
  }
  .pieza--destacada {
    grid-row: auto;
    grid-column: 1 / -1;
    min-height: 320px;
  }
}
</style>
