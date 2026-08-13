<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'PanelLayout' })

/**
 * Marco de las dos zonas de gestión: administración y barbero.
 *
 * Comparten layout porque comparten forma —una cabecera fija y una fila de
 * pestañas— y lo único que cambia es qué pestañas se pintan. Lo decide el rol.
 *
 * Ojo: esto es comodidad visual, NO seguridad. Quien escriba /admin/servicios a
 * mano lo frena el guardia del router y, si lo saltara, el gateway responde 403.
 * La interfaz nunca es la barrera.
 */

const auth = useAuthStore()
const router = useRouter()

const PESTANAS_ADMIN = [
  { to: '/admin', label: 'Resumen', exact: true },
  { to: '/admin/servicios', label: 'Servicios', exact: false },
  { to: '/admin/barberos', label: 'Barberos', exact: false },
  { to: '/admin/citas', label: 'Citas', exact: false },
  { to: '/admin/usuarios', label: 'Usuarios', exact: false },
  { to: '/admin/sitio', label: 'Sitio', exact: false },
]

const PESTANAS_BARBERO = [
  { to: '/barbero', label: 'Mi agenda', exact: true },
  { to: '/barbero/perfil', label: 'Mi perfil', exact: false },
]

const pestanas = computed(() => (auth.esAdmin ? PESTANAS_ADMIN : PESTANAS_BARBERO))

function cerrarSesion() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="panel">
    <header class="panel__cabecera">
      <div class="panel__fila">
        <RouterLink to="/" class="panel__marca">
          <span class="panel__monograma">TB</span>
          <span class="panel__nombre">The Barbershop</span>
          <span class="panel__distintivo">{{ auth.rol }}</span>
        </RouterLink>

        <div class="panel__sesion">
          <span class="panel__usuario">{{ auth.nombre || auth.usuario }}</span>
          <RouterLink to="/" class="bs-btn bs-btn--fantasma bs-btn--pequeno">Ver sitio</RouterLink>
          <button
            type="button"
            class="bs-btn bs-btn--fantasma bs-btn--pequeno panel__salir"
            @click="cerrarSesion"
          >
            Salir
          </button>
        </div>
      </div>

      <nav class="panel__pestanas">
        <RouterLink
          v-for="t in pestanas"
          :key="t.to"
          :to="t.to"
          class="panel__pestana"
          :class="{ 'panel__pestana--activa': t.exact ? $route.path === t.to : $route.path.startsWith(t.to) }"
        >
          {{ t.label }}
        </RouterLink>
      </nav>
    </header>

    <main class="panel__cuerpo">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.panel {
  min-height: 100vh;
  background: var(--ink);
}

.panel__cabecera {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(7, 8, 13, 0.96);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border);
}

.panel__fila {
  height: 64px;
  padding: 0 clamp(16px, 4vw, 32px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel__marca {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.panel__monograma {
  width: 30px;
  height: 30px;
  border: 1px solid var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fuente-display);
  font-size: 13px;
  font-weight: 700;
  font-style: italic;
  color: var(--gold);
  flex-shrink: 0;
}

.panel__nombre {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text);
}

.panel__distintivo {
  padding: 3px 9px;
  background: var(--gold);
  color: var(--ink);
  font-family: var(--fuente-mono);
  font-size: 9px;
  font-weight: 500;
  letter-spacing: 0.16em;
}

.panel__sesion {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel__usuario {
  font-family: var(--fuente-mono);
  font-size: 11px;
  color: var(--muted);
}

.panel__salir:hover {
  border-color: rgba(217, 83, 79, 0.5);
  color: var(--danger);
}

.panel__pestanas {
  display: flex;
  gap: 0;
  padding: 0 clamp(16px, 4vw, 32px);
  border-top: 1px solid var(--border);
  overflow-x: auto;
}

.panel__pestana {
  padding: 14px 18px;
  text-decoration: none;
  font-family: var(--fuente-ui);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: color 0.2s, border-color 0.2s;
}

.panel__pestana:hover {
  color: var(--text);
}

.panel__pestana--activa {
  color: var(--gold);
  border-bottom-color: var(--gold);
}

.panel__cuerpo {
  padding: 40px clamp(16px, 4vw, 32px) 80px;
  max-width: 1280px;
  margin: 0 auto;
}

@media (max-width: 640px) {
  .panel__nombre {
    display: none;
  }
  .panel__usuario {
    display: none;
  }
}
</style>
