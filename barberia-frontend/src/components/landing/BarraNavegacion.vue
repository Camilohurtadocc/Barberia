<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * Barra fija superior. Además de navegar, es donde el visitante ve si tiene
 * sesión abierta, así que cambia de contenido según el rol.
 */
defineEmits(['abrir-acceso'])

const auth = useAuthStore()
const menuAbierto = ref(false)

/**
 * Ojo: 'reservas' NO va en esta lista.
 *
 * Tiene su propio botón dorado a la derecha, y tenerlo además como enlace de
 * texto dejaba dos accesos a la misma sección uno al lado del otro. Cuando la
 * acción principal se repite como enlace secundario, deja de leerse como la
 * acción principal.
 */
const ENLACES = [
  { texto: 'Servicios', ancla: 'servicios' },
  { texto: 'Equipo', ancla: 'equipo' },
  { texto: 'Portafolio', ancla: 'portafolio' },
  { texto: 'Síguenos', ancla: 'redes' },
]

function irA(ancla) {
  menuAbierto.value = false
  document.getElementById(ancla)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// La barra se opaca al separarse del héroe. Arriba del todo se deja translúcida
// para que la imagen del héroe llegue hasta el borde de la pantalla.
const desplazada = ref(false)
function alDesplazar() {
  desplazada.value = window.scrollY > 40
}
onMounted(() => window.addEventListener('scroll', alDesplazar, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', alDesplazar))
</script>

<template>
  <nav class="nav" :class="{ 'nav--solida': desplazada }">
    <a class="nav__marca" href="#" @click.prevent="irA('inicio')">
      <span class="nav__monograma">TB</span>
      <span class="nav__nombre">The Barbershop</span>
    </a>

    <div class="nav__enlaces">
      <button
        v-for="e in ENLACES"
        :key="e.ancla"
        type="button"
        class="nav__enlace"
        @click="irA(e.ancla)"
      >
        {{ e.texto }}
      </button>

      <button type="button" class="nav__cta" @click="irA('reservas')">
        Reservar
      </button>

      <!-- Con sesión: acceso directo al área que corresponde al rol. -->
      <template v-if="auth.estaAutenticado">
        <RouterLink
          :to="auth.inicioSegunRol"
          class="nav__cuenta"
        >
          <span class="nav__cuenta-nombre">{{ auth.nombre || auth.usuario }}</span>
          <span class="nav__cuenta-rol">{{ auth.rol }}</span>
        </RouterLink>
      </template>
      <button
        v-else
        type="button"
        class="nav__acceso"
        @click="$emit('abrir-acceso')"
      >
        Acceder
      </button>
    </div>

    <button
      type="button"
      class="nav__hamburguesa"
      aria-label="Menú"
      @click="menuAbierto = !menuAbierto"
    >
      {{ menuAbierto ? '✕' : '☰' }}
    </button>

    <!-- Menú desplegable en móvil -->
    <div v-if="menuAbierto" class="nav__movil">
      <button
        v-for="e in ENLACES"
        :key="e.ancla"
        type="button"
        class="nav__movil-enlace"
        @click="irA(e.ancla)"
      >
        {{ e.texto }}
      </button>

      <!--
        Aquí sí va explícito. En escritorio, «Reservar» es el botón dorado de la
        barra, que en móvil no se muestra; sin esta entrada el menú desplegable
        se quedaba sin ninguna forma de llegar a la sección de reservas.
      -->
      <button type="button" class="nav__movil-enlace nav__movil-enlace--cta" @click="irA('reservas')">
        Reservar
      </button>

      <RouterLink
        v-if="auth.estaAutenticado"
        :to="auth.inicioSegunRol"
        class="nav__movil-enlace nav__movil-enlace--oro"
        @click="menuAbierto = false"
      >
        {{ auth.esCliente ? 'Mis citas' : 'Mi panel' }}
      </RouterLink>
      <button
        v-else
        type="button"
        class="nav__movil-enlace nav__movil-enlace--oro"
        @click="menuAbierto = false; $emit('abrir-acceso')"
      >
        Acceder
      </button>
    </div>
  </nav>
</template>

<style scoped>
.nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(16px, 4vw, 40px);
  background: transparent;
  border-bottom: 1px solid transparent;
  transition: background 0.3s, border-color 0.3s, backdrop-filter 0.3s;
}

.nav--solida {
  background: rgba(7, 8, 13, 0.94);
  backdrop-filter: blur(16px);
  border-bottom-color: var(--border);
}

.nav__marca {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.nav__monograma {
  width: 32px;
  height: 32px;
  border: 1px solid var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-family: var(--fuente-display);
  font-size: 14px;
  font-weight: 700;
  font-style: italic;
  color: var(--gold);
}

.nav__nombre {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--text);
}

.nav__enlaces {
  display: flex;
  align-items: center;
  gap: 0;
}

.nav__enlace {
  padding: 22px 16px;
  background: none;
  border: none;
  font-family: var(--fuente-ui);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
  transition: color 0.2s;
}
.nav__enlace:hover {
  color: var(--text);
}

.nav__cta {
  margin-left: 16px;
  padding: 10px 24px;
  background: var(--gold);
  border: none;
  color: var(--ink);
  font-family: var(--fuente-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  transition: background 0.2s;
}
.nav__cta:hover {
  background: var(--gold-soft);
}

.nav__acceso {
  margin-left: 10px;
  padding: 10px 18px;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--muted);
  font-family: var(--fuente-ui);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  transition: border-color 0.2s, color 0.2s;
}
.nav__acceso:hover {
  border-color: var(--gold);
  color: var(--gold);
}

.nav__cuenta {
  margin-left: 12px;
  padding: 7px 16px;
  border: 1px solid var(--gold-40);
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  text-decoration: none;
  transition: border-color 0.2s, background 0.2s;
}
.nav__cuenta:hover {
  border-color: var(--gold);
  background: var(--gold-15);
}
.nav__cuenta-nombre {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}
.nav__cuenta-rol {
  font-family: var(--fuente-mono);
  font-size: 8px;
  letter-spacing: 0.16em;
  color: var(--gold);
}

.nav__hamburguesa {
  display: none;
  background: none;
  border: 1px solid var(--border);
  color: var(--text);
  width: 38px;
  height: 38px;
  font-size: 15px;
}

.nav__movil {
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  background: rgba(7, 8, 13, 0.98);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.nav__movil-enlace {
  padding: 16px 24px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--border);
  text-align: left;
  text-decoration: none;
  font-family: var(--fuente-ui);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text);
}
.nav__movil-enlace--oro {
  color: var(--gold);
}

/* Mismo peso visual que el botón dorado del escritorio: en el desplegable sigue
   siendo la acción principal y tiene que distinguirse del resto de enlaces. */
.nav__movil-enlace--cta {
  background: var(--gold);
  color: var(--ink);
  font-weight: 700;
}

/* Por debajo de 900px no caben cinco enlaces más la sesión sin apretujarse. */
@media (max-width: 900px) {
  .nav__enlaces {
    display: none;
  }
  .nav__hamburguesa {
    display: block;
  }
  .nav__nombre {
    display: none;
  }
}
</style>
