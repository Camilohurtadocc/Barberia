<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import BarraNavegacion from '@/components/landing/BarraNavegacion.vue'
import SeccionHero from '@/components/landing/SeccionHero.vue'
import SeccionServicios from '@/components/landing/SeccionServicios.vue'
import SeccionEquipo from '@/components/landing/SeccionEquipo.vue'
import SeccionPortafolio from '@/components/landing/SeccionPortafolio.vue'
import SeccionReservas from '@/components/landing/SeccionReservas.vue'
import SeccionRedes from '@/components/landing/SeccionRedes.vue'
import PieSitio from '@/components/landing/PieSitio.vue'
import DialogoAcceso from '@/components/DialogoAcceso.vue'
import { getBarberos, getConfiguracion, getPerfil, getPortafolio, getServicios } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

/**
 * Landing pública.
 *
 * Aquí solo viven los datos y el estado de la reserva en curso; el aspecto está
 * repartido en las secciones. El estado se sostiene en esta página, y no dentro
 * de cada sección, porque la reserva se va componiendo a lo largo del scroll: el
 * servicio se elige en Servicios, el barbero y la hora en Equipo, y el resto en
 * Reservas. Si cada sección guardase lo suyo, habría que repetir la selección.
 */

const auth = useAuthStore()

const servicios = ref([])
const barberos = ref([])
const portafolio = ref([])
const config = ref({ tickerMensajes: [] })
const perfil = ref(null)

const mostrarAcceso = ref(false)

// Borrador de la reserva, compartido por las tres secciones que participan.
const servicioId = ref(null)
const barberoId = ref(null)
const slot = ref(null)

const servicioNombre = computed(
  () => servicios.value.find((s) => s.id === servicioId.value)?.nombre || '',
)

/**
 * Turnos libres de hoy, para el distintivo del héroe.
 *
 * Es una estimación a partir de los horarios publicados por los barberos, no una
 * consulta a las citas: la agenda solo la puede leer quien tiene sesión, y el
 * héroe lo ve todo el mundo. Pedirla aquí devolvería 401 al visitante anónimo.
 */
const turnosLibres = computed(() =>
  barberos.value.reduce((total, b) => total + (b.slots?.length || 0), 0),
)

const PORTAFOLIO_POR_DEFECTO = [
  { titulo: 'Skin Fade', categoria: 'FADE' },
  { titulo: 'Tijera Clásica', categoria: 'CLASSIC' },
  { titulo: 'The Chair', categoria: 'LIFESTYLE' },
  { titulo: 'Detail Work', categoria: 'PRECISION' },
  { titulo: 'Beard Art', categoria: 'BEARD' },
]

async function cargar() {
  // allSettled y no all: si un microservicio está caído, la landing se pinta con
  // lo que sí haya llegado en vez de quedarse en blanco entera.
  const [resServicios, resBarberos, resConfig, resPortafolio] = await Promise.allSettled([
    getServicios(),
    getBarberos(),
    getConfiguracion(),
    getPortafolio(),
  ])

  if (resServicios.status === 'fulfilled') servicios.value = resServicios.value.data || []
  if (resBarberos.status === 'fulfilled') barberos.value = resBarberos.value.data || []
  if (resConfig.status === 'fulfilled' && resConfig.value.data) {
    config.value = { tickerMensajes: [], ...resConfig.value.data }
  }
  if (resPortafolio.status === 'fulfilled' && resPortafolio.value.data?.length) {
    portafolio.value = resPortafolio.value.data
  } else {
    portafolio.value = PORTAFOLIO_POR_DEFECTO
  }

  // El primer barbero queda preseleccionado para que el panel del equipo no
  // aparezca vacío esperando un clic.
  if (!barberoId.value && barberos.value.length) {
    barberoId.value = barberos.value[0].id
  }
}

async function cargarPerfil() {
  if (!auth.esCliente) {
    perfil.value = null
    return
  }
  try {
    const { data } = await getPerfil()
    perfil.value = data
  } catch {
    // Que falle el perfil no debe romper la landing: solo significa que el
    // formulario de reserva no llegará relleno.
    perfil.value = null
  }
}

onMounted(async () => {
  await cargar()
  await cargarPerfil()
})

// Al entrar o salir de la sesión hay que rehacer el perfil: si no, el formulario
// se quedaría con los datos de quien acaba de cerrar sesión.
watch(() => auth.estaAutenticado, cargarPerfil)
</script>

<template>
  <div>
    <BarraNavegacion @abrir-acceso="mostrarAcceso = true" />

    <SeccionHero :config="config" :turnos-libres="turnosLibres" />

    <SeccionServicios v-model:seleccionado="servicioId" :servicios="servicios" />

    <SeccionEquipo
      v-model:barbero-id="barberoId"
      v-model:slot="slot"
      :barberos="barberos"
      :servicio-nombre="servicioNombre"
    />

    <SeccionPortafolio :items="portafolio" />

    <SeccionReservas
      v-model:servicio-id="servicioId"
      v-model:barbero-id="barberoId"
      :servicios="servicios"
      :barberos="barberos"
      :config="config"
      :slot="slot"
      :perfil="perfil"
    />

    <SeccionRedes :config="config" :portafolio="portafolio" />

    <PieSitio :config="config" />

    <DialogoAcceso v-model="mostrarAcceso" @acceso="cargarPerfil" />
  </div>
</template>
