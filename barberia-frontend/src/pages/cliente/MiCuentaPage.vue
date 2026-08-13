<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cancelarCita, getCitas, getPerfil, getServicios, updatePerfil } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

/**
 * Área del cliente: sus citas y sus datos de contacto.
 *
 * El listado se parte en «próximas» e «historial» en lugar de mostrar una sola
 * tabla ordenada por fecha. Lo que el cliente viene a hacer aquí es comprobar o
 * cancelar lo que tiene por delante; lo pasado es consulta ocasional y estorba
 * arriba.
 */

const auth = useAuthStore()
const router = useRouter()

const citas = ref([])
const servicios = ref([])
const perfil = ref({ nombre: '', email: '', telefono: '' })
const cargando = ref(true)
const error = ref('')
const cancelando = ref(null)
const editandoPerfil = ref(false)
const guardandoPerfil = ref(false)
const avisoPerfil = ref('')

const CANCELADA = 'CANCELADA'

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [resCitas, resServicios, resPerfil] = await Promise.allSettled([
      getCitas(),
      getServicios(),
      getPerfil(),
    ])
    if (resCitas.status === 'fulfilled') {
      citas.value = resCitas.value.data || []
    } else {
      error.value = 'No se pudieron cargar tus citas.'
    }
    if (resServicios.status === 'fulfilled') servicios.value = resServicios.value.data || []
    if (resPerfil.status === 'fulfilled' && resPerfil.value.data) {
      const p = resPerfil.value.data
      perfil.value = { nombre: p.nombre || '', email: p.email || '', telefono: p.telefono || '' }
    }
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

function nombreServicio(id) {
  return servicios.value.find((s) => s.id === id)?.nombre || 'Servicio'
}

function esFutura(cita) {
  return cita.fechaHora && new Date(cita.fechaHora) >= new Date()
}

// Una cita cancelada no es "próxima" aunque su fecha esté por delante: ya no hay
// nada que esperar, y dejarla arriba haría creer que sigue en pie.
const proximas = computed(() =>
  citas.value
    .filter((c) => esFutura(c) && c.estado !== CANCELADA)
    .sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora)),
)

const historial = computed(() =>
  citas.value
    .filter((c) => !esFutura(c) || c.estado === CANCELADA)
    .sort((a, b) => new Date(b.fechaHora) - new Date(a.fechaHora)),
)

async function cancelar(cita) {
  if (!window.confirm(`¿Cancelar tu cita del ${formatearFecha(cita.fechaHora)}?`)) return
  cancelando.value = cita.id
  error.value = ''
  try {
    const { data } = await cancelarCita(cita.id)
    // Se sustituye el elemento en el sitio en vez de recargar la lista entera:
    // así la tarjeta no salta de sección mientras el ojo sigue en ella.
    const i = citas.value.findIndex((c) => c.id === cita.id)
    if (i !== -1) citas.value[i] = data
  } catch (e) {
    error.value =
      e.response?.data?.mensaje || 'No se pudo cancelar la cita. Inténtalo de nuevo.'
  } finally {
    cancelando.value = null
  }
}

async function guardarPerfil() {
  guardandoPerfil.value = true
  avisoPerfil.value = ''
  try {
    await updatePerfil(perfil.value)
    avisoPerfil.value = 'Datos actualizados'
    editandoPerfil.value = false
  } catch {
    avisoPerfil.value = 'No se pudieron guardar los datos'
  } finally {
    guardandoPerfil.value = false
  }
}

function cerrarSesion() {
  auth.logout()
  router.push('/')
}

function formatearFecha(valor) {
  const f = new Date(valor)
  if (Number.isNaN(f.getTime())) return valor ?? ''
  return f.toLocaleString('es-CO', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function claseEstado(estado) {
  return `bs-estado bs-estado--${(estado || 'pendiente').toLowerCase()}`
}
</script>

<template>
  <div class="cuenta">
    <header class="cuenta__barra">
      <RouterLink to="/" class="cuenta__marca">
        <span class="cuenta__monograma">TB</span>
        <span class="cuenta__nombre">The Barbershop</span>
      </RouterLink>
      <div class="cuenta__sesion">
        <span class="cuenta__usuario">{{ auth.nombre || auth.usuario }}</span>
        <RouterLink to="/#reservas" class="bs-btn bs-btn--oro bs-btn--pequeno">
          Reservar
        </RouterLink>
        <button type="button" class="bs-btn bs-btn--fantasma bs-btn--pequeno" @click="cerrarSesion">
          Salir
        </button>
      </div>
    </header>

    <main class="cuenta__cuerpo">
      <div class="bs-rotulo">
        <span class="bs-rotulo__n">01</span>
        <div class="bs-rotulo__linea" />
        <span class="bs-rotulo__texto">Mis citas</span>
      </div>

      <h1 class="bs-h2 cuenta__titulo">
        Hola,<br /><em class="bs-em">{{ (auth.nombre || auth.usuario || '').split(' ')[0] }}.</em>
      </h1>

      <p v-if="error" class="cuenta__error">{{ error }}</p>
      <p v-if="cargando" class="cuenta__cargando">Cargando tus citas…</p>

      <template v-else>
        <!-- Próximas -->
        <section class="cuenta__bloque">
          <h2 class="cuenta__subtitulo">Próximas</h2>

          <div v-if="proximas.length" class="cuenta__lista">
            <article v-for="c in proximas" :key="c.id" class="bs-tarjeta cita">
              <div class="cita__principal">
                <div class="cita__servicio">{{ nombreServicio(c.servicioId) }}</div>
                <div class="cita__fecha">{{ formatearFecha(c.fechaHora) }}</div>
                <div class="cita__meta">
                  <template v-if="c.barbero">Con {{ c.barbero }}</template>
                  <template v-else>Barbero por asignar</template>
                </div>
                <p v-if="c.notas" class="cita__notas">“{{ c.notas }}”</p>
              </div>
              <div class="cita__lateral">
                <span :class="claseEstado(c.estado)">{{ c.estado }}</span>
                <button
                  type="button"
                  class="bs-btn bs-btn--peligro bs-btn--pequeno"
                  :disabled="cancelando === c.id"
                  @click="cancelar(c)"
                >
                  {{ cancelando === c.id ? 'Cancelando…' : 'Cancelar' }}
                </button>
              </div>
            </article>
          </div>

          <div v-else class="cuenta__vacio">
            <p>No tienes ninguna cita reservada.</p>
            <RouterLink to="/#reservas" class="bs-btn bs-btn--oro">Reservar turno</RouterLink>
          </div>
        </section>

        <!-- Historial -->
        <section v-if="historial.length" class="cuenta__bloque">
          <h2 class="cuenta__subtitulo">Historial</h2>
          <div class="cuenta__lista">
            <article v-for="c in historial" :key="c.id" class="bs-tarjeta cita cita--pasada">
              <div class="cita__principal">
                <div class="cita__servicio">{{ nombreServicio(c.servicioId) }}</div>
                <div class="cita__fecha">{{ formatearFecha(c.fechaHora) }}</div>
                <div class="cita__meta">
                  <template v-if="c.barbero">Con {{ c.barbero }}</template>
                </div>
              </div>
              <div class="cita__lateral">
                <span :class="claseEstado(c.estado)">{{ c.estado }}</span>
              </div>
            </article>
          </div>
        </section>

        <!-- Datos de contacto -->
        <section class="cuenta__bloque">
          <h2 class="cuenta__subtitulo">Mis datos</h2>
          <div class="bs-tarjeta cuenta__perfil">
            <template v-if="editandoPerfil">
              <div class="cuenta__perfil-campos">
                <label class="bs-campo">
                  <span class="bs-campo__label">Nombre</span>
                  <input v-model="perfil.nombre" class="bs-input" />
                </label>
                <label class="bs-campo">
                  <span class="bs-campo__label">WhatsApp</span>
                  <input v-model="perfil.telefono" class="bs-input" />
                </label>
                <label class="bs-campo">
                  <span class="bs-campo__label">Correo</span>
                  <input v-model="perfil.email" type="email" class="bs-input" />
                </label>
              </div>
              <div class="cuenta__perfil-acciones">
                <button
                  type="button"
                  class="bs-btn bs-btn--oro bs-btn--pequeno"
                  :disabled="guardandoPerfil"
                  @click="guardarPerfil"
                >
                  {{ guardandoPerfil ? 'Guardando…' : 'Guardar' }}
                </button>
                <button
                  type="button"
                  class="bs-btn bs-btn--fantasma bs-btn--pequeno"
                  @click="editandoPerfil = false"
                >
                  Cancelar
                </button>
              </div>
            </template>

            <template v-else>
              <dl class="cuenta__datos">
                <div>
                  <dt class="bs-eyebrow">Nombre</dt>
                  <dd>{{ perfil.nombre || '—' }}</dd>
                </div>
                <div>
                  <dt class="bs-eyebrow">WhatsApp</dt>
                  <dd>{{ perfil.telefono || '—' }}</dd>
                </div>
                <div>
                  <dt class="bs-eyebrow">Correo</dt>
                  <dd>{{ perfil.email || '—' }}</dd>
                </div>
              </dl>
              <button
                type="button"
                class="bs-btn bs-btn--fantasma bs-btn--pequeno"
                @click="editandoPerfil = true"
              >
                Editar
              </button>
            </template>
          </div>
          <p v-if="avisoPerfil" class="cuenta__aviso">{{ avisoPerfil }}</p>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.cuenta {
  min-height: 100vh;
  background: var(--ink);
}

.cuenta__barra {
  height: 64px;
  padding: 0 clamp(16px, 4vw, 40px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border);
  background: rgba(7, 8, 13, 0.94);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(16px);
}

.cuenta__marca {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.cuenta__monograma {
  width: 30px;
  height: 30px;
  border: 1px solid var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fuente-display);
  font-size: 13px;
  font-weight: 700;
  font-style: italic;
  color: var(--gold);
}

.cuenta__nombre {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text);
}

.cuenta__sesion {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cuenta__usuario {
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: var(--muted);
}

.cuenta__cuerpo {
  max-width: 900px;
  margin: 0 auto;
  padding: 56px clamp(20px, 5vw, 40px) 96px;
}

.cuenta__titulo {
  margin: 0 0 48px;
}

.cuenta__bloque {
  margin-bottom: 56px;
}

.cuenta__subtitulo {
  font-family: var(--fuente-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--muted);
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}

.cuenta__lista {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cita {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

/* Lo pasado se atenúa: sigue consultable, pero no compite con lo que aún puede
   gestionarse. */
.cita--pasada .cita__servicio,
.cita--pasada .cita__fecha {
  color: var(--muted);
}

.cita__servicio {
  font-family: var(--fuente-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--text);
}

.cita__fecha {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--gold);
  margin-top: 6px;
  /* La primera letra del día de la semana llega en minúscula desde toLocaleString. */
  text-transform: capitalize;
}

.cita__meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}

.cita__notas {
  margin: 10px 0 0;
  font-size: 12px;
  font-style: italic;
  color: var(--muted);
  line-height: 1.6;
}

.cita__lateral {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.cuenta__vacio {
  padding: 44px 24px;
  border: 1px dashed var(--border);
  text-align: center;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  font-size: 14px;
}

.cuenta__perfil {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
}

.cuenta__datos {
  display: flex;
  gap: 44px;
  margin: 0;
  flex-wrap: wrap;
}
.cuenta__datos dd {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--text);
}

.cuenta__perfil-campos {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  flex: 1;
}

.cuenta__perfil-acciones {
  display: flex;
  gap: 8px;
}

.cuenta__error {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--danger);
  margin-bottom: 20px;
}

.cuenta__cargando,
.cuenta__aviso {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--muted);
  margin-top: 12px;
}

@media (max-width: 600px) {
  .cita {
    flex-direction: column;
  }
  .cita__lateral {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
  .cuenta__nombre {
    display: none;
  }
}
</style>
