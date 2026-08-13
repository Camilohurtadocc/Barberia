<template>
  <div class="q-pa-lg panel-pagina">
    <div class="bs-eyebrow">[06] Página principal</div>
    <div class="row items-center justify-between q-mb-lg">
      <h1 class="panel-titulo q-my-sm">Sitio</h1>
      <q-btn no-caps label="Guardar cambios ✓" class="panel-boton" :loading="guardando" @click="guardar" />
    </div>

    <div v-if="cargando" class="q-py-xl text-center">
      <q-spinner color="light-green-13" size="40px" />
    </div>

    <div v-else class="row q-col-gutter-lg">
      <!-- Portada -->
      <div class="col-12 col-md-6">
        <div class="panel-tarjeta q-pa-lg">
          <div class="bs-eyebrow q-mb-md">Portada</div>
          <q-input v-model="config.heroTitulo" label="Titular" dark class="panel-campo q-mb-md" />
          <q-input v-model="config.heroSubtitulo" type="textarea" autogrow label="Subtítulo" dark class="panel-campo q-mb-md" />
          <SelectorImagen v-model="config.heroImagenUrl" label="Imagen de portada" />
        </div>

        <div class="panel-tarjeta q-pa-lg q-mt-lg">
          <div class="bs-eyebrow q-mb-md">Bloque "sobre nosotros"</div>
          <q-input v-model="config.sobreTexto" type="textarea" autogrow label="Texto" dark class="panel-campo q-mb-md" />
          <SelectorImagen v-model="config.sobreImagenUrl" label="Imagen del bloque" />
        </div>

        <div class="panel-tarjeta q-pa-lg q-mt-lg">
          <div class="bs-eyebrow q-mb-md">Contacto</div>
          <q-input v-model="config.direccion" label="Dirección" dark class="panel-campo q-mb-md" />
          <q-input v-model="config.telefono" label="Teléfono" dark class="panel-campo q-mb-md" />
          <q-input v-model="config.instagram" label="Instagram" dark class="panel-campo" />
        </div>
      </div>

      <!-- Cinta animada -->
      <div class="col-12 col-md-6">
        <div class="panel-tarjeta q-pa-lg">
          <div class="bs-eyebrow q-mb-sm">Cinta animada</div>
          <p class="text-caption q-mb-md" style="color: var(--text-dim)">
            Las frases se muestran encadenadas y en bucle, separadas por un punto medio.
          </p>

          <!-- Vista previa en vivo: se ve el resultado sin salir de la pantalla -->
          <div class="ticker q-mb-md">
            <div class="ticker__track">
              <div class="ticker__text">{{ previaTicker }}</div>
              <div class="ticker__text">{{ previaTicker }}</div>
            </div>
          </div>

          <div v-for="(m, i) in config.tickerMensajes" :key="i" class="row items-center q-mb-sm">
            <q-input v-model="config.tickerMensajes[i]" dark dense outlined class="panel-campo col" :label="`Frase ${i + 1}`" />
            <q-btn flat dense round icon="close" color="red-4" class="q-ml-sm" @click="config.tickerMensajes.splice(i, 1)" />
          </div>

          <q-btn flat no-caps icon="add" label="Añadir frase" class="panel-boton panel-boton--fantasma q-mt-sm" @click="config.tickerMensajes.push('')" />
        </div>

        <!-- Portafolio -->
        <div class="panel-tarjeta q-pa-lg q-mt-lg">
          <div class="row items-center justify-between q-mb-md">
            <div class="bs-eyebrow">Galería de trabajos</div>
            <q-btn flat dense no-caps icon="add" label="Añadir" class="panel-boton panel-boton--fantasma" @click="abrirItem()" />
          </div>

          <div v-if="!portafolio.length" class="text-caption" style="color: var(--text-dim)">
            Todavía no hay fotos en la galería.
          </div>

          <div v-for="item in portafolio" :key="item.id" class="row items-center q-mb-sm item-galeria">
            <img :src="urlImagen(item.imagenUrl)" :alt="item.titulo" class="miniatura" />
            <div class="col q-ml-md">
              <div style="font-weight: 700">{{ item.titulo }}</div>
              <div class="bs-eyebrow" style="color: var(--text-dim)">{{ item.categoria }}</div>
            </div>
            <q-btn flat dense round icon="edit" color="grey-5" @click="abrirItem(item)" />
            <q-btn flat dense round icon="delete" color="red-4" @click="eliminarItem(item)" />
          </div>
        </div>
      </div>
    </div>

    <!-- Alta/edición de una foto del portafolio -->
    <q-dialog v-model="itemAbierto">
      <q-card class="panel-dialogo" style="min-width: 460px">
        <q-card-section>
          <div class="bs-eyebrow">Galería</div>
          <div class="panel-titulo-menor" style="font-size: 1.8rem">
            {{ item.id ? 'Editar foto' : 'Nueva foto' }}
          </div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="item.titulo" label="Título *" dark class="panel-campo" />
          <q-input v-model="item.categoria" label="Categoría (FADE, CLASSIC...)" dark class="panel-campo" />
          <q-input v-model.number="item.orden" type="number" label="Orden" dark class="panel-campo" />
          <SelectorImagen v-model="item.imagenUrl" label="Imagen" />
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat no-caps label="Cancelar" color="grey-5" v-close-popup />
          <q-btn no-caps label="Guardar" class="panel-boton" :loading="guardandoItem" @click="guardarItem" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import SelectorImagen from '@/components/SelectorImagen.vue'
import {
  createPortafolio,
  deletePortafolio,
  getConfiguracion,
  getPortafolio,
  updateConfiguracion,
  updatePortafolio,
  urlImagen,
} from '@/services/api'

defineOptions({ name: 'AdminSitioPage' })

const $q = useQuasar()

const config = ref({ tickerMensajes: [] })
const portafolio = ref([])
const cargando = ref(false)
const guardando = ref(false)

const itemAbierto = ref(false)
const guardandoItem = ref(false)
const item = ref({ titulo: '', categoria: '', imagenUrl: '', orden: null })

const previaTicker = computed(() => {
  const frases = (config.value.tickerMensajes || []).filter(Boolean)
  if (!frases.length) return 'Añade frases para ver la cinta'
  return frases.join(' · ') + ' · '
})

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
    const [resConfig, resPort] = await Promise.allSettled([getConfiguracion(), getPortafolio()])
    if (resConfig.status === 'fulfilled') {
      config.value = { tickerMensajes: [], ...resConfig.value.data }
      // El backend puede devolver null si nunca se guardó: el v-for necesita array.
      if (!Array.isArray(config.value.tickerMensajes)) config.value.tickerMensajes = []
    } else {
      avisarError(resConfig.reason, 'No se pudo cargar la configuración')
    }
    portafolio.value = resPort.status === 'fulfilled' ? resPort.value.data || [] : []
  } finally {
    cargando.value = false
  }
}

async function guardar() {
  guardando.value = true
  try {
    // Se descartan las frases vacías: una cinta con huecos se ve como un error.
    const payload = {
      ...config.value,
      tickerMensajes: (config.value.tickerMensajes || []).map((m) => m.trim()).filter(Boolean),
    }
    const { data } = await updateConfiguracion(payload)
    config.value = { tickerMensajes: [], ...data }
    $q.notify({ message: 'Página principal actualizada', color: 'grey-9', icon: 'check', position: 'top' })
  } catch (e) {
    avisarError(e, 'No se pudieron guardar los cambios')
  } finally {
    guardando.value = false
  }
}

function abrirItem(existente) {
  item.value = existente
    ? { ...existente }
    : { titulo: '', categoria: '', imagenUrl: '', orden: portafolio.value.length + 1 }
  itemAbierto.value = true
}

async function guardarItem() {
  if (!item.value.titulo) {
    $q.notify({ type: 'warning', message: 'El título es obligatorio', position: 'top' })
    return
  }
  guardandoItem.value = true
  try {
    if (item.value.id) await updatePortafolio(item.value.id, item.value)
    else await createPortafolio(item.value)
    itemAbierto.value = false
    await cargar()
    $q.notify({ message: 'Galería actualizada', color: 'grey-9', position: 'top' })
  } catch (e) {
    avisarError(e, 'No se pudo guardar la foto')
  } finally {
    guardandoItem.value = false
  }
}

function eliminarItem(it) {
  $q.dialog({
    title: 'Eliminar foto',
    message: `¿Quitar "${it.titulo}" de la galería?`,
    cancel: true,
    persistent: true,
    class: 'panel-dialogo',
  }).onOk(async () => {
    try {
      await deletePortafolio(it.id)
      await cargar()
    } catch (e) {
      avisarError(e, 'No se pudo eliminar')
    }
  })
}

onMounted(cargar)
</script>

<style lang="scss" scoped>
.item-galeria {
  border: 1px solid var(--border);
  padding: 8px;
}

.miniatura {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border: 1px solid var(--border-strong);
}
</style>
