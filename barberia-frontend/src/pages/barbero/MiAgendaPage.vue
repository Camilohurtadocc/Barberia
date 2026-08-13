<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCitas, getServicios, updateCitaEstado } from '@/services/api'

defineOptions({ name: 'BarberoMiAgendaPage' })

/**
 * Agenda del barbero.
 *
 * Antes era una tabla ordenable con siete columnas. Se ha cambiado por una lista
 * agrupada por día porque el barbero no consulta su agenda como quien explora
 * datos: la mira entre cliente y cliente para saber quién entra ahora y qué le
 * toca. «Hoy» arriba y el resto por fecha responde a esa pregunta sin obligarle
 * a ordenar ni buscar.
 *
 * El teléfono es un enlace de WhatsApp: avisar de un retraso es lo que más se
 * hace desde esta pantalla, y copiar el número a mano era el paso que sobraba.
 */

const citas = ref([])
const servicios = ref([])
const cargando = ref(false)
const error = ref('')
const filtro = ref('ACTIVAS')
const actualizando = ref(null)

const FILTROS = [
  { clave: 'ACTIVAS', texto: 'Activas' },
  { clave: 'PENDIENTE', texto: 'Pendientes' },
  { clave: 'CONFIRMADA', texto: 'Confirmadas' },
  { clave: 'COMPLETADA', texto: 'Completadas' },
  { clave: 'TODAS', texto: 'Todas' },
]

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    // El backend filtra por el barberoId del token: aquí no se manda ningún
    // identificador, justamente para que no se pueda pedir la agenda de otro.
    const [resCitas, resServicios] = await Promise.allSettled([getCitas(), getServicios()])
    if (resCitas.status === 'fulfilled') {
      citas.value = resCitas.value.data || []
    } else if (resCitas.reason?.response?.status !== 401) {
      // El 401 lo gestiona el interceptor global cerrando la sesión; avisar aquí
      // además solo añade ruido justo mientras se expulsa al usuario.
      error.value = 'No se pudo cargar tu agenda.'
    }
    if (resServicios.status === 'fulfilled') servicios.value = resServicios.value.data || []
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

function nombreServicio(id) {
  return servicios.value.find((s) => s.id === id)?.nombre || 'Servicio'
}

const visibles = computed(() => {
  const lista = [...citas.value].sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora))
  if (filtro.value === 'TODAS') return lista
  if (filtro.value === 'ACTIVAS') {
    return lista.filter((c) => c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA')
  }
  return lista.filter((c) => c.estado === filtro.value)
})

function claveDia(valor) {
  const f = new Date(valor)
  if (Number.isNaN(f.getTime())) return 'sin-fecha'
  return f.toISOString().split('T')[0]
}

/** Agrupa en {titulo, citas} respetando el orden cronológico. */
const grupos = computed(() => {
  const hoy = claveDia(new Date())
  const manana = claveDia(new Date(Date.now() + 86400000))
  const mapa = new Map()

  for (const cita of visibles.value) {
    const clave = claveDia(cita.fechaHora)
    if (!mapa.has(clave)) mapa.set(clave, [])
    mapa.get(clave).push(cita)
  }

  return [...mapa.entries()].map(([clave, lista]) => {
    let titulo
    if (clave === hoy) titulo = 'Hoy'
    else if (clave === manana) titulo = 'Mañana'
    else if (clave === 'sin-fecha') titulo = 'Sin fecha'
    else {
      titulo = new Date(`${clave}T12:00:00`).toLocaleDateString('es-CO', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
      })
    }
    return { clave, titulo, esHoy: clave === hoy, citas: lista }
  })
})

const resumen = computed(() => {
  const hoy = claveDia(new Date())
  return [
    {
      etiqueta: 'Hoy',
      valor: citas.value.filter(
        (c) => claveDia(c.fechaHora) === hoy && c.estado !== 'CANCELADA',
      ).length,
    },
    {
      etiqueta: 'Pendientes de confirmar',
      valor: citas.value.filter((c) => c.estado === 'PENDIENTE').length,
    },
    {
      etiqueta: 'Confirmadas',
      valor: citas.value.filter((c) => c.estado === 'CONFIRMADA').length,
    },
    {
      etiqueta: 'Completadas',
      valor: citas.value.filter((c) => c.estado === 'COMPLETADA').length,
    },
  ]
})

async function cambiar(cita, estado) {
  actualizando.value = cita.id
  error.value = ''
  try {
    const { data } = await updateCitaEstado(cita.id, estado)
    const i = citas.value.findIndex((c) => c.id === cita.id)
    if (i !== -1) citas.value[i] = data
  } catch (e) {
    if (e.response?.status !== 401) {
      error.value = e.response?.data?.mensaje || 'No se pudo actualizar la cita.'
    }
  } finally {
    actualizando.value = null
  }
}

function cancelar(cita) {
  if (!window.confirm(`¿Cancelar la cita de ${cita.clienteNombre}?`)) return
  cambiar(cita, 'CANCELADA')
}

function hora(valor) {
  const f = new Date(valor)
  if (Number.isNaN(f.getTime())) return '—'
  return f.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })
}

/** Deja solo los dígitos: wa.me rechaza espacios, guiones y el signo +. */
function enlaceWhatsapp(telefono) {
  const limpio = (telefono || '').replace(/\D/g, '')
  return limpio ? `https://wa.me/${limpio}` : null
}

function claseEstado(estado) {
  return `bs-estado bs-estado--${(estado || 'pendiente').toLowerCase()}`
}
</script>

<template>
  <div>
    <div class="bs-rotulo">
      <span class="bs-rotulo__n">01</span>
      <div class="bs-rotulo__linea" />
      <span class="bs-rotulo__texto">Agenda personal</span>
    </div>

    <div class="agenda__cabecera">
      <h1 class="bs-h2">
        Mis<br /><em class="bs-em">citas.</em>
      </h1>
      <button type="button" class="bs-btn bs-btn--fantasma bs-btn--pequeno" @click="cargar">
        Actualizar
      </button>
    </div>

    <!-- Los cuatro números que se miran al empezar el día -->
    <div class="agenda__resumen">
      <div v-for="k in resumen" :key="k.etiqueta" class="bs-tarjeta agenda__cifra">
        <div class="agenda__cifra-valor">{{ k.valor }}</div>
        <div class="bs-eyebrow">{{ k.etiqueta }}</div>
      </div>
    </div>

    <div class="agenda__filtros">
      <button
        v-for="f in FILTROS"
        :key="f.clave"
        type="button"
        class="agenda__filtro"
        :class="{ 'agenda__filtro--activo': filtro === f.clave }"
        @click="filtro = f.clave"
      >
        {{ f.texto }}
      </button>
    </div>

    <p v-if="error" class="agenda__error">{{ error }}</p>
    <p v-if="cargando" class="agenda__cargando">Cargando…</p>

    <template v-else>
      <section v-for="g in grupos" :key="g.clave" class="agenda__grupo">
        <h2 class="agenda__dia" :class="{ 'agenda__dia--hoy': g.esHoy }">{{ g.titulo }}</h2>

        <article v-for="c in g.citas" :key="c.id" class="bs-tarjeta cita">
          <div class="cita__hora">{{ hora(c.fechaHora) }}</div>

          <div class="cita__centro">
            <div class="cita__cliente">{{ c.clienteNombre }}</div>
            <div class="cita__servicio">{{ nombreServicio(c.servicioId) }}</div>
            <a
              v-if="enlaceWhatsapp(c.clienteTelefono)"
              :href="enlaceWhatsapp(c.clienteTelefono)"
              target="_blank"
              rel="noreferrer"
              class="cita__telefono"
            >
              {{ c.clienteTelefono }}
            </a>
            <p v-if="c.notas" class="cita__notas">“{{ c.notas }}”</p>
          </div>

          <div class="cita__acciones">
            <span :class="claseEstado(c.estado)">{{ c.estado }}</span>

            <!--
              Solo cambios de estado: el backend responde 403 si un barbero
              intenta borrar, porque eso perdería el historial de la barbería.
              Cancelar es un estado, no un borrado.
            -->
            <div class="cita__botones">
              <button
                v-if="c.estado === 'PENDIENTE'"
                type="button"
                class="bs-btn bs-btn--oro bs-btn--pequeno"
                :disabled="actualizando === c.id"
                @click="cambiar(c, 'CONFIRMADA')"
              >
                Confirmar
              </button>
              <button
                v-if="c.estado === 'CONFIRMADA'"
                type="button"
                class="bs-btn bs-btn--oro bs-btn--pequeno"
                :disabled="actualizando === c.id"
                @click="cambiar(c, 'COMPLETADA')"
              >
                Completar
              </button>
              <button
                v-if="c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA'"
                type="button"
                class="bs-btn bs-btn--peligro bs-btn--pequeno"
                :disabled="actualizando === c.id"
                @click="cancelar(c)"
              >
                Cancelar
              </button>
            </div>
          </div>
        </article>
      </section>

      <div v-if="!grupos.length" class="agenda__vacio">
        No hay citas que mostrar con este filtro.
      </div>
    </template>
  </div>
</template>

<style scoped>
.agenda__cabecera {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 36px;
}

.agenda__resumen {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 2px;
  margin-bottom: 32px;
}

.agenda__cifra {
  padding: 20px 22px;
}

.agenda__cifra-valor {
  font-family: var(--fuente-display);
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: var(--gold);
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.agenda__filtros {
  display: flex;
  gap: 2px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.agenda__filtro {
  padding: 9px 18px;
  background: transparent;
  border: 1px solid var(--border);
  font-family: var(--fuente-ui);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.agenda__filtro:hover {
  color: var(--text);
}
.agenda__filtro--activo {
  background: var(--gold);
  border-color: var(--gold);
  color: var(--ink);
}

.agenda__grupo {
  margin-bottom: 36px;
}

.agenda__dia {
  font-family: var(--fuente-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--muted);
  margin: 0 0 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.agenda__dia--hoy {
  color: var(--gold);
  border-bottom-color: var(--gold-40);
}

.cita {
  display: flex;
  gap: 24px;
  padding: 20px 24px;
  margin-bottom: 2px;
  align-items: flex-start;
}

/* La hora se separa a la izquierda con tipografía monoespaciada: así todas
   quedan alineadas en columna y se recorre la jornada de un vistazo. */
.cita__hora {
  font-family: var(--fuente-mono);
  font-size: 18px;
  color: var(--gold);
  flex-shrink: 0;
  min-width: 62px;
  padding-top: 2px;
}

.cita__centro {
  flex: 1;
  min-width: 0;
}

.cita__cliente {
  font-family: var(--fuente-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--text);
}

.cita__servicio {
  font-size: 13px;
  color: var(--muted);
  margin-top: 4px;
}

.cita__telefono {
  display: inline-block;
  margin-top: 8px;
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--muted);
  text-decoration: none;
  border-bottom: 1px solid var(--border);
  transition: color 0.2s, border-color 0.2s;
}
.cita__telefono:hover {
  color: var(--gold);
  border-bottom-color: var(--gold);
}

.cita__notas {
  margin: 10px 0 0;
  font-size: 12px;
  font-style: italic;
  line-height: 1.6;
  color: var(--muted);
}

.cita__acciones {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.cita__botones {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.agenda__vacio {
  padding: 44px 24px;
  border: 1px dashed var(--border);
  text-align: center;
  color: var(--muted);
  font-size: 14px;
}

.agenda__error {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--danger);
  margin-bottom: 16px;
}

.agenda__cargando {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 700px) {
  .cita {
    flex-wrap: wrap;
    gap: 12px;
  }
  .cita__acciones {
    align-items: flex-start;
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }
}
</style>
