<script setup>
import { computed } from 'vue'
import RotuloSeccion from '@/components/RotuloSeccion.vue'
import { urlImagen } from '@/services/api'

/**
 * Equipo. Funciona como un selector: elegir barbero cambia el panel de abajo,
 * donde están sus turnos libres y su portafolio.
 *
 * El barbero y la hora elegidos suben al padre, que es quien mantiene el estado
 * de la reserva. Así esta sección y el formulario de Reservas hablan de la misma
 * cita en lugar de llevar dos borradores distintos.
 */
const props = defineProps({
  barberos: { type: Array, default: () => [] },
  barberoId: { type: [Number, String], default: null },
  slot: { type: String, default: null },
  /** Nombre del servicio ya elegido, para recordarlo junto a los turnos. */
  servicioNombre: { type: String, default: '' },
})

const emit = defineEmits(['update:barberoId', 'update:slot'])

const FOTO_POR_DEFECTO = 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500&h=600&fit=crop&auto=format'

const activo = computed(
  () => props.barberos.find((b) => b.id === props.barberoId) || props.barberos[0] || null,
)

function elegirBarbero(b) {
  emit('update:barberoId', b.id)
  // La hora se limpia al cambiar de barbero: los turnos son de cada uno, y
  // conservar la selección anterior mandaría al backend una hora que este
  // barbero no ofrece.
  emit('update:slot', null)
}

function foto(b) {
  return b?.fotoUrl ? urlImagen(b.fotoUrl) : FOTO_POR_DEFECTO
}
</script>

<template>
  <section id="equipo" class="bs-seccion bs-fondo-surface">
    <div class="bs-contenedor">
      <RotuloSeccion numero="02" texto="Equipo" />

      <h2 class="bs-h2 equipo__titulo">
        Los maestros<br /><em class="bs-em">del oficio.</em>
      </h2>

      <!-- Selector -->
      <div class="equipo__rejilla">
        <div
          v-for="b in barberos"
          :key="b.id"
          class="barbero"
          :class="{ 'barbero--activo': activo && activo.id === b.id }"
          role="button"
          tabindex="0"
          @click="elegirBarbero(b)"
          @keydown.enter="elegirBarbero(b)"
        >
          <img :src="foto(b)" :alt="b.nombre" class="barbero__foto" />
          <div class="barbero__velo" />
          <div v-if="activo && activo.id === b.id" class="barbero__subrayado" />
          <div class="barbero__datos">
            <div v-if="b.especialidad" class="barbero__especialidad">{{ b.especialidad }}</div>
            <div class="barbero__nombre">{{ b.nombre }}</div>
            <div class="barbero__meta">
              {{ b.cargo || 'Barbero' }}
              <template v-if="b.aniosExperiencia"> · {{ b.aniosExperiencia }} años</template>
            </div>
          </div>
        </div>
      </div>

      <!-- Panel del barbero activo -->
      <div v-if="activo" class="panel">
        <div class="panel__columnas">
          <div>
            <div class="panel__nombre">{{ activo.nombre }}</div>
            <div class="panel__rotulo">TURNOS DISPONIBLES</div>

            <div v-if="servicioNombre" class="panel__servicio">
              <span class="panel__servicio-punto" />
              <span class="panel__servicio-texto">{{ servicioNombre }}</span>
            </div>

            <div v-if="activo.slots && activo.slots.length" class="panel__slots">
              <button
                v-for="h in activo.slots"
                :key="h"
                type="button"
                class="slot"
                :class="{ 'slot--activo': slot === h }"
                @click="emit('update:slot', slot === h ? null : h)"
              >
                {{ h }}
              </button>
            </div>
            <p v-else class="panel__sin-slots">
              Este barbero aún no tiene horarios publicados. Puedes pedir tu turno indicando la
              hora en el formulario de reservas.
            </p>
          </div>

          <div>
            <div class="panel__rotulo">PORTAFOLIO DEL BARBERO</div>
            <div v-if="activo.galeria && activo.galeria.length" class="panel__galeria">
              <div v-for="(g, i) in activo.galeria" :key="i" class="panel__miniatura">
                <img :src="urlImagen(g)" alt="" />
              </div>
            </div>
            <p v-else class="panel__sin-slots">Sin trabajos publicados todavía.</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.equipo__titulo {
  margin-bottom: 48px;
}

.equipo__rejilla {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2px;
  margin-bottom: 2px;
}

.barbero {
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;
  transition: border-color 0.3s;
}

.barbero--activo {
  border-color: var(--gold-60);
}

/* Los barberos no elegidos van en gris: la foto en color señala al activo sin
   necesidad de ningún indicador añadido. */
.barbero__foto {
  width: 100%;
  height: 340px;
  object-fit: cover;
  display: block;
  filter: grayscale(70%);
  transition: filter 0.4s, transform 0.4s;
}
.barbero--activo .barbero__foto {
  filter: none;
}
.barbero:hover .barbero__foto {
  transform: scale(1.03);
}

.barbero__velo {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, var(--ink) 0%, transparent 50%);
}

.barbero__subrayado {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--gold);
}

.barbero__datos {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
}

.barbero__especialidad {
  font-family: var(--fuente-mono);
  font-size: 9px;
  color: var(--gold);
  letter-spacing: 0.18em;
  margin-bottom: 4px;
}

.barbero__nombre {
  font-family: var(--fuente-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.barbero__meta {
  font-family: var(--fuente-mono);
  font-size: 10px;
  color: var(--muted);
  margin-top: 3px;
}

.panel {
  background: var(--card);
  border: 1px solid var(--border);
  padding: 36px;
}

.panel__columnas {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
}

.panel__nombre {
  font-family: var(--fuente-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 4px;
}

.panel__rotulo {
  font-family: var(--fuente-mono);
  font-size: 10px;
  color: var(--muted);
  letter-spacing: 0.14em;
  margin-bottom: 16px;
}

.panel__servicio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 6px 12px;
  background: var(--gold-15);
  border: 1px solid var(--gold-40);
}

.panel__servicio-punto {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gold);
}

.panel__servicio-texto {
  font-family: var(--fuente-mono);
  font-size: 10px;
  color: var(--gold);
  letter-spacing: 0.12em;
}

.panel__slots {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.slot {
  padding: 12px 22px;
  background: transparent;
  color: var(--text);
  border: 1px solid var(--border);
  font-family: var(--fuente-mono);
  font-size: 14px;
  font-weight: 500;
  transition: background 0.18s, color 0.18s, border-color 0.18s;
}
.slot:hover {
  border-color: var(--gold-60);
}
.slot--activo {
  background: var(--gold);
  color: var(--ink);
  border-color: var(--gold);
}

.panel__sin-slots {
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
  margin: 0;
}

.panel__galeria {
  display: flex;
  gap: 3px;
}

.panel__miniatura {
  flex: 1;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--surface);
}

.panel__miniatura img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s;
}
.panel__miniatura:hover img {
  transform: scale(1.08);
}

@media (max-width: 900px) {
  .equipo__rejilla {
    grid-template-columns: 1fr;
  }
  .barbero__foto {
    height: 260px;
  }
  .panel {
    padding: 24px;
  }
  .panel__columnas {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}
</style>
