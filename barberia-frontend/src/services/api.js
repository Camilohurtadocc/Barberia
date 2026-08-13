import axios from 'axios'

/**
 * Punto único de entrada: todo pasa por el API Gateway.
 * El gateway enruta /api/** y /auth/** hacia los microservicios y valida el JWT.
 */
export const GATEWAY_URL = import.meta.env?.VITE_GATEWAY_URL || 'http://localhost:8080'

export const TOKEN_KEY = 'barberia_token'
/** El nombre para mostrar no viaja en el token, así que se guarda aparte. */
export const NOMBRE_KEY = 'barberia_nombre'

export const api = axios.create({
  baseURL: GATEWAY_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Callback que se dispara cuando el gateway responde 401 (token ausente, inválido
// o expirado). Lo registra el boot file para cerrar sesión y volver a la landing,
// evitando que este módulo dependa del store o del router (import circular).
let onUnauthorized = null

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

// --- Interceptor de petición: adjunta el token a TODAS las llamadas -----------
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// --- Interceptor de respuesta: 401 => sesión terminada ------------------------
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && onUnauthorized) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)

// =============================================================================
// Servicios (servicio-catalogo)
// =============================================================================
export const getServicios = () => api.get('/api/servicios')
export const getServicio = (id) => api.get(`/api/servicios/${id}`)
export const createServicio = (data) => api.post('/api/servicios', data)
export const updateServicio = (id, data) => api.put(`/api/servicios/${id}`, data)
export const deleteServicio = (id) => api.delete(`/api/servicios/${id}`)

// =============================================================================
// Barberos (servicio-barberos)
// =============================================================================
export const getBarberos = () => api.get('/api/barberos')
export const getBarbero = (id) => api.get(`/api/barberos/${id}`)
export const createBarbero = (data) => api.post('/api/barberos', data)
export const updateBarbero = (id, data) => api.put(`/api/barberos/${id}`, data)
export const deleteBarbero = (id) => api.delete(`/api/barberos/${id}`)

// =============================================================================
// Citas (servicio-citas)
// =============================================================================
/**
 * Devuelve lo que corresponda a quien pregunta: el ADMIN recibe todas las citas,
 * el BARBERO las de su agenda y el CLIENTE su historial. El filtro lo hace
 * servicio-citas leyendo el token, así que desde aquí es la misma llamada.
 */
export const getCitas = () => api.get('/api/citas')
export const getCita = (id) => api.get(`/api/citas/${id}`)

/**
 * Crear cita es público, para no perder reservas de quien no quiere registrarse.
 * Si hay sesión, el interceptor manda el token igualmente y el gateway propaga la
 * identidad, de modo que la cita queda ligada a la cuenta y aparece en su historial.
 */
export const createCita = (data) => api.post('/api/citas', data)
export const updateCitaEstado = (id, estado) =>
  api.put(`/api/citas/${id}/estado`, null, { params: { estado } })
export const deleteCita = (id) => api.delete(`/api/citas/${id}`)

/**
 * Cancelación desde el área de cliente. Es el mismo endpoint de estado: el
 * cliente no borra la cita, la marca como CANCELADA para que el barbero vea el
 * hueco liberado y la barbería conserve el historial.
 */
export const cancelarCita = (id) => updateCitaEstado(id, 'CANCELADA')

// =============================================================================
// Contenido de la portada (servicio-catalogo)
// =============================================================================
export const getConfiguracion = () => api.get('/api/configuracion')
export const updateConfiguracion = (data) => api.put('/api/configuracion', data)

export const getPortafolio = () => api.get('/api/portafolio')
export const createPortafolio = (data) => api.post('/api/portafolio', data)
export const updatePortafolio = (id, data) => api.put(`/api/portafolio/${id}`, data)
export const deletePortafolio = (id) => api.delete(`/api/portafolio/${id}`)

// =============================================================================
// Imágenes
// =============================================================================

/**
 * Sube una imagen elegida en el explorador de archivos.
 *
 * El `Content-Type: undefined` es imprescindible, no un descuido. Esta instancia de
 * axios fija 'application/json' por defecto para todas las peticiones (arriba, en
 * `axios.create`), y esa cabecera pisa la que axios calcularía para un FormData. El
 * servidor recibía entonces un cuerpo multipart etiquetado como JSON y respondía
 * 415 Unsupported Media Type.
 *
 * Poniéndola a undefined se borra la cabecera heredada y axios genera la correcta:
 * `multipart/form-data; boundary=...`. El boundary es la marca que separa las partes
 * del cuerpo, la calcula axios y por eso no se puede escribir a mano.
 *
 * @param {File} archivo fichero del input
 * @returns {Promise} respuesta con { nombre, url }
 */
export const uploadArchivo = (archivo) => {
  const datos = new FormData()
  datos.append('archivo', archivo)
  return api.post('/api/archivos', datos, { headers: { 'Content-Type': undefined } })
}

/**
 * Convierte lo guardado en base a una URL que el navegador pueda pintar.
 *
 * Las imágenes conviven en dos formatos: las subidas quedan como ruta relativa
 * (/api/archivos/xxx.png) y las de ejemplo son URLs externas de Unsplash. Esto
 * decide cuál necesita el prefijo del gateway y cuál ya está completa.
 */
export function urlImagen(valor) {
  if (!valor) return ''
  if (valor.startsWith('http://') || valor.startsWith('https://') || valor.startsWith('data:')) {
    return valor
  }
  return `${GATEWAY_URL}${valor.startsWith('/') ? '' : '/'}${valor}`
}

// =============================================================================
// Autenticación (servicio-auth)
// =============================================================================
export const login = (credenciales) => api.post('/auth/login', credenciales)
export const logout = () => api.post('/auth/logout')

/**
 * Alta de cliente desde la web. Devuelve ya el token, así que tras registrarse
 * no hace falta pasar por el formulario de acceso.
 *
 * El rol no se manda: lo fija servicio-auth a CLIENTE. Si se pudiera elegir
 * desde aquí, este endpoint (que es público por necesidad) permitiría crearse
 * una cuenta de administrador.
 */
export const registro = (datos) => api.post('/auth/registro', datos)

/** Datos de contacto de la propia cuenta; el backend identifica por el token. */
export const getPerfil = () => api.get('/auth/perfil')
export const updatePerfil = (datos) => api.put('/auth/perfil', datos)

// =============================================================================
// Cuentas de acceso (solo ADMIN). El gateway devuelve 403 a cualquier otro rol.
// =============================================================================
export const getUsuarios = () => api.get('/auth/usuarios')
export const createUsuario = (data) => api.post('/auth/usuarios', data)
export const deleteUsuario = (id) => api.delete(`/auth/usuarios/${id}`)
export const deleteUsuarioDeBarbero = (barberoId) =>
  api.delete(`/auth/usuarios/barbero/${barberoId}`)

/** Cambio de contraseña propia: el backend identifica al usuario por el token. */
export const cambiarPassword = (actual, nueva) => api.put('/auth/password', { actual, nueva })
