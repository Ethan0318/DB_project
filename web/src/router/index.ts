import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import DocsView from '@/views/DocsView.vue'
import DocEditorView from '@/views/DocEditorView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AdminView from '@/views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/docs' },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/docs', component: DocsView },
    { path: '/docs/:id', component: DocEditorView, props: true },
    { path: '/notifications', component: NotificationsView },
    { path: '/profile', component: ProfileView },
    { path: '/admin', component: AdminView }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.isReady) {
    auth.restore()
  }
  if (['/login', '/register'].includes(to.path)) {
    return true
  }
  if (!auth.token) {
    return '/login'
  }
  if (to.path.startsWith('/admin') && !auth.isAdmin) {
    return '/docs'
  }
  return true
})

export default router
