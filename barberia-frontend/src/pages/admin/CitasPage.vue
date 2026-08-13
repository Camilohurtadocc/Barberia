<template>
  <div class="q-pa-lg panel-pagina">
    <div class="row items-center justify-between q-mb-lg">
      <div>
        <div class="bs-eyebrow">[03] Agenda</div>
        <h1 class="panel-titulo q-mt-sm q-mb-none" style="font-size: 2.2rem">CITAS</h1>
      </div>
      <q-btn icon="refresh" label="Actualizar" no-caps class="panel-boton panel-boton--fantasma" @click="cargar" />
    </div>

    <div class="row q-gutter-sm q-mb-md">
      <q-btn
        v-for="filtro in filtros"
        :key="filtro"
        dense
        no-caps
        flat
        :label="filtro"
        :class="['filtro-btn', { activo: estadoFiltro === filtro }]"
        @click="estadoFiltro = filtro"
      />
    </div>

    <div class="panel-tarjeta q-pa-sm">
      <q-table
        flat
        dark
        class="panel-tabla"
        row-key="id"
        :rows="citasFiltradas"
        :columns="columnas"
        :loading="cargando"
        :rows-per-page-options="[10, 25, 50, 0]"
        no-data-label="No hay citas para este filtro"
        loading-label="Cargando citas..."
      >
        <template #body-cell-fechaHora="props">
          <q-td :props="props">{{ formatearFecha(props.row.fechaHora) }}</q-td>
        </template>

        <template #body-cell-servicioId="props">
          <q-td :props="props">{{ nombreServicio(props.row.servicioId) }}</q-td>
        </template>

        <template #body-cell-estado="props">
          <q-td :props="props">
            <q-badge :color="colorEstado(props.row.estado)" :label="props.row.estado || '—'" />
          </q-td>
        </template>

        <template #body-cell-acciones="props">
          <q-td :props="props" class="text-right">
            <q-btn
              v-if="props.row.estado !== 'CONFIRMADA' && props.row.estado !== 'COMPLETADA'"
              flat
              dense
              no-caps
              icon="check_circle"
              label="Confirmar"
              color="green-4"
              :disable="actualizando === props.row.id"
              @click="cambiarEstado(props.row, 'CONFIRMADA')"
            />
            <q-btn
              v-if="props.row.estado === 'CONFIRMADA'"
              flat
              dense
              no-caps
              icon="task_alt"
              label="Completar"
              color="light-blue-4"
              :disable="actualizando === props.row.id"
              @click="cambiarEstado(props.row, 'COMPLETADA')"
            />
            <q-btn
              v-if="props.row.estado !== 'CANCELADA'"
              flat
              dense
              no-caps
              icon="cancel"
              label="Cancelar"
              color="red-4"
              :disable="actualizando === props.row.id"
              @click="confirmarCancelar(props.row)"
            />
            <q-btn
              flat
              dense
              round
              icon="delete_forever"
              color="grey-6"
              :disable="actualizando === props.row.id"
              @click="confirmarEliminar(props.row)"
            >
              <q-tooltip>Eliminar definitivamente</q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { deleteCita, getCitas, getServicios, updateCitaEstado } from '@/services/api'

defineOptions({ name: 'AdminCitasPage' })

const $q = useQuasar()

const columnas = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'clienteNombre', label: 'Cliente', field: 'clienteNombre', align: 'left', sortable: true },
  { name: 'clienteTelefono', label: 'Teléfono', field: 'clienteTelefono', align: 'left' },
  { name: 'servicioId', label: 'Servicio', field: 'servicioId', align: 'left' },
  { name: 'barbero', label: 'Barbero', field: 'barbero', align: 'left' },
  { name: 'fechaHora', label: 'Fecha y hora', field: 'fechaHora', align: 'left', sortable: true },
  { name: 'estado', label: 'Estado', field: 'estado', align: 'left', sortable: true },
  { name: 'acciones', label: 'Acciones', field: 'acciones', align: 'right' },
]

const filtros = ['TODAS', 'PENDIENTE', 'CONFIRMADA', 'COMPLETADA', 'CANCELADA']

const citas = ref([])
const servicios = ref([])
const cargando = ref(false)
const actualizando = ref(null)
const estadoFiltro = ref('TODAS')

const citasFiltradas = computed(() =>
  estadoFiltro.value === 'TODAS'
    ? citas.value
    : citas.value.filter((c) => c.estado === estadoFiltro.value),
)

function nombreServicio(id) {
  return servicios.value.find((s) => s.id === id)?.nombre || `#${id ?? '—'}`
}

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
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function notificarError(e, accionFallida) {
  if (e.response?.status === 401) return
  $q.notify({
    message: `No se pudo ${accionFallida}`,
    caption: e.response?.data?.mensaje || e.message,
    color: 'red-9',
    icon: 'error_outline',
    position: 'top',
  })
}

function notificarOk(mensaje) {
  $q.notify({ message: mensaje, color: 'amber-8', textColor: 'black', icon: 'check', position: 'top' })
}

async function cargar() {
  cargando.value = true
  const [resCitas, resServicios] = await Promise.allSettled([getCitas(), getServicios()])

  if (resCitas.status === 'fulfilled') {
    citas.value = resCitas.value.data || []
  } else {
    notificarError(resCitas.reason, 'cargar las citas')
  }
  // El nombre del servicio es un adorno: si falla, se muestra el id.
  servicios.value = resServicios.status === 'fulfilled' ? resServicios.value.data || [] : []
  cargando.value = false
}

async function cambiarEstado(cita, estado) {
  actualizando.value = cita.id
  try {
    await updateCitaEstado(cita.id, estado)
    notificarOk(`Cita #${cita.id} → ${estado}`)
    await cargar()
  } catch (e) {
    notificarError(e, `cambiar la cita a ${estado}`)
  } finally {
    actualizando.value = null
  }
}

function confirmarCancelar(cita) {
  $q.dialog({
    title: 'Cancelar cita',
    message: `¿Cancelar la cita de ${cita.clienteNombre}?`,
    cancel: { flat: true, label: 'Volver', color: 'grey' },
    ok: { label: 'Cancelar cita', color: 'red-8' },
    dark: true,
  }).onOk(() => cambiarEstado(cita, 'CANCELADA'))
}

function confirmarEliminar(cita) {
  $q.dialog({
    title: 'Eliminar cita',
    message: `Se borrará la cita #${cita.id} de ${cita.clienteNombre}. Esta acción no se puede deshacer.`,
    cancel: { flat: true, label: 'Cancelar', color: 'grey' },
    ok: { label: 'Eliminar', color: 'red-8' },
    dark: true,
    persistent: true,
  }).onOk(async () => {
    actualizando.value = cita.id
    try {
      await deleteCita(cita.id)
      notificarOk('Cita eliminada')
      await cargar()
    } catch (e) {
      notificarError(e, 'eliminar la cita')
    } finally {
      actualizando.value = null
    }
  })
}

onMounted(cargar)
</script>

<style lang="scss" scoped>
.filtro-btn {
  color: #888 !important;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50px;
  padding: 2px 16px;
  font-size: 0.72rem;
  letter-spacing: 1px;
}

.filtro-btn.activo {
  color: #ffd700 !important;
  border-color: rgba(255, 215, 0, 0.45);
  background: rgba(255, 215, 0, 0.08);
}
</style>
