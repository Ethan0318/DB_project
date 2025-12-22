import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import DocsView from '@/views/DocsView.vue'
import DocEditorView from '@/views/DocEditorView.vue'
import NotificationsView from '@/views/NotificationsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/docs' },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/docs', component: DocsView },
    { path: '/docs/:id', component: DocEditorView, props: true },
    { path: '/notifications', component: NotificationsView }
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
  return true
})

export default router
