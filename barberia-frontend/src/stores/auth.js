import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  GATEWAY_URL,
  NOMBRE_KEY,
  TOKEN_KEY,
  login as loginRequest,
  registro as registroRequest,
} from '../services/api.js'

/**
 * Traduce un fallo de login a un mensaje que diga DÓNDE se rompió.
 *
 * Distinguir estos casos importa: "no se pudo conectar" para todo lo que no sea
 * 401 hace imposible saber si el problema son las credenciales, el gateway
 * caído o un microservicio que no arranca.
 */
function describirError(e) {
  if (!e.response) {
    return `Sin respuesta de ${GATEWAY_URL}. Revisa que los contenedores estén arriba.`
  }
  const status = e.response.status
  if (status === 401) return 'Usuario o contraseña incorrectos'
  if (status === 404) return `El gateway no tiene ruta para /auth/login (404)`
  if (status === 503 || status === 502 || status === 504) {
    return `El gateway responde pero no alcanza a servicio-auth (${status}). ¿Está arrancado?`
  }
  if (status >= 500) return `Error en el servidor de autenticación (${status})`
  return `Respuesta inesperada del servidor (${status})`
}

/**
 * Decodifica el payload de un JWT sin verificar la firma.
 *
 * La firma la valida el gateway; aquí solo se lee el contenido para saber quién
 * es el usuario y cuándo expira el token, y así no pedir al backend cosas que
 * ya sabemos que van a devolver 401.
 */
function decodificarToken(token) {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || null)
  const cargando = ref(false)
  const error = ref('')

  const claims = computed(() => (token.value ? decodificarToken(token.value) : null))
  const usuario = computed(() => claims.value?.sub || null)
  const rol = computed(() => claims.value?.rol || null)

  /**
   * Id del barbero dueño de la sesión; null en cuentas ADMIN.
   *
   * Se lee del token y no de una variable aparte porque el backend lo valida
   * desde ese mismo claim firmado. Guardarlo suelto invitaría a manipularlo en
   * localStorage y a que la interfaz mostrara una agenda que la API va a negar.
   */
  const barberoId = computed(() => claims.value?.barberoId ?? null)

  /** true si el token existe y su `exp` ya pasó. */
  const tokenExpirado = computed(() => {
    const exp = claims.value?.exp
    if (!exp) return false
    return Date.now() >= exp * 1000
  })

  // Se exige `claims`: un token ilegible (localStorage manipulado o corrupto) no
  // debe pasar el guardia de rutas, aunque la cadena exista.
  const estaAutenticado = computed(() => !!token.value && !!claims.value && !tokenExpirado.value)
  const esAdmin = computed(() => estaAutenticado.value && rol.value === 'ADMIN')
  const esBarbero = computed(() => estaAutenticado.value && rol.value === 'BARBERO')
  const esCliente = computed(() => estaAutenticado.value && rol.value === 'CLIENTE')

  /** Nombre para mostrar. Llega en la respuesta del login, no en el token. */
  const nombre = ref(localStorage.getItem(NOMBRE_KEY) || '')

  /**
   * A dónde mandar al usuario tras iniciar sesión, según su rol.
   *
   * El cliente va a su área y NO al panel: aunque el guardia de rutas lo cortaría
   * igual, mandarlo a una pantalla que va a rebotar es una bienvenida pésima.
   */
  const inicioSegunRol = computed(() => {
    if (esBarbero.value) return '/barbero'
    if (esCliente.value) return '/mi-cuenta'
    return '/admin'
  })

  function guardarToken(nuevoToken) {
    token.value = nuevoToken
    localStorage.setItem(TOKEN_KEY, nuevoToken)
  }

  /**
   * Envía las credenciales a /auth/login a través del gateway.
   * @returns {Promise<boolean>} true si el login fue correcto.
   */
  async function login(username, password) {
    cargando.value = true
    error.value = ''
    try {
      const { data } = await loginRequest({ username, password })
      if (!data?.token) {
        error.value = 'La respuesta del servidor no incluyó un token'
        return false
      }
      guardarToken(data.token)
      nombre.value = data.nombre || data.username || ''
      localStorage.setItem(NOMBRE_KEY, nombre.value)
      return true
    } catch (e) {
      error.value = describirError(e)
      return false
    } finally {
      cargando.value = false
    }
  }

  /**
   * Registra una cuenta de cliente y deja la sesión abierta.
   *
   * El backend responde con el mismo cuerpo que el login (token incluido), así
   * que se reaprovecha el guardado: obligar a iniciar sesión justo después de
   * rellenar el formulario de registro es un paso de más sin ninguna ganancia.
   *
   * @returns {Promise<boolean>} true si la cuenta se creó.
   */
  async function registrar(datos) {
    cargando.value = true
    error.value = ''
    try {
      const { data } = await registroRequest(datos)
      if (!data?.token) {
        error.value = 'La respuesta del servidor no incluyó un token'
        return false
      }
      guardarToken(data.token)
      nombre.value = data.nombre || data.username || ''
      localStorage.setItem(NOMBRE_KEY, nombre.value)
      return true
    } catch (e) {
      // 409 es el caso corriente (nombre cogido) y merece su propio mensaje: el
      // genérico "respuesta inesperada" no le dice a nadie qué corregir.
      if (e.response?.status === 409) {
        error.value = e.response.data?.mensaje || 'Ese usuario ya está registrado'
      } else if (e.response?.status === 400) {
        error.value = e.response.data?.mensaje || 'Revisa los datos del formulario'
      } else {
        error.value = describirError(e)
      }
      return false
    } finally {
      cargando.value = false
    }
  }

  /** Cierra la sesión localmente. El JWT no tiene estado en el servidor. */
  function logout() {
    token.value = null
    nombre.value = ''
    error.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NOMBRE_KEY)
  }

  /**
   * Verifica el token guardado y descarta el que ya expiró.
   * @returns {boolean} true si queda una sesión válida.
   */
  function verificarToken() {
    // Cubre tanto el token vencido como el ilegible: si hay algo guardado que no
    // sirve, se descarta para que el interceptor deje de mandarlo en cada petición.
    if (token.value && !estaAutenticado.value) {
      logout()
      return false
    }
    return estaAutenticado.value
  }

  return {
    token,
    cargando,
    error,
    usuario,
    rol,
    nombre,
    barberoId,
    estaAutenticado,
    esAdmin,
    esBarbero,
    esCliente,
    inicioSegunRol,
    tokenExpirado,
    login,
    registrar,
    logout,
    verificarToken,
  }
})
