<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * Acceso y alta de cliente en una sola ventana, con dos pestañas.
 *
 * Van juntas a propósito: quien llega a reservar no sabe todavía si tiene cuenta,
 * y separarlas en dos pantallas obliga a volver atrás en cuanto se equivoca. Aquí
 * cambiar de una a otra no pierde lo ya escrito.
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** Pestaña inicial: 'entrar' o 'registro'. */
  pestanaInicial: { type: String, default: 'entrar' },
})

const emit = defineEmits(['update:modelValue', 'acceso'])

const auth = useAuthStore()
const router = useRouter()

const pestana = ref(props.pestanaInicial)
const verClave = ref(false)

const acceso = ref({ username: '', password: '' })
const alta = ref({ username: '', password: '', repetir: '', nombre: '', email: '', telefono: '' })

const errorLocal = ref('')

watch(
  () => props.modelValue,
  (abierto) => {
    if (abierto) {
      pestana.value = props.pestanaInicial
      errorLocal.value = ''
      auth.error = ''
    }
  },
)

// Al cambiar de pestaña se limpian los errores: un mensaje de "usuario o
// contraseña incorrectos" colgando sobre el formulario de registro no significa
// nada y hace pensar que el alta ya ha fallado.
watch(pestana, () => {
  errorLocal.value = ''
  auth.error = ''
})

const mensaje = computed(() => errorLocal.value || auth.error)

function cerrar() {
  emit('update:modelValue', false)
}

async function entrar() {
  errorLocal.value = ''
  if (!acceso.value.username || !acceso.value.password) {
    errorLocal.value = 'Escribe tu usuario y tu contraseña'
    return
  }
  const ok = await auth.login(acceso.value.username, acceso.value.password)
  if (!ok) return
  emit('acceso')
  cerrar()
  // Al cliente se le deja donde estaba: probablemente estaba reservando y
  // mandarlo a otra pantalla le haría perder el formulario a medio llenar.
  if (!auth.esCliente) {
    router.push(auth.inicioSegunRol)
  }
}

async function registrar() {
  errorLocal.value = ''
  if (!alta.value.username.trim() || !alta.value.password) {
    errorLocal.value = 'El usuario y la contraseña son obligatorios'
    return
  }
  if (alta.value.password.length < 6) {
    errorLocal.value = 'La contraseña debe tener al menos 6 caracteres'
    return
  }
  // Se comprueba aquí y no solo en el servidor: es un error de tecleo y avisar
  // sin dar el viaje de ida y vuelta es más rápido para quien lo comete.
  if (alta.value.password !== alta.value.repetir) {
    errorLocal.value = 'Las dos contraseñas no coinciden'
    return
  }
  const ok = await auth.registrar({
    username: alta.value.username.trim(),
    password: alta.value.password,
    nombre: alta.value.nombre.trim() || alta.value.username.trim(),
    email: alta.value.email.trim(),
    telefono: alta.value.telefono.trim(),
  })
  if (!ok) return
  emit('acceso')
  cerrar()
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="velo" @click.self="cerrar">
      <div class="dialogo" role="dialog" aria-modal="true">
        <button type="button" class="dialogo__cerrar" aria-label="Cerrar" @click="cerrar">✕</button>

        <div class="dialogo__marca">
          <span class="dialogo__monograma">TB</span>
        </div>

        <div class="dialogo__pestanas">
          <button
            type="button"
            class="dialogo__pestana"
            :class="{ 'dialogo__pestana--activa': pestana === 'entrar' }"
            @click="pestana = 'entrar'"
          >
            Entrar
          </button>
          <button
            type="button"
            class="dialogo__pestana"
            :class="{ 'dialogo__pestana--activa': pestana === 'registro' }"
            @click="pestana = 'registro'"
          >
            Crear cuenta
          </button>
        </div>

        <!-- Entrar -->
        <form v-if="pestana === 'entrar'" novalidate @submit.prevent="entrar">
          <label class="bs-campo">
            <span class="bs-campo__label">Usuario</span>
            <input
              v-model="acceso.username"
              class="bs-input"
              autocomplete="username"
              placeholder="tu usuario"
            />
          </label>
          <label class="bs-campo">
            <span class="bs-campo__label">Contraseña</span>
            <div class="dialogo__clave">
              <input
                v-model="acceso.password"
                :type="verClave ? 'text' : 'password'"
                class="bs-input"
                autocomplete="current-password"
                placeholder="••••••••"
              />
              <button type="button" class="dialogo__ojo" @click="verClave = !verClave">
                {{ verClave ? 'ocultar' : 'ver' }}
              </button>
            </div>
          </label>

          <p v-if="mensaje" class="dialogo__error">{{ mensaje }}</p>

          <button type="submit" class="bs-btn bs-btn--oro bs-btn--bloque" :disabled="auth.cargando">
            {{ auth.cargando ? 'Entrando…' : 'Entrar' }}
          </button>
        </form>

        <!-- Registro -->
        <form v-else novalidate @submit.prevent="registrar">
          <label class="bs-campo">
            <span class="bs-campo__label">Usuario</span>
            <input
              v-model="alta.username"
              class="bs-input"
              autocomplete="username"
              placeholder="juanperez"
            />
          </label>

          <label class="bs-campo">
            <span class="bs-campo__label">Nombre completo</span>
            <input v-model="alta.nombre" class="bs-input" placeholder="Juan Pérez" />
          </label>

          <div class="dialogo__par">
            <label class="bs-campo">
              <span class="bs-campo__label">WhatsApp</span>
              <input v-model="alta.telefono" class="bs-input" placeholder="+57 300 000 0000" />
            </label>
            <label class="bs-campo">
              <span class="bs-campo__label">Correo</span>
              <input
                v-model="alta.email"
                type="email"
                class="bs-input"
                placeholder="juan@correo.com"
              />
            </label>
          </div>

          <div class="dialogo__par">
            <label class="bs-campo">
              <span class="bs-campo__label">Contraseña</span>
              <input
                v-model="alta.password"
                type="password"
                class="bs-input"
                autocomplete="new-password"
                placeholder="mínimo 6"
              />
            </label>
            <label class="bs-campo">
              <span class="bs-campo__label">Repetir</span>
              <input
                v-model="alta.repetir"
                type="password"
                class="bs-input"
                autocomplete="new-password"
                placeholder="otra vez"
              />
            </label>
          </div>

          <p v-if="mensaje" class="dialogo__error">{{ mensaje }}</p>

          <button type="submit" class="bs-btn bs-btn--oro bs-btn--bloque" :disabled="auth.cargando">
            {{ auth.cargando ? 'Creando…' : 'Crear cuenta' }}
          </button>

          <p class="dialogo__nota">
            Con una cuenta puedes consultar tus citas, ver tu historial y cancelar sin llamar.
          </p>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.velo {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(7, 8, 13, 0.86);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
}

.dialogo {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: var(--card);
  border: 1px solid var(--border);
  padding: 36px;
}

.dialogo__cerrar {
  position: absolute;
  top: 14px;
  right: 14px;
  background: none;
  border: none;
  color: var(--muted);
  font-size: 14px;
  transition: color 0.2s;
}
.dialogo__cerrar:hover {
  color: var(--gold);
}

.dialogo__marca {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.dialogo__monograma {
  width: 40px;
  height: 40px;
  border: 1px solid var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fuente-display);
  font-size: 16px;
  font-weight: 700;
  font-style: italic;
  color: var(--gold);
}

.dialogo__pestanas {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  margin-bottom: 28px;
  border-bottom: 1px solid var(--border);
}

.dialogo__pestana {
  padding: 12px 8px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-family: var(--fuente-ui);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
  transition: color 0.2s, border-color 0.2s;
}
.dialogo__pestana--activa {
  color: var(--gold);
  border-bottom-color: var(--gold);
}

.dialogo__par {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.dialogo__clave {
  position: relative;
}
.dialogo__ojo {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-family: var(--fuente-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--muted);
}
.dialogo__ojo:hover {
  color: var(--gold);
}

.dialogo__error {
  margin: 4px 0 14px;
  font-family: var(--fuente-mono);
  font-size: 11px;
  line-height: 1.5;
  color: var(--danger);
}

.dialogo__nota {
  margin: 18px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
  text-align: center;
}

@media (max-width: 480px) {
  .dialogo {
    padding: 28px 20px;
  }
  .dialogo__par {
    grid-template-columns: 1fr;
  }
}
</style>
