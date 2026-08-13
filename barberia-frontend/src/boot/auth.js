import { defineBoot } from '#q-app'
import { setUnauthorizedHandler } from '../services/api.js'
import { useAuthStore } from '../stores/auth.js'

/*
 * Conecta el interceptor de Axios con el store y el router: si el gateway
 * responde 401 (token expirado o manipulado), se cierra la sesión y se
 * devuelve al usuario a la landing pública.
 */
export default defineBoot(({ router, store }) => {
  const auth = useAuthStore(store)

  // Descarta de entrada un token viejo que haya quedado en localStorage.
  auth.verificarToken()

  setUnauthorizedHandler(() => {
    auth.logout()
    if (router.currentRoute.value.path.startsWith('/admin')) {
      router.replace({ path: '/', query: { sesion: 'expirada' } })
    }
  })
})
