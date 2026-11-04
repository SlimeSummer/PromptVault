<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="logo">PV</span>
        <div class="brand-text">
          <div class="name">PromptVault</div>
          <div class="sub">提示词经验库</div>
        </div>
      </div>

      <nav class="menu">
        <router-link to="/tags" class="menu-item" active-class="active">
          <el-icon><Collection /></el-icon>
          <span>全局词库</span>
        </router-link>
        <router-link to="/loras" class="menu-item" active-class="active">
          <el-icon><Postcard /></el-icon>
          <span>LoRA 档案</span>
        </router-link>
        <router-link to="/groups" class="menu-item" active-class="active">
          <el-icon><Files /></el-icon>
          <span>词组库</span>
        </router-link>
        <div class="menu-item locked">
          <el-icon><EditPen /></el-icon>
          <span>编辑区</span>
          <em>阶段 4</em>
        </div>
        <router-link to="/settings" class="menu-item" active-class="active">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </router-link>
      </nav>

      <div class="status">
        <i :class="{ on: online }"></i>
        {{ online ? '后端已连接' : '后端未连接' }}
      </div>
    </aside>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Collection, EditPen, Files, Postcard, Setting } from '@element-plus/icons-vue'

const online = ref(false)

onMounted(async () => {
  try {
    const response = await fetch('/api/health')
    const body = await response.json()
    online.value = body.code === 0
  } catch (e) {
    online.value = false
  }
})
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f5f6f8;
}

.sidebar {
  display: flex;
  flex-direction: column;
  width: 220px;
  flex-shrink: 0;
  background: #1b1d22;
  color: #fff;
  padding: 20px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 22px;
  border-bottom: 1px solid #2b2e35;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  font-weight: 700;
  letter-spacing: 1px;
}

.name {
  font-size: 17px;
  font-weight: 600;
}

.sub {
  font-size: 11px;
  color: #8b909a;
}

.menu {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #c7cbd4;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.15s, color 0.15s;
}

.menu-item:hover {
  background: #262931;
  color: #fff;
}

.menu-item.active {
  background: #6c5ce7;
  color: #fff;
}

.menu-item.locked {
  color: #6b707a;
  cursor: not-allowed;
}

.menu-item em {
  margin-left: auto;
  font-style: normal;
  font-size: 10px;
  color: #6b707a;
}

.status {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px 2px;
  font-size: 12px;
  color: #8b909a;
}

.status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.status i.on {
  background: #67c23a;
}

.content {
  flex: 1;
  min-width: 0;
  padding: 28px 32px 48px;
}
</style>
