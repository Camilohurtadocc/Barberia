
import { defineRouter } from '#q-app'
import {
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory
} from 'vue-router'

import routes from './routes.js'
import { useAuthStore } from '@/stores/auth'

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */

export default defineRouter((/* { store, ssrContext } */ { store }) => {
  const createHistory = import.meta.env.QUASAR_SERVER
    ? createMemoryHistory
    : import.meta.env.QUASAR_VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,

    // Leave this as is and make changes in quasar.conf.js instead!
    // quasar.conf.js -> build -> vueRouterMode
    // quasar.conf.js -> build -> publicPath
    history: createHistory(import.meta.env.QUASAR_VUE_ROUTER_BASE)
  })

  // -------------------------------------------------------------------------
  // Guardia global: protege las rutas marcadas con meta.requiereAuth.
  //
  // Sin token válido (o con token expirado) se redirige a la landing pública,
  // que es donde vive el modal de login. `verificarToken()` además limpia de
  // localStorage un token ya vencido para que el header no siga mostrando al
  // admin como conectado.
  // -------------------------------------------------------------------------
  Router.beforeEach((to) => {
    const auth = useAuthStore(store)
    const requiereAuth = to.matched.some((record) => record.meta.requiereAuth)

    if (!requiereAuth) return true

    if (!auth.verificarToken()) {
      // Se manda a /acceso y no a la portada: allí el formulario está a la vista.
      // Devolver a la landing dejaba al usuario en el héroe, sin nada que indicara
      // que se le había cortado el paso ni dónde identificarse.
      return { path: '/acceso', query: { redirect: to.fullPath } }
    }

    const rolesPermitidos = to.matched.find((record) => record.meta.roles)?.meta.roles
    if (rolesPermitidos && !rolesPermitidos.includes(auth.rol)) {
      // No se echa a la landing: se manda al panel que SÍ le corresponde. Un
      // barbero que abre /admin por error acaba en su agenda, no en la calle.
      // Si ya estuviera en su panel se cortaría el bucle devolviendo false.
      return to.path === auth.inicioSegunRol ? false : { path: auth.inicioSegunRol }
    }

    return true
  })

  return Router
})
