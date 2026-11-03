import { createRouter, createWebHistory } from 'vue-router'
import TagLibraryView from '@/views/TagLibraryView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/tags' },
    { path: '/tags', name: 'tags', component: TagLibraryView }
  ]
})

export default router
