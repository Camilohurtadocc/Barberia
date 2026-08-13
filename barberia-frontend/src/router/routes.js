const routes = [
  // ---------------------------------------------------------------------
  // Zona pública (cliente sin autenticar)
  // ---------------------------------------------------------------------
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'landing', component: () => import('@/pages/IndexPage.vue') },
      { path: 'acceso', name: 'acceso', component: () => import('@/pages/AccesoPage.vue') },
    ],
  },

  // ---------------------------------------------------------------------
  // Área del cliente. Va fuera de /admin y de /barbero a propósito: son tres
  // zonas con permisos distintos y el guardia corta por URL antes de montar
  // nada, así que no hay forma de colarse en la gestión escribiendo la
  // dirección a mano.
  // ---------------------------------------------------------------------
  {
    path: '/mi-cuenta',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiereAuth: true, roles: ['CLIENTE'] },
    children: [
      {
        path: '',
        name: 'cliente-cuenta',
        component: () => import('@/pages/cliente/MiCuentaPage.vue'),
      },
    ],
  },

  // ---------------------------------------------------------------------
  // Panel de administración. `meta.roles` lo consume el guardia global de
  // router/index.js. Es una lista, no un rol suelto, porque hay pantallas
  // que comparten los dos roles.
  // ---------------------------------------------------------------------
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiereAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/pages/admin/DashboardPage.vue'),
      },
      {
        path: 'servicios',
        name: 'admin-servicios',
        component: () => import('@/pages/admin/ServiciosPage.vue'),
      },
      {
        path: 'barberos',
        name: 'admin-barberos',
        component: () => import('@/pages/admin/BarberosPage.vue'),
      },
      {
        path: 'citas',
        name: 'admin-citas',
        component: () => import('@/pages/admin/CitasPage.vue'),
      },
      {
        path: 'usuarios',
        name: 'admin-usuarios',
        component: () => import('@/pages/admin/UsuariosPage.vue'),
      },
      {
        path: 'sitio',
        name: 'admin-sitio',
        component: () => import('@/pages/admin/SitioPage.vue'),
      },
    ],
  },

  // ---------------------------------------------------------------------
  // Panel del barbero. Rutas separadas de /admin a propósito: así el guardia
  // corta por URL antes de montar nada, y no hay forma de llegar a una
  // pantalla de gestión escribiendo la dirección a mano.
  // ---------------------------------------------------------------------
  {
    path: '/barbero',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiereAuth: true, roles: ['BARBERO'] },
    children: [
      {
        path: '',
        name: 'barbero-agenda',
        component: () => import('@/pages/barbero/MiAgendaPage.vue'),
      },
      {
        path: 'perfil',
        name: 'barbero-perfil',
        component: () => import('@/pages/barbero/MiPerfilPage.vue'),
      },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue'),
  },
]

export default routes
