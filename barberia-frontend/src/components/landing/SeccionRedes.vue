<script setup>
import { computed } from 'vue'
import RotuloSeccion from '@/components/RotuloSeccion.vue'
import { urlImagen } from '@/services/api'

/**
 * «Síguenos». No hay un microservicio de redes sociales, así que las tarjetas se
 * arman con lo que sí existe: el usuario de Instagram que el administrador guarda
 * en Sitio, y las imágenes del portafolio como muestra de la cuenta.
 *
 * Se construye a partir de datos reales en vez de dejar cifras inventadas fijas
 * en el código: un contador de seguidores falso envejece mal y nadie se acuerda
 * de quitarlo.
 */
const props = defineProps({
  config: { type: Object, default: () => ({}) },
  /** Se usan como muestra visual de las cuentas. */
  portafolio: { type: Array, default: () => [] },
})

const IMAGEN_POR_DEFECTO =
  'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=400&h=400&fit=crop&auto=format'

/** Cuatro imágenes por tarjeta, que es lo que pide la rejilla 2x2. */
function muestras(desplazamiento) {
  const fuentes = props.portafolio.map((p) => p.imagenUrl).filter(Boolean)
  const salida = []
  for (let i = 0; i < 4; i++) {
    const url = fuentes.length ? fuentes[(i + desplazamiento) % fuentes.length] : null
    salida.push(url ? urlImagen(url) : IMAGEN_POR_DEFECTO)
  }
  return salida
}

function normalizarUsuario(valor, plataforma) {
  if (!valor) return `@thebarbershop${plataforma === 'Instagram' ? '' : '.bog'}`
  return valor.startsWith('@') ? valor : `@${valor}`
}

const redes = computed(() => {
  const instagram = normalizarUsuario(props.config.instagram, 'Instagram')
  return [
    {
      plataforma: 'Instagram',
      usuario: instagram,
      url: `https://instagram.com/${instagram.replace('@', '')}`,
      imagenes: muestras(0),
    },
    {
      plataforma: 'TikTok',
      usuario: instagram,
      url: `https://tiktok.com/${instagram}`,
      imagenes: muestras(2),
    },
  ]
})
</script>

<template>
  <section id="redes" class="bs-seccion bs-fondo-surface">
    <div class="bs-contenedor">
      <RotuloSeccion numero="05" texto="Síguenos" />

      <h2 class="bs-h2 redes__titulo">
        Vívelo en<br /><em class="bs-em">redes.</em>
      </h2>

      <div class="redes__rejilla">
        <article
          v-for="r in redes"
          :key="r.plataforma"
          class="bs-tarjeta bs-tarjeta--pulsable red"
        >
          <header class="red__cabecera">
            <div>
              <div class="red__plataforma">{{ r.plataforma }}</div>
              <div class="red__usuario">{{ r.usuario }}</div>
            </div>
          </header>

          <div class="red__imagenes">
            <div v-for="(img, i) in r.imagenes" :key="i" class="red__celda">
              <img :src="img" alt="" />
            </div>
          </div>

          <footer class="red__pie">
            <span class="red__nota">Trabajos recientes</span>
            <a
              :href="r.url"
              target="_blank"
              rel="noreferrer"
              class="bs-btn bs-btn--pequeno red__boton"
            >
              Seguir
            </a>
          </footer>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.redes__titulo {
  margin-bottom: 48px;
}

.redes__rejilla {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 2px;
}

.red {
  overflow: hidden;
}

.red__cabecera {
  padding: 22px 22px 18px;
  border-bottom: 1px solid var(--border);
}

.red__plataforma {
  font-family: var(--fuente-display);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
}

.red__usuario {
  font-family: var(--fuente-mono);
  font-size: 10px;
  color: var(--muted);
  margin-top: 2px;
}

.red__imagenes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px;
  padding: 2px;
}

.red__celda {
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--surface);
}

.red__celda img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: grayscale(15%);
  transition: transform 0.35s, filter 0.35s;
}
.red__celda:hover img {
  transform: scale(1.08);
  filter: grayscale(0%);
}

.red__pie {
  padding: 16px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--border);
}

.red__nota {
  font-family: var(--fuente-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--muted);
}

.red__boton {
  background: transparent;
  border-color: var(--gold-40);
  color: var(--gold);
}
.red__boton:hover {
  background: var(--gold);
  color: var(--ink);
}
</style>
