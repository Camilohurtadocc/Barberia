<script setup>
import CintaAnimada from '@/components/CintaAnimada.vue'

defineProps({
  config: { type: Object, default: () => ({}) },
})

const anio = new Date().getFullYear()
</script>

<template>
  <!-- La cinta vuelve a aparecer aquí: abre y cierra la página con el mismo gesto. -->
  <CintaAnimada :mensajes="config.tickerMensajes || []" />

  <footer class="pie">
    <div class="pie__marca">
      <span class="pie__monograma">TB</span>
      <span class="pie__nombre">The Barbershop</span>
    </div>

    <span class="pie__legal">© {{ anio }} · Bogotá, Colombia</span>

    <!--
      El acceso al panel se deja aquí abajo, discreto y sin destacar: quien
      gestiona la barbería sabe dónde está, y para el visitante es ruido.
    -->
    <RouterLink to="/acceso" class="pie__admin">panel</RouterLink>
  </footer>
</template>

<style scoped>
.pie {
  background: var(--ink);
  padding: 24px clamp(24px, 5vw, 80px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  border-top: 1px solid var(--border);
}

.pie__marca {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pie__monograma {
  width: 24px;
  height: 24px;
  border: 1px solid var(--gold-40);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fuente-display);
  font-size: 10px;
  font-weight: 700;
  font-style: italic;
  color: var(--gold);
}

.pie__nombre {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--muted);
}

.pie__legal {
  font-family: var(--fuente-mono);
  font-size: 10px;
  color: #3a3830;
}

.pie__admin {
  font-family: var(--fuente-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #2a2820;
  text-decoration: none;
  transition: color 0.2s;
}
.pie__admin:hover {
  color: var(--muted);
}
</style>
