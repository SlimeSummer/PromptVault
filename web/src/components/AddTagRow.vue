<template>
  <div class="add-row">
    <el-button type="primary" plain :icon="Plus" @click="open">从词库添加</el-button>
    <span class="hint">{{ polarity === 'neg' ? '负向' : '正向' }}：已添加 {{ existingTagIds.length }} 条</span>

    <el-dialog v-model="visible" title="从词库选择" width="640px" append-to-body>
      <div class="search">
        <el-input
          v-model="keyword"
          placeholder="按英文 / 中文搜索，回车确认"
          clearable
          style="width: 340px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button :icon="Search" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border size="small" max-height="360" stripe>
        <el-table-column prop="prompt" label="提示词" min-width="220" />
        <el-table-column prop="nameZh" label="中文" width="150">
          <template #default="{ row }">
            <span class="muted">{{ row.nameZh || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="isAdded(row.id)" @click="pick(row)">
              {{ isAdded(row.id) ? '已添加' : '添加' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的词条" :image-size="60" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>

      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { fetchTags } from '@/api/tag'

const props = defineProps({
  polarity: { type: String, required: true },
  existingTagIds: { type: Array, default: () => [] }
})
const emit = defineEmits(['added'])

const visible = ref(false)
const keyword = ref('')
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const justAdded = ref([])

function isAdded(tagId) {
  return props.existingTagIds.includes(tagId) || justAdded.value.includes(tagId)
}

async function load() {
  loading.value = true
  try {
    const data = await fetchTags({ keyword: keyword.value, page: page.value, size })
    rows.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function open() {
  keyword.value = ''
  page.value = 1
  justAdded.value = []
  visible.value = true
  load()
}

function onSearch() {
  page.value = 1
  load()
}

function onPageChange(value) {
  page.value = value
  load()
}

function pick(row) {
  emit('added', { tagId: row.id, polarity: props.polarity })
  if (!justAdded.value.includes(row.id)) {
    justAdded.value.push(row.id)
  }
}
</script>

<style scoped>
.add-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hint {
  font-size: 12px;
  color: #8b909a;
}

.search {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.muted {
  color: #8b909a;
  font-size: 13px;
}

.pager {
  margin-top: 12px;
  text-align: right;
}
</style>
