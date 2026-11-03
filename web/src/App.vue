<template>
  <el-container class="shell">
    <el-header class="header">
      <div class="brand">
        <span class="logo">PV</span>
        <span class="title">PromptVault</span>
        <span class="tagline">ComfyUI 提示词经验库</span>
      </div>
      <el-tag :type="online ? 'success' : 'danger'" effect="dark" round>
        {{ online ? '后端已连接' : '后端未连接' }}
      </el-tag>
    </el-header>

    <el-main class="main">
      <el-result icon="success" title="阶段 0 · 项目骨架已就绪">
        <template #subtitle>
          <p>数据库已初始化，前后端链路通畅。</p>
          <p class="hint">接下来按模块交付：词库 → LoRA 预设 → 词组 → 结构化编辑区 → PNG 导入 → AI 生成。</p>
        </template>
      </el-result>

      <el-descriptions v-if="info" :column="3" border class="info">
        <el-descriptions-item label="服务状态">{{ info.status }}</el-descriptions-item>
        <el-descriptions-item label="数据表数量">{{ info.tables }}</el-descriptions-item>
        <el-descriptions-item label="服务器时间">{{ info.time }}</el-descriptions-item>
      </el-descriptions>
    </el-main>
  </el-container>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const online = ref(false)
const info = ref(null)

onMounted(async () => {
  try {
    const res = await fetch('/api/health')
    const body = await res.json()
    if (body.code === 0) {
      online.value = true
      info.value = body.data
    }
  } catch (e) {
    online.value = false
  }
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background: #f5f6f8;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1b1d22;
  color: #fff;
  border-bottom: 2px solid #6c5ce7;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  font-weight: 700;
  letter-spacing: 1px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.tagline {
  font-size: 12px;
  color: #9aa0a6;
}

.main {
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 20px;
}

.hint {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}

.info {
  margin-top: 8px;
}
</style>
