/**
 * Verificación del store de autenticación y de la capa de API contra el gateway
 * real levantado en http://localhost:8080. Ejecutar con:  node verificar-auth.mjs
 *
 * No es parte del build; es un script de comprobación manual.
 */
import { createPinia, setActivePinia } from 'pinia'

// El store usa localStorage; en Node lo simulamos.
const almacen = new Map()
globalThis.localStorage = {
  getItem: (k) => (almacen.has(k) ? almacen.get(k) : null),
  setItem: (k, v) => almacen.set(k, String(v)),
  removeItem: (k) => almacen.delete(k),
}

const { useAuthStore } = await import('./src/stores/auth.js')
const { getServicios, createServicio, TOKEN_KEY } = await import('./src/services/api.js')

setActivePinia(createPinia())

let fallos = 0
function comprobar(descripcion, condicion, detalle = '') {
  const icono = condicion ? 'OK  ' : 'FALLA'
  if (!condicion) fallos++
  console.log(`${icono} ${descripcion}${detalle ? ` -> ${detalle}` : ''}`)
}

const auth = useAuthStore()

console.log('--- estado inicial (sin sesión) ---')
comprobar('no autenticado', auth.estaAutenticado === false)
comprobar('sin usuario', auth.usuario === null)
comprobar('no es admin', auth.esAdmin === false)

console.log('\n--- login contra el gateway ---')
comprobar('login con credenciales malas falla', (await auth.login('admin', 'incorrecta')) === false)
comprobar('mensaje de error correcto', auth.error === 'Usuario o contraseña incorrectos', auth.error)

comprobar('login admin/admin123 funciona', (await auth.login('admin', 'admin123')) === true)
comprobar('token guardado en localStorage', !!localStorage.getItem(TOKEN_KEY))
comprobar('usuario decodificado del JWT', auth.usuario === 'admin', auth.usuario)
comprobar('rol decodificado del JWT', auth.rol === 'ADMIN', auth.rol)
comprobar('esAdmin', auth.esAdmin === true)
comprobar('token no expirado', auth.tokenExpirado === false)

console.log('\n--- el interceptor adjunta el token ---')
const creado = await createServicio({
  nombre: 'Prueba interceptor',
  descripcion: 'creado desde verificar-auth.mjs',
  precio: 1000,
  duracionMinutos: 10,
})
comprobar('POST protegido responde 200 con el token del store', creado.status === 200)
const listado = await getServicios()
comprobar('el servicio aparece en el listado', listado.data.some((s) => s.id === creado.data.id))

console.log('\n--- logout ---')
auth.logout()
comprobar('logout limpia el estado', auth.estaAutenticado === false)
comprobar('logout limpia localStorage', localStorage.getItem(TOKEN_KEY) === null)

console.log('\n--- token expirado ---')
const expirado = [
  Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url'),
  Buffer.from(
    JSON.stringify({ sub: 'admin', rol: 'ADMIN', exp: Math.floor(Date.now() / 1000) - 60 }),
  ).toString('base64url'),
  'firma-irrelevante',
].join('.')
localStorage.setItem(TOKEN_KEY, expirado)
auth.token = expirado
comprobar('token vencido detectado', auth.tokenExpirado === true)
comprobar('no cuenta como autenticado', auth.estaAutenticado === false)
comprobar('verificarToken() devuelve false', auth.verificarToken() === false)
comprobar('verificarToken() purga el token vencido', localStorage.getItem(TOKEN_KEY) === null)

console.log('\n--- token corrupto ---')
localStorage.setItem(TOKEN_KEY, 'esto-no-es-un-jwt')
auth.token = 'esto-no-es-un-jwt'
comprobar('no revienta al decodificar basura', auth.usuario === null)
comprobar('no autentica con basura', auth.estaAutenticado === false)
comprobar('verificarToken() rechaza la basura', auth.verificarToken() === false)
comprobar('verificarToken() purga la basura', localStorage.getItem(TOKEN_KEY) === null)

// El POST protegido de prueba deja rastro en el mock; lo limpiamos si sigue ahí.
console.log(`\n${fallos === 0 ? 'TODAS LAS COMPROBACIONES PASARON' : `${fallos} COMPROBACIONES FALLARON`}`)
process.exit(fallos === 0 ? 0 : 1)
