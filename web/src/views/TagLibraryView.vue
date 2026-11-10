<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>全局词库</h1>
        <p class="desc">所有原子提示词都在这里。按分类归档，中英文都能搜。</p>
      </div>
      <div class="head-actions">
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="onBatchDelete">
          批量删除（{{ selectedIds.length }}）
        </el-button>
        <el-button :icon="Upload" @click="openBatch">批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增词条</el-button>
      </div>
    </header>

    <section class="toolbar">
      <el-autocomplete
        v-model="keyword"
        :fetch-suggestions="querySuggestions"
        :trigger-on-focus="false"
        :highlight-first-item="false"
        clearable
        placeholder="搜索英文标签 / 中文释义，回车确认"
        style="width: 340px"
        @keyup.enter="onSearch"
        @select="onSearch"
        @clear="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #default="{ item }">
          <span style="font-family: 'JetBrains Mono', Consolas, monospace; font-size: 13px; color: #3b3f46">
            {{ item.prompt }}
          </span>
          <span style="float: right; margin-left: 16px; font-size: 12px; color: #9aa0a6">
            {{ item.nameZh || '' }}
          </span>
        </template>
      </el-autocomplete>

      <el-button :icon="Search" @click="onSearch">搜索</el-button>

      <el-checkbox v-model="onlyFavorite" @change="onSearch">只看收藏</el-checkbox>

      <el-select
        v-model="category"
        :placeholder="`全部分类（${stats.total}）`"
        clearable
        style="width: 190px"
        @change="onSearch"
      >
        <el-option
          v-for="item in categories"
          :key="item.value"
          :value="item.value"
          :label="`${item.label}（${countOf(item.value)}）`"
        />
      </el-select>

      <span class="total">当前筛选 {{ total }} 条</span>
    </section>

    <el-table
      v-loading="loading"
      :data="tags"
      stripe
      border
      class="table"
      @sort-change="onSortChange"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="45" />

      <el-table-column label="收藏" width="70" align="center">
        <template #default="{ row }">
          <el-button link class="star-btn" @click="onToggleFavorite(row)">
            <el-icon :size="16" :color="row.favorite ? '#f7ba2a' : '#c0c4cc'">
              <StarFilled v-if="row.favorite" />
              <Star v-else />
            </el-icon>
          </el-button>
        </template>
      </el-table-column>

      <el-table-column prop="prompt" label="提示词" width="220" sortable="custom">
        <template #default="{ row }">
          <code class="prompt-text">{{ row.prompt }}</code>
          <el-button link type="primary" size="small" @click="copy(row.prompt)">复制</el-button>
        </template>
      </el-table-column>

      <el-table-column prop="nameZh" label="中文释义" width="180" sortable="custom">
        <template #default="{ row }">
          <span class="muted">{{ row.nameZh || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="category" label="分类" width="120" sortable="custom">
        <template #default="{ row }">
          <el-tag :type="typeOf(row.category)" effect="light" size="small">
            {{ labelOf(row.category) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="muted">{{ row.note || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openImages(row)">图片</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="还没有词条，先新增一条或批量导入" :image-size="80" />
      </template>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增词条' : '编辑词条'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="提示词" prop="prompt">
          <el-input v-model="form.prompt" placeholder="英文标签，如 blue eyes" />
        </el-form-item>
        <el-form-item label="中文释义">
          <el-input v-model="form.nameZh" placeholder="如 蓝眼睛（用于中文搜索）" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option
              v-for="item in categories"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="2"
            placeholder="可选，例如适用场景或注意事项"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 参考图片 -->
    <el-dialog v-model="imageDialog.visible" :title="imageDialog.title" width="560px">
      <ImagePanel
        v-if="imageDialog.visible"
        ref-type="tag"
        :ref-id="imageDialog.refId"
      />
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="batchVisible" title="批量导入词条" width="580px" @closed="resetBatch">
      <el-form label-width="88px">
        <el-form-item label="导入内容">
          <el-input
            v-model="batchText"
            type="textarea"
            :rows="8"
            placeholder="支持逗号、顿号或换行分隔，例如：&#10;(masterpiece:1.2), best quality, blue eyes&#10;long hair, smile"
          />
        </el-form-item>
        <el-form-item label="自动判断">
          <el-checkbox
            v-model="batchAutoCategory"
            @change="onAutoCategoryToggle"
          >勾选后忽略下方"归类为"，逐条按关键词规则自动分类</el-checkbox>
        </el-form-item>
        <el-form-item label="归类为">
          <el-select v-model="batchCategory" style="width: 100%" :disabled="batchAutoCategory">
            <el-option
              v-for="item in categories"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <p class="tip">
        提示：权重写法 <code>(tag:1.2)</code> 会自动去掉括号与权重，只保留标签本体；已存在的词条自动跳过。
      </p>

      <el-alert
        v-if="batchResult"
        :type="batchResult.skipped > 0 ? 'warning' : 'success'"
        :closable="false"
        show-icon
        :title="`解析 ${batchResult.total} 条：新增 ${batchResult.inserted} 条，跳过 ${batchResult.skipped} 条${batchResult.autoCategory ? '（自动分类）' : ''}`"
      />

      <div v-if="batchResult && batchResult.byCategory && Object.keys(batchResult.byCategory).length" class="batch-cat-summary">
        <div v-for="(stat, cat) in batchResult.byCategory" :key="cat" class="cat-row">
          <el-tag :type="typeOf(cat)" effect="plain" size="small">{{ labelOf(cat) }}</el-tag>
          <span class="cat-num">新增 {{ stat.inserted }} · 跳过 {{ stat.skipped }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="batchVisible = false">关闭</el-button>
        <el-button type="primary" :loading="batchLoading" @click="submitBatch">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Star, StarFilled, Upload } from '@element-plus/icons-vue'
import {
  batchDeleteTags,
  batchImportTags,
  createTag,
  deleteTag,
  fetchTagStats,
  fetchTagSuggestions,
  fetchTags,
  toggleTagFavorite,
  updateTag
} from '@/api/tag'
import { fetchTagCategories } from '@/api/setting'
import ImagePanel from '@/components/ImagePanel.vue'

const FALLBACK_CATEGORIES = [
  { value: 'trigger', label: '触发词' },
  { value: 'quality', label: '质量词' },
  { value: 'style', label: '风格' },
  { value: 'outfit', label: '穿搭' },
  { value: 'scene', label: '场景' },
  { value: 'action', label: '动作' },
  { value: 'expression', label: '表情' },
  { value: 'detail', label: '细节' },
  { value: 'artist', label: '画师' },
  { value: 'negative', label: '负面' },
  { value: 'other', label: '其他' }
]

const categories = ref(FALLBACK_CATEGORIES)

const CATEGORY_TYPES = {
  trigger: 'warning',
  quality: 'info',
  style: undefined,
  outfit: 'success',
  scene: undefined,
  action: 'danger',
  expression: 'warning',
  detail: 'info',
  artist: 'warning',
  negative: 'danger',
  other: 'info'
}

const loading = ref(false)
const tags = ref([])
const selectedIds = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const category = ref('')
const onlyFavorite = ref(false)
const sortField = ref('')
const sortOrder = ref('')
const stats = reactive({ total: 0, categories: {} })

const dialogVisible = ref(false)
const dialogMode = ref('create')
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, prompt: '', nameZh: '', category: 'other', note: '' })
const rules = {
  prompt: [{ required: true, message: '请输入提示词', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const batchVisible = ref(false)
const batchText = ref('')
const batchCategory = ref('other')
const batchAutoCategory = ref(false)
const batchLoading = ref(false)
const batchResult = ref(null)

const imageDialog = reactive({ visible: false, refId: null, title: '' })

function openImages(row) {
  imageDialog.refId = row.id
  imageDialog.title = `参考图片 · ${row.prompt}`
  imageDialog.visible = true
}

function labelOf(value) {
  return categories.value.find((item) => item.value === value)?.label || value
}

function defaultCategory() {
  return categories.value.some((item) => item.value === 'other')
    ? 'other'
    : categories.value[0]?.value || 'other'
}

function typeOf(value) {
  return CATEGORY_TYPES[value]
}

function countOf(value) {
  return stats.categories[value] || 0
}

async function load() {
  loading.value = true
  try {
    const data = await fetchTags({
      keyword: keyword.value,
      category: category.value,
      sort: sortField.value,
      order: sortOrder.value,
      favorite: onlyFavorite.value ? true : '',
      page: page.value,
      size: size.value
    })
    tags.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await fetchTagStats()
    stats.total = data.total || 0
    stats.categories = data.categories || {}
  } catch (e) {
    // 统计失败不影响主流程
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onSortChange({ prop, order }) {
  sortField.value = order ? prop : ''
  sortOrder.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  page.value = 1
  load()
}

async function onToggleFavorite(row) {
  try {
    const data = await toggleTagFavorite(row.id)
    row.favorite = data.favorite
    if (onlyFavorite.value && !data.favorite) {
      await Promise.all([load(), loadStats()])
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function querySuggestions(queryString, callback) {
  const query = (queryString || '').trim()
  if (!query) {
    callback([])
    return
  }
  try {
    const list = await fetchTagSuggestions(query)
    callback((list || []).map((item) => ({ ...item, value: item.prompt })))
  } catch (e) {
    callback([])
  }
}

function onPageChange(value) {
  page.value = value
  load()
}

function onSizeChange(value) {
  size.value = value
  page.value = 1
  load()
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: null, prompt: '', nameZh: '', category: defaultCategory(), note: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    prompt: row.prompt,
    nameZh: row.nameZh || '',
    category: row.category || 'other',
    note: row.note || ''
  })
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  saving.value = true
  try {
    const payload = {
      prompt: form.prompt,
      nameZh: form.nameZh,
      category: form.category,
      note: form.note
    }
    if (dialogMode.value === 'create') {
      await createTag(payload)
      ElMessage.success('已新增词条')
    } else {
      await updateTag(form.id, payload)
      ElMessage.success('已保存修改')
    }
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function resetForm() {
  formRef.value?.clearValidate()
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

async function onBatchDelete() {
  if (selectedIds.value.length === 0) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条词条吗？此操作不可撤销。`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    const data = await batchDeleteTags(selectedIds.value)
    ElMessage.success(`已删除 ${data.deleted} 条`)
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 后端会把被引用的词条拼成错误信息
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.prompt}」吗？`, '删除词条', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }

  try {
    await deleteTag(row.id)
    ElMessage.success('已删除')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.warning('浏览器拒绝了剪贴板访问')
  }
}

function openBatch() {
  if (!categories.value.some((item) => item.value === batchCategory.value)) {
    batchCategory.value = defaultCategory()
  }
  batchAutoCategory.value = false
  batchResult.value = null
  batchVisible.value = true
}

function onAutoCategoryToggle(val) {
  if (val) {
    batchCategory.value = ''
  } else if (!categories.value.some((item) => item.value === batchCategory.value)) {
    batchCategory.value = defaultCategory()
  }
}

async function submitBatch() {
  if (!batchText.value.trim()) {
    ElMessage.warning('请先粘贴要导入的提示词')
    return
  }

  batchLoading.value = true
  try {
    const data = await batchImportTags({
      text: batchText.value,
      category: batchAutoCategory.value ? undefined : batchCategory.value,
      autoCategory: batchAutoCategory.value || undefined
    })
    batchResult.value = data
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    batchLoading.value = false
  }
}

function resetBatch() {
  batchText.value = ''
  batchCategory.value = defaultCategory()
  batchAutoCategory.value = false
  batchResult.value = null
}

async function loadCategories() {
  try {
    const data = await fetchTagCategories()
    if (Array.isArray(data) && data.length) {
      categories.value = data
    }
  } catch (e) {
    categories.value = FALLBACK_CATEGORIES
  }
}

onMounted(() => {
  loadCategories()
  loadStats()
  load()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

h1 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
}

.desc {
  margin: 0;
  font-size: 13px;
  color: #8a9099;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.total {
  margin-left: auto;
  font-size: 13px;
  color: #8a9099;
}

.table {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
}

.prompt-text {
  display: inline-block;
  margin-right: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  color: #3b3f46;
  word-break: break-all;
}

.batch-cat-summary {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 3px 0;
}

.cat-num {
  font-size: 13px;
  color: #606266;
}

.muted {
  color: #9aa0a6;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.tip {
  margin: 0 0 12px;
  font-size: 12px;
  color: #8a9099;
  line-height: 1.7;
}

.tip code {
  padding: 1px 4px;
  border-radius: 3px;
  background: #f3f4f6;
  font-family: Consolas, monospace;
}
</style>
