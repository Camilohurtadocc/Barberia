<template>
  <div class="q-pa-lg panel-pagina">
    <div class="row items-center justify-between q-mb-lg">
      <div>
        <div class="bs-eyebrow">[02] Equipo</div>
        <h1 class="panel-titulo q-mt-sm q-mb-none" style="font-size: 2.2rem">BARBEROS</h1>
      </div>
      <q-btn icon="add" label="Nuevo barbero" no-caps class="panel-boton" @click="abrirCrear" />
    </div>

    <div class="panel-tarjeta q-pa-sm">
      <q-table
        flat
        dark
        class="panel-tabla"
        row-key="id"
        :rows="barberos"
        :columns="columnas"
        :loading="cargando"
        :rows-per-page-options="[10, 25, 50, 0]"
        no-data-label="No hay barberos registrados"
        loading-label="Cargando barberos..."
      >
        <template #body-cell-fotoUrl="props">
          <q-td :props="props">
            <q-avatar size="38px">
              <img v-if="props.row.fotoUrl" :src="urlImagen(props.row.fotoUrl)" :alt="props.row.nombre" />
              <q-icon v-else name="person" color="grey-7" />
            </q-avatar>
          </q-td>
        </template>

        <template #body-cell-redes="props">
          <q-td :props="props">
            <span v-if="props.row.instagram" class="red-social">📸 {{ props.row.instagram }}</span>
            <span v-if="props.row.facebook" class="red-social">📘 {{ props.row.facebook }}</span>
            <span v-if="!props.row.instagram && !props.row.facebook" style="color: #555">—</span>
          </q-td>
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

    <q-dialog v-model="dialogo">
      <q-card class="panel-dialogo" style="width: 460px; max-width: 92vw">
        <q-card-section>
          <div class="bs-eyebrow">{{ editando ? 'Editar' : 'Crear' }}</div>
          <div class="panel-titulo q-mt-xs" style="font-size: 1.4rem">
            {{ editando ? 'ACTUALIZAR BARBERO' : 'NUEVO BARBERO' }}
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
            <div class="row q-col-gutter-md">
              <q-input v-model="formulario.nombreCorto" filled dark label="Nombre corto (MARCOS V.)" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.cargo" filled dark label="Cargo (Master Barber)" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.especialidad" filled dark label="Especialidad" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.color" filled dark label="Color de acento (#39ff14)" class="panel-campo col-12 col-sm-6" />
              <q-input v-model.number="formulario.aniosExperiencia" type="number" filled dark label="Años de experiencia" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.cortes" filled dark label="Cortes realizados (8.4K)" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.instagram" filled dark label="Instagram" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="formulario.facebook" filled dark label="Facebook" class="panel-campo col-12 col-sm-6" />
              <q-input v-model="slotsTexto" filled dark label="Franjas horarias (separadas por coma)" hint="10:00, 11:30, 14:00" class="panel-campo col-12" />
            </div>

            <SelectorImagen v-model="formulario.fotoUrl" label="Foto principal" />

            <q-toggle v-model="formulario.activo" color="light-green-13" label="Activo (se muestra en la landing)" />

            <!--
              Al crear un barbero se ofrece crear su acceso en el mismo paso: si se
              dejara para después, es fácil acabar con barberos sin cuenta que no
              pueden entrar a ver su agenda.
            -->
            <div v-if="!editando" class="panel-tarjeta q-pa-md">
              <div class="bs-eyebrow q-mb-sm">Acceso del barbero</div>
              <q-toggle v-model="crearAcceso" color="light-green-13" label="Crear su usuario ahora" />
              <div v-if="crearAcceso" class="row q-col-gutter-md q-mt-sm">
                <q-input v-model="acceso.username" filled dark label="Usuario *" class="panel-campo col-12 col-sm-6" />
                <q-input v-model="acceso.password" filled dark label="Contraseña *" class="panel-campo col-12 col-sm-6" />
              </div>
            </div>

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
import SelectorImagen from '@/components/SelectorImagen.vue'
import {
  createBarbero,
  createUsuario,
  deleteBarbero,
  deleteUsuarioDeBarbero,
  getBarberos,
  updateBarbero,
  urlImagen,
} from '@/services/api'

defineOptions({ name: 'AdminBarberosPage' })

const $q = useQuasar()

const columnas = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'fotoUrl', label: 'Foto', field: 'fotoUrl', align: 'left' },
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left', sortable: true },
  { name: 'especialidad', label: 'Especialidad', field: 'especialidad', align: 'left', sortable: true },
  { name: 'redes', label: 'Redes', field: 'redes', align: 'left' },
  { name: 'acciones', label: 'Acciones', field: 'acciones', align: 'right' },
]

const barberos = ref([])
const cargando = ref(false)
const guardando = ref(false)
const dialogo = ref(false)
const editando = ref(false)
const formulario = ref(vacio())
const slotsTexto = ref('')
const crearAcceso = ref(true)
const acceso = ref({ username: '', password: '' })

function vacio() {
  return {
    id: null, nombre: '', nombreCorto: '', cargo: '', especialidad: '',
    aniosExperiencia: null, cortes: '', color: '#39ff14',
    instagram: '', facebook: '', fotoUrl: '', activo: true,
  }
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
  try {
    const { data } = await getBarberos()
    barberos.value = data || []
  } catch (e) {
    notificarError(e, 'cargar los barberos')
  } finally {
    cargando.value = false
  }
}

function abrirCrear() {
  formulario.value = vacio()
  slotsTexto.value = ''
  crearAcceso.value = true
  acceso.value = { username: '', password: '' }
  editando.value = false
  dialogo.value = true
}

function abrirEditar(barbero) {
  formulario.value = { ...barbero }
  slotsTexto.value = (barbero.slots || []).join(', ')
  editando.value = true
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  try {
    const payload = {
      ...formulario.value,
      slots: slotsTexto.value.split(',').map((s) => s.trim()).filter(Boolean),
    }
    if (editando.value) {
      await updateBarbero(formulario.value.id, payload)
      notificarOk('Barbero actualizado')
    } else {
      const { data } = await createBarbero(payload)
      // El alta son dos llamadas a dos microservicios distintos (barberos y auth),
      // así que no hay transacción que las una. Si la segunda falla, el barbero ya
      // existe: se avisa explícitamente para que se le cree el acceso a mano en
      // Usuarios, en vez de dejar el fallo en silencio.
      if (crearAcceso.value && acceso.value.username && acceso.value.password) {
        try {
          await createUsuario({
            username: acceso.value.username,
            password: acceso.value.password,
            rol: 'BARBERO',
            nombre: data.nombre,
            barberoId: data.id,
          })
        } catch (e) {
          notificarError(e, 'crear el acceso (el barbero SÍ se creó; añade su usuario desde Usuarios)')
        }
      }
      notificarOk('Barbero creado')
    }
    dialogo.value = false
    await cargar()
  } catch (e) {
    notificarError(e, editando.value ? 'actualizar el barbero' : 'crear el barbero')
  } finally {
    guardando.value = false
  }
}

function confirmarEliminar(barbero) {
  $q.dialog({
    title: 'Eliminar barbero',
    message: `¿Seguro que quieres eliminar a "${barbero.nombre}"? Esta acción no se puede deshacer.`,
    cancel: { flat: true, label: 'Cancelar', color: 'grey' },
    ok: { label: 'Eliminar', color: 'red-8' },
    dark: true,
    persistent: true,
  }).onOk(async () => {
    try {
      await deleteBarbero(barbero.id)
      // Su cuenta de acceso se va con él: dejarla viva permitiría iniciar sesión
      // a un barbero que ya no existe, con una agenda que nadie puede ver.
      // Va en try aparte porque el barbero YA se borró; que falle esto no debe
      // presentarse como si el borrado principal hubiera fallado.
      try {
        await deleteUsuarioDeBarbero(barbero.id)
      } catch {
        notificarOk('Barbero eliminado (revisa si quedó su usuario en Usuarios)')
        await cargar()
        return
      }
      notificarOk('Barbero eliminado')
      await cargar()
    } catch (e) {
      notificarError(e, 'eliminar el barbero')
    }
  })
}

onMounted(cargar)
</script>

<style lang="scss" scoped>
.red-social {
  display: block;
  font-size: 0.75rem;
  color: #aaa;
}
</style>
