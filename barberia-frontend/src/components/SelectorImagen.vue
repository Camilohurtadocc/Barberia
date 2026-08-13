<template>
  <div>
    <div v-if="label" class="bs-eyebrow q-mb-sm">{{ label }}</div>

    <div class="row items-start q-col-gutter-md">
      <!-- Vista previa: sin esto no se sabe qué imagen quedó guardada -->
      <div class="col-auto">
        <div class="vista-previa">
          <img v-if="urlPrevia" :src="urlPrevia" alt="Vista previa" class="foto-mockup" />
          <q-icon v-else name="image" size="32px" color="grey-7" />
        </div>
      </div>

      <div class="col">
        <q-file
          v-model="archivo"
          dark
          dense
          outlined
          class="input-neon"
          label="Elegir imagen del equipo"
          accept="image/*"
          :loading="subiendo"
          @update:model-value="subir"
        >
          <template #prepend><q-icon name="folder_open" /></template>
        </q-file>

        <!--
          El campo de URL se mantiene junto al selector: las fotos de ejemplo son
          enlaces externos, y obligar a descargarlas y volverlas a subir solo para
          cambiar una dirección sería absurdo. Los dos caminos escriben el mismo dato.
        -->
        <q-input
          :model-value="modelValue"
          dark
          dense
          outlined
          class="input-neon q-mt-sm"
          label="...o pega una URL"
          @update:model-value="(v) => emit('update:modelValue', v)"
        />

        <div class="text-caption q-mt-xs" style="color: var(--text-dim)">
          JPG, PNG, WEBP, GIF o AVIF. Máximo 5 MB.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useQuasar } from 'quasar'
import { uploadArchivo, urlImagen } from '@/services/api'

defineOptions({ name: 'SelectorImagen' })

const props = defineProps({
  /** Valor guardado: ruta /api/archivos/... o URL externa. */
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const $q = useQuasar()
const archivo = ref(null)
const subiendo = ref(false)

const urlPrevia = computed(() => urlImagen(props.modelValue))

async function subir(fichero) {
  if (!fichero) return
  subiendo.value = true
  try {
    const { data } = await uploadArchivo(fichero)
    // Se guarda la ruta relativa que devuelve el backend, no la URL absoluta:
    // así el dato sigue siendo válido si mañana el gateway cambia de dominio.
    emit('update:modelValue', data.url)
    $q.notify({ message: 'Imagen subida', color: 'grey-9', icon: 'check', position: 'top' })
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: 'No se pudo subir la imagen',
      caption: e.response?.data?.mensaje || e.message,
      position: 'top',
    })
  } finally {
    // Se limpia para poder volver a elegir el MISMO archivo: q-file no dispara
    // el evento si el valor no cambia.
    archivo.value = null
    subiendo.value = false
  }
}
</script>

<style lang="scss" scoped>
.vista-previa {
  width: 96px;
  height: 96px;
  border: 2px solid var(--border-strong);
  background: var(--bg-raised);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.vista-previa img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
