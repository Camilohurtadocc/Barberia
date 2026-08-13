<script setup>
import { computed } from 'vue'
import CintaAnimada from '@/components/CintaAnimada.vue'
import ContadorAnimado from '@/components/ContadorAnimado.vue'
import { urlImagen } from '@/services/api'

const props = defineProps({
  config: { type: Object, default: () => ({}) },
  /** Turnos que quedan libres hoy; se pinta en el distintivo flotante. */
  turnosLibres: { type: Number, default: 0 },
})

const IMAGEN_POR_DEFECTO =
  'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=900&h=1100&fit=crop&auto=format'

const imagen = computed(() =>
  props.config.heroImagenUrl ? urlImagen(props.config.heroImagenUrl) : IMAGEN_POR_DEFECTO,
)

const subtitulo = computed(
  () =>
    props.config.heroSubtitulo ||
    'Más de 12 años formando el estilo de Bogotá. Fade, diseño y precisión — cada detalle cuenta.',
)

const direccion = computed(() => props.config.direccion || 'Cra 7 #85-32, Bogotá')

function irA(ancla) {
  document.getElementById(ancla)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <section id="inicio" class="hero">
    <CintaAnimada :mensajes="config.tickerMensajes || []" />

    <div class="hero__cuerpo">
      <div class="bs-grano" />

      <!-- Columna de texto -->
      <div class="hero__texto">
        <div class="hero__distintivo">
          <span class="bs-punto-vivo" />
          <span class="hero__distintivo-texto">BOGOTÁ · EST. 2012</span>
        </div>

        <!--
          El titular se parte en tres renglones a mano en lugar de dejar que fluya:
          el salto entre «El arte» y «del buen» es lo que coloca la itálica dorada
          justo en el centro óptico del bloque.
        -->
        <h1 class="bs-h1">
          El arte<br />
          <em class="bs-em hero__acento">del buen</em><br />
          corte.
        </h1>

        <p class="bs-parrafo hero__subtitulo">{{ subtitulo }}</p>

        <div class="hero__botones">
          <button
            type="button"
            class="bs-btn bs-btn--oro"
            @click="irA('reservas')"
          >
            Reservar turno
          </button>
          <button
            type="button"
            class="bs-btn bs-btn--fantasma"
            @click="irA('servicios')"
          >
            Ver servicios
          </button>
        </div>

        <div class="hero__cifras">
          <div class="hero__cifra">
            <ContadorAnimado :valor="12" sufijo="+" />
            <span class="bs-eyebrow">Años de experiencia</span>
          </div>
          <div class="hero__cifra">
            <ContadorAnimado :valor="3200" :duracion="1800" en-miles sufijo="K+" />
            <span class="bs-eyebrow">Clientes al mes</span>
          </div>
          <div class="hero__cifra">
            <ContadorAnimado :valor="98" :duracion="1200" sufijo="%" />
            <span class="bs-eyebrow">Satisfacción</span>
          </div>
        </div>
      </div>

      <!-- Columna de imagen -->
      <div class="hero__imagen" @click="irA('portafolio')">
        <img :src="imagen" alt="Barbero trabajando con precisión" />
        <div class="hero__velo" />

        <div class="hero__flotante">
          <div class="hero__flotante-rotulo">DISPONIBLE HOY</div>
          <div class="hero__flotante-cifra">{{ turnosLibres }}</div>
          <div class="hero__flotante-pie">turnos libres</div>
        </div>

        <div class="hero__franja">
          <span class="bs-mono hero__franja-texto">{{ direccion }}</span>
          <span class="hero__estrellas">
            <span v-for="n in 5" :key="n">★</span>
            <span class="bs-mono hero__nota">4.9</span>
          </span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--ink);
  display: grid;
  grid-template-rows: auto 1fr;
}

.hero__cuerpo {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: calc(100vh - 112px);
  position: relative;
}

.hero__texto {
  padding: 72px clamp(24px, 5vw, 60px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.hero__distintivo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 44px;
}

.hero__distintivo-texto {
  font-family: var(--fuente-mono);
  font-size: 10px;
  letter-spacing: 0.24em;
  color: var(--gold);
}

/* La itálica va un punto más grande que el resto del titular: al ser de peso
   ligero, en el mismo cuerpo se vería más pequeña de lo que es. */
.hero__acento {
  font-size: 1.08em;
}

.hero__subtitulo {
  max-width: 400px;
  margin: 28px 0 44px;
}

.hero__botones {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.hero__cifras {
  display: flex;
  border-top: 1px solid var(--border);
  padding-top: 40px;
  margin-top: 40px;
}

.hero__cifra {
  flex: 1;
  padding-left: 28px;
  border-left: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
/* El primero no lleva línea: marcaría un borde suelto al inicio de la fila. */
.hero__cifra:first-child {
  padding-left: 0;
  border-left: none;
}

.hero__imagen {
  position: relative;
  overflow: hidden;
  background: var(--surface);
  z-index: 2;
}

.hero__imagen img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: grayscale(10%) contrast(1.04);
}

/* Doble degradado: uno funde el borde izquierdo con la columna de texto y otro
   oscurece la base para que la franja inferior se lea sobre cualquier foto. */
.hero__velo {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(to right, var(--ink) 0%, transparent 28%),
    linear-gradient(to top, rgba(7, 8, 13, 0.6) 0%, transparent 40%);
}

.hero__flotante {
  position: absolute;
  top: 36px;
  right: 36px;
  background: rgba(7, 8, 13, 0.88);
  backdrop-filter: blur(12px);
  border: 1px solid var(--gold-40);
  padding: 18px 22px;
}

.hero__flotante-rotulo {
  font-family: var(--fuente-mono);
  font-size: 9px;
  color: var(--gold);
  letter-spacing: 0.2em;
  margin-bottom: 4px;
}

.hero__flotante-cifra {
  font-family: var(--fuente-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
}

.hero__flotante-pie {
  font-size: 10px;
  color: var(--muted);
  margin-top: 2px;
}

.hero__franja {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 18px 24px;
  background: rgba(7, 8, 13, 0.8);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero__franja-texto {
  font-size: 11px;
  color: var(--muted);
}

.hero__estrellas {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: var(--gold);
}

.hero__nota {
  font-size: 10px;
  color: var(--muted);
  margin-left: 4px;
}

@media (max-width: 900px) {
  .hero__cuerpo {
    grid-template-columns: 1fr;
  }
  /* La imagen pasa debajo del texto y se le fija altura: sin ella, al perder la
     rejilla de dos columnas, el contenedor colapsa y la foto desaparece. */
  .hero__imagen {
    min-height: 60vh;
    order: 2;
  }
  .hero__velo {
    background: linear-gradient(to top, rgba(7, 8, 13, 0.7) 0%, transparent 45%);
  }
  .hero__cifras {
    flex-wrap: wrap;
    gap: 20px;
  }
}
</style>
