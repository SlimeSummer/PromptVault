import { createRouter, createWebHistory } from 'vue-router'
import TagLibraryView from '@/views/TagLibraryView.vue'
import LoraView from '@/views/LoraView.vue'
import SettingsView from '@/views/SettingsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/tags' },
    { path: '/tags', name: 'tags', component: TagLibraryView },
    { path: '/loras', name: 'loras', component: LoraView },
    { path: '/settings', name: 'settings', component: SettingsView }
  ]
})

export default router
