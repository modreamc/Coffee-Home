import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/game', name: 'GamePosts', component: () => import('../views/GamePosts.vue') },
  { path: '/anime', name: 'AnimePosts', component: () => import('../views/AnimePosts.vue') },
  { path: '/topics', name: 'Topics', component: () => import('../views/Topics.vue') },
  { path: '/search', name: 'Search', component: () => import('../views/Search.vue') },
  { path: '/user/:id', name: 'Profile', component: () => import('../views/Profile.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/post/:id', name: 'PostDetail', component: () => import('../views/PostDetail.vue') },
  { path: '/create', name: 'CreatePost', component: () => import('../views/CreatePost.vue'), meta: { requiresAuth: true } },
  { path: '/edit/:id', name: 'EditPost', component: () => import('../views/CreatePost.vue'), meta: { requiresAuth: true } },
  { path: '/collects', name: 'MyCollects', component: () => import('../views/MyCollects.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'Admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // 有 token 但没用户信息（比如换了标签页）时先补拉一次，避免误判角色
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') {
    next('/')
  } else {
    next()
  }
})

export default router
