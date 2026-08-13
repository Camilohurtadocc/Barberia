<script setup>
import { computed, onMounted, ref } from 'vue'
import SelectorImagen from '@/components/SelectorImagen.vue'
import { cambiarPassword, getBarbero, updateBarbero, urlImagen } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'BarberoMiPerfilPage' })

/**
 * Ficha propia del barbero: lo que la landing enseña de él, sus horarios y su
 * galería.
 *
 * Los horarios se editan como fichas sueltas y no como una lista separada por
 * comas. Con el campo de texto era muy fácil dejar «10:00,, 11:30» o un formato
 * que la landing no sabía pintar, y no había forma de darse cuenta hasta ver el
 * sitio publicado.
 */

const auth = useAuthStore()

const ficha = ref(null)
const cargando = ref(false)
const guardando = ref(false)
const aviso = ref('')
const error = ref('')

const nuevoSlot = ref('')

const passActual = ref('')
const passNueva = ref('')
const cambiandoPass = ref(false)
const avisoPass = ref('')

async function cargar() {
  // El id sale del token (claim firmado), no de la URL ni de un desplegable:
  // así la pantalla solo puede pedir la ficha propia.
  if (!auth.barberoId) {
    error.value = 'Tu cuenta no está vinculada a ninguna ficha de barbero.'
    return
  }
  cargando.value = true
  error.value = ''
  try {
    const { data } = await getBarbero(auth.barberoId)
    // Se normalizan a array: el v-for y los push/splice fallarían con null.
    ficha.value = { ...data, galeria: data.galeria || [], slots: data.slots || [] }
  } catch (e) {
    if (e.response?.status !== 401) error.value = 'No se pudo cargar tu ficha.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

// Se ordenan para que la landing los muestre cronológicamente aunque se añadan
// en cualquier orden. Comparar como texto basta con el formato HH:MM.
const slotsOrdenados = computed(() => [...(ficha.value?.slots || [])].sort())

function anadirSlot() {
  const valor = nuevoSlot.value.trim()
  if (!valor) return
  if (ficha.value.slots.includes(valor)) {
    avisoPass.value = ''
    aviso.value = 'Ese horario ya está en la lista'
    return
  }
  ficha.value.slots.push(valor)
  nuevoSlot.value = ''
  aviso.value = ''
}

function quitarSlot(valor) {
  ficha.value.slots = ficha.value.slots.filter((s) => s !== valor)
}

async function guardar() {
  guardando.value = true
  aviso.value = ''
  error.value = ''
  try {
    const { data } = await updateBarbero(auth.barberoId, {
      ...ficha.value,
      slots: slotsOrdenados.value,
      // Se descartan los huecos que deja «Añadir foto» sin llegar a subir nada.
      galeria: (ficha.value.galeria || []).filter(Boolean),
    })
    ficha.value = { ...data, galeria: data.galeria || [], slots: data.slots || [] }
    aviso.value = 'Perfil actualizado'
  } catch (e) {
    if (e.response?.status !== 401) {
      error.value = e.response?.data?.mensaje || 'No se pudo guardar el perfil.'
    }
  } finally {
    guardando.value = false
  }
}

async function actualizarPassword() {
  if (!passNueva.value) {
    avisoPass.value = 'Escribe la contraseña nueva'
    return
  }
  cambiandoPass.value = true
  avisoPass.value = ''
  try {
    await cambiarPassword(passActual.value, passNueva.value)
    passActual.value = ''
    passNueva.value = ''
    avisoPass.value = 'Contraseña actualizada'
  } catch (e) {
    // El backend devuelve 400 (no 401) cuando la actual no coincide, justamente
    // para que el interceptor global no cierre la sesión por un error de tecleo.
    avisoPass.value = e.response?.data?.mensaje || 'No se pudo cambiar la contraseña'
  } finally {
    cambiandoPass.value = false
  }
}
</script>

<template>
  <div>
    <div class="bs-rotulo">
      <span class="bs-rotulo__n">02</span>
      <div class="bs-rotulo__linea" />
      <span class="bs-rotulo__texto">Perfil público</span>
    </div>

    <h1 class="bs-h2 perfil__titulo">
      Mi<br /><em class="bs-em">ficha.</em>
    </h1>

    <p v-if="error" class="perfil__error">{{ error }}</p>
    <p v-if="cargando" class="perfil__cargando">Cargando…</p>

    <template v-if="ficha">
      <div class="perfil__columnas">
        <!-- Foto y datos -->
        <section class="bs-tarjeta perfil__bloque">
          <h2 class="perfil__subtitulo">Datos</h2>

          <div class="perfil__foto">
            <img v-if="ficha.fotoUrl" :src="urlImagen(ficha.fotoUrl)" alt="" />
            <div v-else class="perfil__foto-vacia">Sin foto</div>
          </div>
          <SelectorImagen v-model="ficha.fotoUrl" label="Foto de perfil" />

          <label class="bs-campo">
            <span class="bs-campo__label">Nombre</span>
            <input v-model="ficha.nombre" class="bs-input" />
          </label>

          <div class="perfil__par">
            <label class="bs-campo">
              <span class="bs-campo__label">Cargo</span>
              <input v-model="ficha.cargo" class="bs-input" placeholder="Master Barber" />
            </label>
            <label class="bs-campo">
              <span class="bs-campo__label">Años</span>
              <input v-model.number="ficha.aniosExperiencia" type="number" min="0" class="bs-input" />
            </label>
          </div>

          <label class="bs-campo">
            <span class="bs-campo__label">Especialidad</span>
            <input v-model="ficha.especialidad" class="bs-input" placeholder="Fades & Degradados" />
          </label>

          <div class="perfil__par">
            <label class="bs-campo">
              <span class="bs-campo__label">Instagram</span>
              <input v-model="ficha.instagram" class="bs-input" placeholder="@usuario" />
            </label>
            <label class="bs-campo">
              <span class="bs-campo__label">Cortes</span>
              <input v-model="ficha.cortes" class="bs-input" placeholder="8.4K" />
            </label>
          </div>
        </section>

        <!-- Horarios y galería -->
        <div class="perfil__lateral">
          <section class="bs-tarjeta perfil__bloque">
            <h2 class="perfil__subtitulo">Horarios que ofrezco</h2>
            <p class="perfil__ayuda">
              Son los turnos que verá un cliente al elegirte en la web.
            </p>

            <div v-if="slotsOrdenados.length" class="perfil__slots">
              <span v-for="s in slotsOrdenados" :key="s" class="perfil__slot">
                {{ s }}
                <button
                  type="button"
                  class="perfil__slot-quitar"
                  :aria-label="`Quitar ${s}`"
                  @click="quitarSlot(s)"
                >
                  ✕
                </button>
              </span>
            </div>
            <p v-else class="perfil__ayuda">Todavía no has publicado ningún horario.</p>

            <div class="perfil__anadir">
              <!-- type=time evita de raíz los formatos que la landing no sabe pintar. -->
              <input v-model="nuevoSlot" type="time" class="bs-input" @keydown.enter.prevent="anadirSlot" />
              <button type="button" class="bs-btn bs-btn--fantasma bs-btn--pequeno" @click="anadirSlot">
                Añadir
              </button>
            </div>
          </section>

          <section class="bs-tarjeta perfil__bloque">
            <h2 class="perfil__subtitulo">Mi galería</h2>
            <div class="perfil__galeria">
              <div v-for="(g, i) in ficha.galeria" :key="i" class="perfil__galeria-item">
                <img v-if="g" :src="urlImagen(g)" alt="" />
                <SelectorImagen v-model="ficha.galeria[i]" label="Cambiar" />
                <button
                  type="button"
                  class="bs-btn bs-btn--peligro bs-btn--pequeno"
                  @click="ficha.galeria.splice(i, 1)"
                >
                  Quitar
                </button>
              </div>
            </div>
            <button
              type="button"
              class="bs-btn bs-btn--fantasma bs-btn--pequeno"
              @click="ficha.galeria.push('')"
            >
              Añadir foto
            </button>
          </section>
        </div>
      </div>

      <div class="perfil__guardar">
        <span v-if="aviso" class="perfil__aviso">{{ aviso }}</span>
        <button type="button" class="bs-btn bs-btn--oro" :disabled="guardando" @click="guardar">
          {{ guardando ? 'Guardando…' : 'Guardar cambios' }}
        </button>
      </div>

      <!-- Contraseña -->
      <section class="bs-tarjeta perfil__bloque perfil__clave">
        <h2 class="perfil__subtitulo">Cambiar contraseña</h2>
        <div class="perfil__par">
          <label class="bs-campo">
            <span class="bs-campo__label">Actual</span>
            <input v-model="passActual" type="password" class="bs-input" autocomplete="current-password" />
          </label>
          <label class="bs-campo">
            <span class="bs-campo__label">Nueva</span>
            <input v-model="passNueva" type="password" class="bs-input" autocomplete="new-password" />
          </label>
        </div>
        <div class="perfil__guardar">
          <span v-if="avisoPass" class="perfil__aviso">{{ avisoPass }}</span>
          <button
            type="button"
            class="bs-btn bs-btn--fantasma bs-btn--pequeno"
            :disabled="cambiandoPass"
            @click="actualizarPassword"
          >
            {{ cambiandoPass ? 'Actualizando…' : 'Actualizar contraseña' }}
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.perfil__titulo {
  margin: 0 0 36px;
}

.perfil__columnas {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px;
  align-items: start;
}

.perfil__lateral {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.perfil__bloque {
  padding: 24px;
}

.perfil__subtitulo {
  font-family: var(--fuente-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--muted);
  margin: 0 0 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}

.perfil__ayuda {
  font-size: 12px;
  line-height: 1.7;
  color: var(--muted);
  margin: 0 0 14px;
}

.perfil__foto {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  margin-bottom: 12px;
}
.perfil__foto img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.perfil__foto-vacia {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: var(--muted);
}

.perfil__par {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.perfil__slots {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.perfil__slot {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px 8px 14px;
  border: 1px solid var(--gold-40);
  font-family: var(--fuente-mono);
  font-size: 13px;
  color: var(--gold);
}

.perfil__slot-quitar {
  background: none;
  border: none;
  color: var(--muted);
  font-size: 10px;
  padding: 0;
  transition: color 0.2s;
}
.perfil__slot-quitar:hover {
  color: var(--danger);
}

.perfil__anadir {
  display: flex;
  gap: 8px;
  align-items: center;
}
.perfil__anadir .bs-input {
  flex: 1;
}

.perfil__galeria {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.perfil__galeria-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.perfil__galeria-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  border: 1px solid var(--border);
}

.perfil__guardar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin: 24px 0;
}

.perfil__aviso {
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: var(--gold);
}

.perfil__error {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--danger);
  margin-bottom: 16px;
}

.perfil__cargando {
  font-family: var(--fuente-mono);
  font-size: 12px;
  color: var(--muted);
}

.perfil__clave {
  margin-top: 24px;
}

@media (max-width: 900px) {
  .perfil__columnas {
    grid-template-columns: 1fr;
  }
}
</style>
