<template>
  <div class="q-pa-lg panel-pagina">
    <div class="bs-eyebrow">[05] Cuentas de acceso</div>
    <div class="row items-center justify-between q-mb-lg">
      <h1 class="panel-titulo q-my-sm">Usuarios</h1>
      <q-btn no-caps icon="add" label="Nueva cuenta" class="panel-boton" @click="abrirFormulario" />
    </div>

    <q-table
      :rows="usuarios"
      :columns="columnas"
      row-key="id"
      flat
      class="panel-tabla panel-tarjeta"
      :loading="cargando"
      no-data-label="No hay cuentas registradas"
    >
      <template #body-cell-rol="props">
        <q-td :props="props">
          <q-badge
            :color="props.row.rol === 'ADMIN' ? 'light-green-13' : 'blue-4'"
            :label="props.row.rol"
          />
        </q-td>
      </template>

      <template #body-cell-acciones="props">
        <q-td :props="props" class="text-right">
          <q-btn
            flat dense round icon="delete" color="red-4"
            @click="confirmarEliminar(props.row)"
          >
            <q-tooltip>Eliminar cuenta</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <p class="text-caption q-mt-md" style="color: var(--text-dim)">
      Las contraseñas se guardan cifradas con BCrypt y no se pueden consultar, ni siquiera
      desde aquí. Si un barbero olvida la suya, elimina la cuenta y créala de nuevo.
    </p>

    <!-- Formulario -->
    <q-dialog v-model="formularioAbierto">
      <q-card class="panel-dialogo" style="min-width: 420px">
        <q-card-section>
          <div class="bs-eyebrow">Nueva cuenta</div>
          <div class="panel-titulo-menor" style="font-size: 1.8rem">Crear acceso</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input v-model="formulario.nombre" label="Nombre para mostrar" dark class="panel-campo" />
          <q-input v-model="formulario.username" label="Usuario *" dark class="panel-campo" />
          <q-input v-model="formulario.password" type="password" label="Contraseña *" dark class="panel-campo" />

          <q-select
            v-model="formulario.rol"
            :options="['ADMIN', 'BARBERO']"
            label="Rol *"
            dark
            class="panel-campo"
          />

          <!--
            El barbero se elige de una lista, no se teclea el id: así la cuenta
            queda vinculada a una ficha que existe de verdad. El backend rechaza
            un BARBERO sin barberoId, porque sin él su agenda saldría vacía.
          -->
          <q-select
            v-if="formulario.rol === 'BARBERO'"
            v-model="formulario.barberoId"
            :options="opcionesBarberos"
            option-value="id"
            option-label="nombre"
            emit-value
            map-options
            label="Barbero vinculado *"
            dark
            class="panel-campo"
          />

          <div v-if="formulario.rol === 'ADMIN'" class="text-caption" style="color: var(--warning)">
            <q-icon name="info" size="14px" /> Un administrador tiene control total: gestiona
            catálogo, barberos, citas y cuentas.
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat no-caps label="Cancelar" color="grey-5" v-close-popup />
          <q-btn no-caps label="Crear cuenta" class="panel-boton" :loading="guardando" @click="guardar" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { createUsuario, deleteUsuario, getBarberos, getUsuarios } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'AdminUsuariosPage' })

const $q = useQuasar()
const auth = useAuthStore()

const usuarios = ref([])
const barberos = ref([])
const cargando = ref(false)
const guardando = ref(false)
const formularioAbierto = ref(false)

const formulario = ref({ nombre: '', username: '', password: '', rol: 'BARBERO', barberoId: null })

const columnas = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'username', label: 'Usuario', field: 'username', align: 'left', sortable: true },
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  { name: 'rol', label: 'Rol', field: 'rol', align: 'left', sortable: true },
  { name: 'barberoId', label: 'Barbero', field: (f) => nombreBarbero(f.barberoId), align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' },
]

const opcionesBarberos = computed(() => barberos.value)

function nombreBarbero(id) {
  if (!id) return '—'
  return barberos.value.find((b) => b.id === id)?.nombre || `#${id}`
}

function avisarError(e, porDefecto) {
  if (e.response?.status === 401) return
  $q.notify({
    type: 'negative',
    message: porDefecto,
    caption: e.response?.data?.mensaje || e.message,
    position: 'top',
  })
}

async function cargar() {
  cargando.value = true
  try {
    // allSettled y no all: si falla la lista de barberos, la de usuarios sigue
    // mostrándose (solo se degradan los nombres a "#id").
    const [resUsuarios, resBarberos] = await Promise.allSettled([getUsuarios(), getBarberos()])
    if (resUsuarios.status === 'fulfilled') usuarios.value = resUsuarios.value.data || []
    else avisarError(resUsuarios.reason, 'No se pudieron cargar las cuentas')
    barberos.value = resBarberos.status === 'fulfilled' ? resBarberos.value.data || [] : []
  } finally {
    cargando.value = false
  }
}

function abrirFormulario() {
  formulario.value = { nombre: '', username: '', password: '', rol: 'BARBERO', barberoId: null }
  formularioAbierto.value = true
}

async function guardar() {
  const f = formulario.value
  if (!f.username || !f.password) {
    $q.notify({ type: 'warning', message: 'Usuario y contraseña son obligatorios', position: 'top' })
    return
  }
  if (f.rol === 'BARBERO' && !f.barberoId) {
    $q.notify({ type: 'warning', message: 'Elige el barbero a vincular', position: 'top' })
    return
  }
  guardando.value = true
  try {
    await createUsuario({
      nombre: f.nombre,
      username: f.username,
      password: f.password,
      rol: f.rol,
      // El backend ignora el barberoId de un ADMIN, pero se manda null igualmente
      // para que la intención quede explícita en la petición.
      barberoId: f.rol === 'BARBERO' ? f.barberoId : null,
    })
    formularioAbierto.value = false
    $q.notify({ message: 'Cuenta creada', color: 'grey-9', icon: 'check', position: 'top' })
    await cargar()
  } catch (e) {
    avisarError(e, 'No se pudo crear la cuenta')
  } finally {
    guardando.value = false
  }
}

function confirmarEliminar(usuario) {
  if (usuario.username === auth.usuario) {
    $q.notify({ type: 'warning', message: 'No puedes eliminar tu propia cuenta', position: 'top' })
    return
  }
  $q.dialog({
    title: 'Eliminar cuenta',
    message: `¿Eliminar el acceso de "${usuario.username}"? Dejará de poder iniciar sesión.`,
    cancel: true,
    persistent: true,
    class: 'panel-dialogo',
  }).onOk(async () => {
    try {
      await deleteUsuario(usuario.id)
      $q.notify({ message: 'Cuenta eliminada', color: 'grey-9', position: 'top' })
      await cargar()
    } catch (e) {
      avisarError(e, 'No se pudo eliminar la cuenta')
    }
  })
}

onMounted(cargar)
</script>
