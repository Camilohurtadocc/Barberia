import { defineStore } from '#q-app'
import { createPinia } from 'pinia'

/*
 * Quasar detecta pinia automáticamente y usa este archivo como
 * sourceFiles.store, así que la instancia queda disponible en los
 * boot files y en defineRouter a través del parámetro `store`.
 */
export default defineStore(() => {
  return createPinia()
})
