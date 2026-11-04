import { createRouter, createWebHistory } from 'vue-router'
import TagLibraryView from '@/views/TagLibraryView.vue'
import LoraView from '@/views/LoraView.vue'
import SettingsView from '@/views/SettingsView.vue'
import GroupLibraryView from '@/views/GroupLibraryView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/tags' },
    { path: '/tags', name: 'tags', component: TagLibraryView },
    { path: '/loras', name: 'loras', component: LoraView },
    { path: '/groups', name: 'groups', component: GroupLibraryView },
    { path: '/settings', name: 'settings', component: SettingsView }
  ]
})

export default router
