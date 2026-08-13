<script setup>
import { computed, ref, watch } from 'vue'
import RotuloSeccion from '@/components/RotuloSeccion.vue'
import { createCita } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

/**
 * Formulario de reserva. Es el único punto de la landing que escribe en el
 * backend.
 *
 * Reservar NO exige cuenta: pedir registro antes de dejar pedir hora hace perder
 * reservas. Quien sí tiene sesión abierta gana dos cosas: el formulario le llega
 * relleno y la cita queda ligada a su historial (eso lo hace el backend leyendo
 * el token, aquí no hay que mandar nada).
 */
const props = defineProps({
  servicios: { type: Array, default: () => [] },
  barberos: { type: Array, default: () => [] },
  config: { type: Object, default: () => ({}) },
  servicioId: { type: [Number, String], default: null },
  barberoId: { type: [Number, String], default: null },
  slot: { type: String, default: null },
  /** Datos de contacto de la cuenta, si hay sesión de cliente. */
  perfil: { type: Object, default: null },
})

const emit = defineEmits(['update:servicioId', 'update:barberoId', 'reservada'])

const auth = useAuthStore()

const formulario = ref({
  clienteNombre: '',
  clienteTelefono: '',
  fecha: '',
  hora: '',
  notas: '',
})

const enviando = ref(false)
const error = ref('')
const confirmada = ref(null)

// El perfil llega de forma asíncrona, después del primer render. Sin este watch
// el formulario se quedaría vacío para un cliente identificado.
watch(
  () => props.perfil,
  (p) => {
    if (!p) return
    if (!formulario.value.clienteNombre) formulario.value.clienteNombre = p.nombre || ''
    if (!formulario.value.clienteTelefono) formulario.value.clienteTelefono = p.telefono || ''
  },
  { immediate: true },
)

// La hora elegida en la sección de Equipo cae aquí: son el mismo dato y tenerlos
// separados obligaría a escribirla dos veces.
watch(
  () => props.slot,
  (h) => {
    if (h) formulario.value.hora = h
  },
  { immediate: true },
)

const hoy = new Date().toISOString().split('T')[0]

const barberoElegido = computed(() => props.barberos.find((b) => b.id === props.barberoId) || null)

const horario = computed(() => {
  const t = props.config.telefono || '+57 310 234 5678'
  return {
    direccion: props.config.direccion || 'Cra 7 #85-32, Chapinero, Bogotá',
    dias: 'Lunes a Sábado · 9:00–20:00',
    telefono: t,
  }
})

const faltante = computed(() => {
  if (!props.servicioId) return 'Selecciona un servicio'
  if (!formulario.value.clienteNombre.trim()) return 'Escribe tu nombre'
  if (!formulario.value.clienteTelefono.trim()) return 'Escribe tu WhatsApp'
  if (!formulario.value.fecha) return 'Elige la fecha'
  if (!formulario.value.hora) return 'Elige la hora'
  return null
})

async function reservar() {
  if (faltante.value) return
  enviando.value = true
  error.value = ''
  try {
    // El backend espera un LocalDateTime; date + time dan las dos mitades y se
    // unen con 'T', que es el separador ISO que Jackson entiende sin configurar
    // nada. Se añaden los segundos porque algunos navegadores devuelven "HH:mm".
    const hora = formulario.value.hora.length === 5 ? `${formulario.value.hora}:00` : formulario.value.hora
    const { data } = await createCita({
      clienteNombre: formulario.value.clienteNombre.trim(),
      clienteTelefono: formulario.value.clienteTelefono.trim(),
      servicioId: props.servicioId,
      barberoId: props.barberoId,
      barbero: barberoElegido.value?.nombre || null,
      fechaHora: `${formulario.value.fecha}T${hora}`,
      notas: formulario.value.notas.trim() || null,
    })
    confirmada.value = data
    formulario.value.notas = ''
    emit('reservada', data)
  } catch (e) {
    error.value =
      e.response?.data?.mensaje ||
      'No se pudo registrar la reserva. Inténtalo de nuevo en un momento.'
  } finally {
    enviando.value = false
  }
}

function otraReserva() {
  confirmada.value = null
  formulario.value.fecha = ''
  formulario.value.hora = ''
  emit('update:servicioId', null)
}

function formatearFecha(valor) {
  const f = new Date(valor)
  if (Number.isNaN(f.getTime())) return valor
  return f.toLocaleString('es-CO', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <section id="reservas" class="bs-seccion bs-fondo-ink reservas">
    <div class="reservas__halo" />

    <div class="reservas__contenedor">
      <RotuloSeccion numero="04" texto="Reservas" />

      <div class="reservas__columnas">
        <!-- Columna izquierda: el porqué y los datos de contacto -->
        <div>
          <h2 class="bs-h2">
            Tu turno,<br /><em class="bs-em">cuando quieras.</em>
          </h2>
          <p class="bs-parrafo reservas__intro">
            Sin llamadas. Sin filas. Confirmación instantánea por WhatsApp.
          </p>

          <div class="reservas__datos">
            <div class="reservas__dato">
              <span class="reservas__icono">◎</span>
              <span>{{ horario.direccion }}</span>
            </div>
            <div class="reservas__dato">
              <span class="reservas__icono">◷</span>
              <span>{{ horario.dias }}</span>
            </div>
            <div class="reservas__dato">
              <span class="reservas__icono">◈</span>
              <span>{{ horario.telefono }}</span>
            </div>
          </div>

          <p v-if="!auth.estaAutenticado" class="reservas__aviso">
            Puedes reservar sin cuenta. Si te registras, podrás consultar y cancelar tus citas
            desde tu área personal.
          </p>
        </div>

        <!-- Columna derecha: formulario o confirmación -->
        <div>
          <!-- Confirmación -->
          <div v-if="confirmada" class="confirmacion">
            <div class="confirmacion__marca">✓</div>
            <h3 class="confirmacion__titulo">Reserva registrada</h3>
            <dl class="confirmacion__lista">
              <div>
                <dt>Cliente</dt>
                <dd>{{ confirmada.clienteNombre }}</dd>
              </div>
              <div v-if="confirmada.barbero">
                <dt>Barbero</dt>
                <dd>{{ confirmada.barbero }}</dd>
              </div>
              <div>
                <dt>Cuándo</dt>
                <dd>{{ formatearFecha(confirmada.fechaHora) }}</dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd><span class="bs-estado bs-estado--pendiente">Pendiente</span></dd>
              </div>
            </dl>
            <p class="confirmacion__nota">
              Te confirmaremos por WhatsApp al {{ confirmada.clienteTelefono }}.
            </p>
            <div class="confirmacion__acciones">
              <button type="button" class="bs-btn bs-btn--fantasma" @click="otraReserva">
                Reservar otra
              </button>
              <RouterLink v-if="auth.esCliente" to="/mi-cuenta" class="bs-btn bs-btn--oro">
                Ver mis citas
              </RouterLink>
            </div>
          </div>

          <!-- Formulario -->
          <form v-else novalidate @submit.prevent="reservar">
            <label class="bs-campo">
              <span class="bs-campo__label">Servicio</span>
              <select
                class="bs-input"
                :value="servicioId ?? ''"
                @change="emit('update:servicioId', $event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">Elige un servicio…</option>
                <option v-for="s in servicios" :key="s.id" :value="s.id">
                  {{ s.nombre }} — ${{ s.precio }} USD · {{ s.duracionMinutos }} min
                </option>
              </select>
            </label>

            <label class="bs-campo">
              <span class="bs-campo__label">Barbero</span>
              <select
                class="bs-input"
                :value="barberoId ?? ''"
                @change="emit('update:barberoId', $event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">Cualquiera disponible</option>
                <option v-for="b in barberos" :key="b.id" :value="b.id">
                  {{ b.nombre }}<template v-if="b.especialidad"> — {{ b.especialidad }}</template>
                </option>
              </select>
            </label>

            <label class="bs-campo">
              <span class="bs-campo__label">Nombre completo</span>
              <input v-model="formulario.clienteNombre" class="bs-input" placeholder="Juan Pérez" />
            </label>

            <label class="bs-campo">
              <span class="bs-campo__label">WhatsApp</span>
              <input
                v-model="formulario.clienteTelefono"
                class="bs-input"
                placeholder="+57 300 000 0000"
              />
            </label>

            <div class="reservas__par">
              <label class="bs-campo">
                <span class="bs-campo__label">Fecha</span>
                <!-- `min` evita el error más habitual: pedir hora para ayer. -->
                <input v-model="formulario.fecha" type="date" :min="hoy" class="bs-input" />
              </label>
              <label class="bs-campo">
                <span class="bs-campo__label">Hora</span>
                <input v-model="formulario.hora" type="time" class="bs-input" />
              </label>
            </div>

            <label class="bs-campo">
              <span class="bs-campo__label">Notas adicionales</span>
              <textarea
                v-model="formulario.notas"
                class="bs-input bs-input--area"
                rows="3"
                placeholder="Referencia de corte, alergias, detalles…"
              />
            </label>

            <p v-if="error" class="reservas__error">{{ error }}</p>

            <!--
              El botón deshabilitado dice QUÉ falta en vez de un «Reservar» apagado
              sin explicación: con siete campos, adivinar cuál queda pendiente es
              el motivo más común de abandono.
            -->
            <button
              type="submit"
              class="bs-btn bs-btn--oro bs-btn--bloque"
              :disabled="!!faltante || enviando"
            >
              {{ enviando ? 'Enviando…' : faltante || 'Confirmar reserva' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.reservas {
  position: relative;
  overflow: hidden;
}

/* Halo dorado muy tenue en la esquina: rompe la planitud del fondo sin
   introducir ninguna forma reconocible. */
.reservas__halo {
  position: absolute;
  right: -120px;
  top: -120px;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 169, 110, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

.reservas__contenedor {
  max-width: 1000px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.reservas__columnas {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 80px;
  align-items: start;
}

.reservas__intro {
  margin: 20px 0 36px;
}

.reservas__datos {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reservas__dato {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
}

.reservas__icono {
  font-family: var(--fuente-mono);
  font-size: 14px;
  color: var(--gold);
  flex-shrink: 0;
}

.reservas__aviso {
  margin-top: 32px;
  padding: 14px 16px;
  border-left: 2px solid var(--gold-40);
  font-size: 12px;
  line-height: 1.7;
  color: var(--muted);
}

.reservas__par {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.reservas__error {
  margin: 0 0 12px;
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: var(--danger);
}

.confirmacion {
  border: 1px solid var(--gold-40);
  background: var(--gold-15);
  padding: 32px;
}

.confirmacion__marca {
  width: 44px;
  height: 44px;
  border: 1px solid var(--gold);
  color: var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 20px;
}

.confirmacion__titulo {
  font-family: var(--fuente-display);
  font-size: 26px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 20px;
}

.confirmacion__lista {
  margin: 0 0 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.confirmacion__lista > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}
.confirmacion__lista dt {
  font-family: var(--fuente-mono);
  font-size: 9px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted);
}
.confirmacion__lista dd {
  margin: 0;
  font-size: 13px;
  color: var(--text);
  text-align: right;
}

.confirmacion__nota {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.7;
  margin: 0 0 24px;
}

.confirmacion__acciones {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .reservas__columnas {
    grid-template-columns: 1fr;
    gap: 48px;
  }
}
</style>
