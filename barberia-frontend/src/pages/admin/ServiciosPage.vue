<template>
  <div class="q-pa-lg panel-pagina">
    <div class="row items-center justify-between q-mb-lg">
      <div>
        <div class="bs-eyebrow">[01] Catálogo</div>
        <h1 class="panel-titulo q-mt-sm q-mb-none" style="font-size: 2.2rem">SERVICIOS</h1>
      </div>
      <q-btn icon="add" label="Nuevo servicio" no-caps class="panel-boton" @click="abrirCrear" />
    </div>

    <div class="panel-tarjeta q-pa-sm">
      <q-table
        flat
        dark
        class="panel-tabla"
        row-key="id"
        :rows="servicios"
        :columns="columnas"
        :loading="cargando"
        :rows-per-page-options="[10, 25, 50, 0]"
        no-data-label="No hay servicios registrados"
        loading-label="Cargando servicios..."
      >
        <template #body-cell-precio="props">
          <q-td :props="props" class="text-amber-6 text-weight-bold">
            {{ formatearPrecio(props.row.precio) }}
          </q-td>
        </template>

        <template #body-cell-duracionMinutos="props">
          <q-td :props="props">{{ props.row.duracionMinutos }} min</q-td>
        </template>

        <template #body-cell-acciones="props">
          <q-td :props="props" class="text-right">
            <q-btn flat dense round icon="edit" color="amber-6" @click="abrirEditar(props.row)">
              <q-tooltip>Editar</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="delete" color="red-4" @click="confirmarEliminar(props.row)">
              <q-tooltip>Eliminar</q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- Formulario crear / editar -->
    <q-dialog v-model="dialogo">
      <q-card class="panel-dialogo" style="width: 460px; max-width: 92vw">
        <q-card-section>
          <div class="bs-eyebrow">{{ editando ? 'Editar' : 'Crear' }}</div>
          <div class="panel-titulo q-mt-xs" style="font-size: 1.4rem">
            {{ editando ? 'ACTUALIZAR SERVICIO' : 'NUEVO SERVICIO' }}
          </div>
        </q-card-section>

        <q-card-section>
          <q-form class="q-gutter-md" @submit="guardar">
            <q-input
              v-model="formulario.nombre"
              filled
              dark
              label="Nombre"
              class="panel-campo"
              :rules="[(val) => !!val || 'El nombre es obligatorio']"
            />
            <q-input
              v-model="formulario.descripcion"
              filled
              dark
              type="textarea"
              rows="2"
              label="Descripción"
              class="panel-campo"
            />
            <q-input
              v-model.number="formulario.precio"
              filled
              dark
              type="number"
              step="0.01"
              label="Precio"
              class="panel-campo"
              :rules="[(val) => (val !== null && val !== '' && val >= 0) || 'Precio inválido']"
            />
            <q-input
              v-model.number="formulario.duracionMinutos"
              filled
              dark
              type="number"
              label="Duración (minutos)"
              class="panel-campo"
              :rules="[(val) => (val > 0) || 'La duración debe ser mayor a 0']"
            />

            <div class="row justify-end q-gutter-sm q-mt-md">
              <q-btn flat no-caps label="Cancelar" style="color: #888" @click="dialogo = false" />
              <q-btn
                type="submit"
                no-caps
                :label="editando ? 'Guardar cambios' : 'Crear'"
                class="panel-boton"
                :loading="guardando"
              />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { createServicio, deleteServicio, getServicios, updateServicio } from '@/services/api'

defineOptions({ name: 'AdminServiciosPage' })

const $q = useQuasar()

const columnas = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left', sortable: true },
  { name: 'descripcion', label: 'Descripción', field: 'descripcion', align: 'left' },
  { name: 'precio', label: 'Precio', field: 'precio', align: 'left', sortable: true },
  {
    name: 'duracionMinutos',
    label: 'Duración',
    field: 'duracionMinutos',
    align: 'left',
    sortable: true,
  },
  { name: 'acciones', label: 'Acciones', field: 'acciones', align: 'right' },
]

const servicios = ref([])
const cargando = ref(false)
const guardando = ref(false)
const dialogo = ref(false)
const editando = ref(false)
const formulario = ref(vacio())

function vacio() {
  return { id: null, nombre: '', descripcion: '', precio: null, duracionMinutos: null }
}

function formatearPrecio(valor) {
  if (valor === null || valor === undefined) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(valor)
}

function notificarError(e, accionFallida) {
  // El 401 ya lo maneja el interceptor global (cierra sesión y redirige).
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
  try {
    const { data } = await getServicios()
    servicios.value = data || []
  } catch (e) {
    notificarError(e, 'cargar los servicios')
  } finally {
    cargando.value = false
  }
}

function abrirCrear() {
  formulario.value = vacio()
  editando.value = false
  dialogo.value = true
}

function abrirEditar(servicio) {
  formulario.value = { ...servicio }
  editando.value = true
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  try {
    const payload = {
      nombre: formulario.value.nombre,
      descripcion: formulario.value.descripcion,
      precio: formulario.value.precio,
      duracionMinutos: formulario.value.duracionMinutos,
    }
    if (editando.value) {
      await updateServicio(formulario.value.id, payload)
      notificarOk('Servicio actualizado')
    } else {
      await createServicio(payload)
      notificarOk('Servicio creado')
    }
    dialogo.value = false
    await cargar()
  } catch (e) {
    notificarError(e, editando.value ? 'actualizar el servicio' : 'crear el servicio')
  } finally {
    guardando.value = false
  }
}

function confirmarEliminar(servicio) {
  $q.dialog({
    title: 'Eliminar servicio',
    message: `¿Seguro que quieres eliminar "${servicio.nombre}"? Esta acción no se puede deshacer.`,
    cancel: { flat: true, label: 'Cancelar', color: 'grey' },
    ok: { label: 'Eliminar', color: 'red-8' },
    dark: true,
    persistent: true,
  }).onOk(async () => {
    try {
      await deleteServicio(servicio.id)
      notificarOk('Servicio eliminado')
      await cargar()
    } catch (e) {
      notificarError(e, 'eliminar el servicio')
    }
  })
}

onMounted(cargar)
</script>
