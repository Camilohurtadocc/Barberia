<template>
  <div class="q-pa-lg panel-pagina">
    <div class="bs-eyebrow">[00] Resumen</div>
    <h1 class="panel-titulo q-mt-sm q-mb-lg" style="font-size: 2.2rem">DASHBOARD</h1>

    <div class="panel-cifras">
      <div v-for="stat in estadisticas" :key="stat.label" class="panel-tarjeta panel-tarjeta panel-caja-cifra">
        <q-icon :name="stat.icon" size="26px" :color="stat.color" />
        <div class="panel-cifra">{{ stat.valor }}</div>
        <div class="bs-eyebrow">{{ stat.label }}</div>
      </div>
    </div>

    <div class="row q-col-gutter-md q-mt-md">
      <div class="col-12 col-md-7">
        <div class="panel-tarjeta q-pa-md">
          <div class="panel-title">Próximas citas</div>
          <q-markup-table flat dark class="panel-tabla" v-if="proximas.length">
            <thead>
              <tr>
                <th class="text-left">Cliente</th>
                <th class="text-left">Fecha</th>
                <th class="text-left">Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cita in proximas" :key="cita.id">
                <td>{{ cita.clienteNombre }}</td>
                <td>{{ formatearFecha(cita.fechaHora) }}</td>
                <td>
                  <q-badge :color="colorEstado(cita.estado)" :label="cita.estado" />
                </td>
              </tr>
            </tbody>
          </q-markup-table>
          <div v-else class="vacio">No hay citas próximas.</div>
        </div>
      </div>

      <div class="col-12 col-md-5">
        <div class="panel-tarjeta q-pa-md">
          <div class="panel-title">Citas por estado</div>
          <div v-for="(cantidad, estado) in porEstado" :key="estado" class="estado-row">
            <span class="estado-nombre">
              <q-badge :color="colorEstado(estado)" :label="estado" />
            </span>
            <span class="estado-cantidad">{{ cantidad }}</span>
          </div>
          <div v-if="!Object.keys(porEstado).length" class="vacio">Sin datos.</div>
        </div>
      </div>
    </div>

    <q-inner-loading :showing="cargando" dark>
      <q-spinner-gears size="48px" color="amber-6" />
    </q-inner-loading>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { getBarberos, getCitas, getServicios } from '@/services/api'

defineOptions({ name: 'AdminDashboardPage' })

const $q = useQuasar()
const citas = ref([])
const servicios = ref([])
const barberos = ref([])
const cargando = ref(false)

const esHoy = (fechaHora) => {
  if (!fechaHora) return false
  const fecha = new Date(fechaHora)
  const hoy = new Date()
  return (
    fecha.getFullYear() === hoy.getFullYear() &&
    fecha.getMonth() === hoy.getMonth() &&
    fecha.getDate() === hoy.getDate()
  )
}

const citasHoy = computed(() => citas.value.filter((c) => esHoy(c.fechaHora)).length)
const pendientes = computed(() => citas.value.filter((c) => c.estado === 'PENDIENTE').length)

const estadisticas = computed(() => [
  { label: 'Citas totales', valor: citas.value.length, icon: 'event', color: 'amber-6' },
  { label: 'Citas hoy', valor: citasHoy.value, icon: 'today', color: 'light-blue-4' },
  { label: 'Pendientes', valor: pendientes.value, icon: 'pending_actions', color: 'orange-5' },
  { label: 'Servicios', valor: servicios.value.length, icon: 'content_cut', color: 'green-4' },
  { label: 'Barberos', valor: barberos.value.length, icon: 'groups', color: 'purple-4' },
])

const porEstado = computed(() =>
  citas.value.reduce((acc, cita) => {
    const estado = cita.estado || 'SIN ESTADO'
    acc[estado] = (acc[estado] || 0) + 1
    return acc
  }, {}),
)

const proximas = computed(() =>
  [...citas.value]
    .filter((c) => c.fechaHora)
    .sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora))
    .slice(0, 5),
)

function colorEstado(estado) {
  return (
    {
      PENDIENTE: 'orange-8',
      CONFIRMADA: 'green-8',
      COMPLETADA: 'blue-8',
      CANCELADA: 'red-9',
    }[estado] || 'grey-8'
  )
}

function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return valor
  return fecha.toLocaleString('es-CO', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function cargar() {
  cargando.value = true
  const resultados = await Promise.allSettled([getCitas(), getServicios(), getBarberos()])
  const [resCitas, resServicios, resBarberos] = resultados

  citas.value = resCitas.status === 'fulfilled' ? resCitas.value.data || [] : []
  servicios.value = resServicios.status === 'fulfilled' ? resServicios.value.data || [] : []
  barberos.value = resBarberos.status === 'fulfilled' ? resBarberos.value.data || [] : []

  if (resultados.some((r) => r.status === 'rejected')) {
    $q.notify({
      message: 'Algunos datos no se pudieron cargar desde el gateway',
      color: 'red-9',
      icon: 'cloud_off',
      position: 'top',
    })
  }
  cargando.value = false
}

onMounted(cargar)
</script>

<style lang="scss" scoped>
.panel-cifras {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
}

.panel-tarjeta panel-caja-cifra {
  padding: 22px 18px;
  text-align: center;
}

.panel-cifra {
  font-size: 2.2rem;
  font-weight: 800;
  color: #fff;
  margin-top: 6px;
  line-height: 1;
}

.bs-eyebrow {
  font-size: 0.65rem;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: #888;
  margin-top: 6px;
}

.panel-title {
  color: #ffd700;
  font-size: 0.7rem;
  letter-spacing: 3px;
  text-transform: uppercase;
  font-weight: 700;
  margin-bottom: 14px;
}

.estado-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.estado-cantidad {
  color: #fff;
  font-weight: 700;
}

.vacio {
  color: #666;
  font-size: 0.9rem;
  padding: 12px 0;
}
</style>
