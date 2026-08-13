<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DialogoAcceso from '@/components/DialogoAcceso.vue'
import { useAuthStore } from '@/stores/auth'

/**
 * Pantalla de acceso con URL propia.
 *
 * La landing ya abre el mismo diálogo sin cambiar de página, pero hace falta una
 * dirección a la que enviar a alguien: el enlace del pie, y sobre todo el guardia
 * de rutas cuando corta el paso a /admin. Antes ese corte devolvía a la portada
 * con un parámetro en la URL, y el usuario aterrizaba en el héroe sin ver ningún
 * formulario y sin entender por qué no había entrado.
 */
const abierto = ref(true)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

// Con sesión ya iniciada no tiene sentido pedir credenciales otra vez.
onMounted(() => {
  if (auth.estaAutenticado) {
    router.replace(route.query.redirect || auth.inicioSegunRol)
  }
})

function trasAcceder() {
  router.replace(route.query.redirect || auth.inicioSegunRol)
}

// Cerrar el diálogo aquí equivale a desistir: se vuelve a la portada.
function alCerrar(valor) {
  abierto.value = valor
  if (!valor) router.push('/')
}
</script>

<template>
  <div class="acceso">
    <DialogoAcceso
      :model-value="abierto"
      pestana-inicial="entrar"
      @update:model-value="alCerrar"
      @acceso="trasAcceder"
    />
  </div>
</template>

<style scoped>
.acceso {
  min-height: 100vh;
  background: var(--ink);
}
</style>
