<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>LoRA 档案</h1>
        <p class="desc">
          一个 LoRA 一份档案。每个预设都是它的「角色基线」：身份 + 穿搭 + 画风锚点，正向与负向都存。
        </p>
      </div>
      <div class="head-actions">
        <el-button type="primary" :icon="Plus" @click="openLoraDialog()">新增 LoRA</el-button>
      </div>
    </header>

    <section class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索名称 / 触发词 / 备注，回车确认"
        clearable
        style="width: 320px"
        @keyup.enter="loadLoras"
        @clear="loadLoras"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="loadLoras">搜索</el-button>
      <span class="total">共 {{ loras.length }} 个 LoRA</span>
    </section>

    <el-table
      v-loading="loading"
      :data="loras"
      stripe
      border
      class="table"
      @sort-change="onSortChange"
    >
      <el-table-column prop="name" label="名称" min-width="180" sortable="custom">
        <template #default="{ row }">
          <span class="lora-name">{{ row.name }}</span>
          <el-tag size="small" effect="light" :type="row.source === 'self_trained' ? 'warning' : 'info'">
            {{ row.source === 'self_trained' ? '自训练' : '第三方' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="baseModel" label="底模" width="120" sortable="custom">
        <template #default="{ row }">
          <span class="muted">{{ row.baseModel || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="triggerWords" label="触发词" min-width="200" sortable="custom">
        <template #default="{ row }">
          <code v-if="row.triggerWords" class="prompt-text">{{ row.triggerWords }}</code>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column prop="presetCount" label="预设" width="90" align="center" sortable="custom">
        <template #default="{ row }">
          <el-tag size="small" type="success" effect="plain">{{ row.presetCount || 0 }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="muted">{{ row.note || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPresetDrawer(row)">预设管理</el-button>
          <el-button link type="primary" @click="openLoraDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="removeLora(row)">删除</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="还没有 LoRA 档案，先新增一个" :image-size="80" />
      </template>
    </el-table>

    <!-- LoRA 新增 / 编辑 -->
    <el-dialog
      v-model="loraDialogVisible"
      :title="loraForm.id ? '编辑 LoRA' : '新增 LoRA'"
      width="540px"
      @closed="loraFormRef?.clearValidate()"
    >
      <el-form ref="loraFormRef" :model="loraForm" :rules="loraRules" label-width="86px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="loraForm.name" placeholder="例如 anna_dooley_v1" />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="loraForm.source">
            <el-radio-button value="third_party">第三方</el-radio-button>
            <el-radio-button value="self_trained">自训练</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="底模">
          <el-select v-model="loraForm.baseModel" clearable placeholder="选择或留空" style="width: 100%">
            <el-option v-for="item in baseModels" :key="item" :value="item" :label="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发词">
          <el-input
            v-model="loraForm.triggerWords"
            type="textarea"
            :rows="2"
            placeholder="例如 anna dooley, serafuku, school uniform"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="loraForm.note" type="textarea" :rows="2" placeholder="这个 LoRA 的脾气、注意事项" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="loraDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loraSaving" @click="submitLora">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预设列表抽屉 -->
    <el-drawer
      v-model="presetDrawerVisible"
      :title="currentLora ? `${currentLora.name} · 预设` : '预设'"
      size="640px"
      append-to-body
    >
      <div class="drawer-toolbar">
        <el-button type="primary" :icon="Plus" @click="openPresetDialog">新增预设</el-button>
        <span class="muted">预设 = 角色基线，动作 / 场景 / 表情留给编辑区</span>
      </div>

      <el-table v-loading="presetLoading" :data="presets" stripe border>
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            <span class="lora-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_TYPES[row.status]" effect="light">
              {{ STATUS_LABELS[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="条目" width="70" align="center">
          <template #default="{ row }">{{ row.itemCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="muted">{{ row.note || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">编辑标签</el-button>
            <el-button link @click="copyPreset(row)">复制</el-button>
            <el-button link type="danger" @click="removePreset(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有预设，先建一个角色基线" :image-size="70" />
        </template>
      </el-table>
    </el-drawer>

    <!-- 新增预设 -->
    <el-dialog v-model="presetDialogVisible" title="新增预设" width="480px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="presetForm.name" placeholder="例如 白色水手服" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="presetForm.status" style="width: 100%">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="presetForm.note" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="presetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="presetSaving" @click="submitPreset">创建</el-button>
      </template>
    </el-dialog>

    <!-- 预设编辑抽屉 -->
    <el-drawer
      v-model="editorVisible"
      :title="editorForm.name ? `编辑预设 · ${editorForm.name}` : '编辑预设'"
      size="780px"
      append-to-body
    >
      <el-form label-width="72px" class="editor-meta">
        <el-form-item label="名称">
          <el-input v-model="editorForm.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editorForm.status" style="width: 180px">
            <el-option
              v-for="item in STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editorForm.note" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="metaSaving" @click="saveMeta">保存基础信息</el-button>
        </el-form-item>
      </el-form>

      <div v-for="section in SECTIONS" :key="section.polarity" class="section">
        <div class="section-head">
          <h3>{{ section.title }}</h3>
          <span class="count">{{ itemsOf(section.polarity).length }} 条</span>
        </div>

        <div class="add-row">
          <el-select
            v-model="newTagId[section.polarity]"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchTagOptions"
            :loading="tagLoading"
            placeholder="从全局词库搜索（英文 / 中文）"
            style="flex: 1"
            @focus="onTagFocus"
          >
            <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.prompt" :value="tag.id">
              <span class="opt-prompt">{{ tag.prompt }}</span>
              <span class="opt-zh">{{ tag.nameZh || '' }}</span>
            </el-option>
          </el-select>
          <el-button type="primary" plain @click="addItem(section.polarity)">添加</el-button>
        </div>

        <ul class="item-list">
          <li v-for="(item, index) in itemsOf(section.polarity)" :key="item.id">
            <span class="idx">{{ index + 1 }}</span>
            <code class="prompt-text">{{ item.prompt }}</code>
            <span class="zh">{{ item.nameZh || '' }}</span>
            <el-input-number
              :model-value="item.weight"
              :min="0.1"
              :max="2"
              :step="0.05"
              :precision="2"
              size="small"
              controls-position="right"
              style="width: 100px"
              @change="(value) => onWeightChange(item, value)"
            />
            <el-button link @click="moveItem(section.polarity, index, -1)">上移</el-button>
            <el-button link @click="moveItem(section.polarity, index, 1)">下移</el-button>
            <el-button link type="warning" @click="togglePolarity(item)">
              {{ section.polarity === 'pos' ? '转负向' : '转正向' }}
            </el-button>
            <el-button link type="danger" @click="removeItem(item)">移除</el-button>
          </li>
          <li v-if="!itemsOf(section.polarity).length" class="empty-line">暂无词条</li>
        </ul>
      </div>

      <div class="preview">
        <div class="preview-head">
          <h3>拼接预览</h3>
          <div>
            <el-button size="small" @click="copyText(positiveText)">复制正向</el-button>
            <el-button size="small" @click="copyText(negativeText)">复制负向</el-button>
          </div>
        </div>
        <dl>
          <dt>正向</dt>
          <dd class="preview-text">{{ positiveText || '—' }}</dd>
          <dt>负向</dt>
          <dd class="preview-text">{{ negativeText || '—' }}</dd>
        </dl>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { fetchTags } from '@/api/tag'
import { fetchSettings } from '@/api/setting'
import {
  addPresetItem,
  createLora,
  createPreset,
  deleteLora,
  deletePreset,
  deletePresetItem,
  duplicatePreset,
  fetchLoraPresets,
  fetchLoras,
  fetchPresetDetail,
  reorderPresetItems,
  updateLora,
  updatePreset,
  updatePresetItem
} from '@/api/lora'

const DEFAULT_BASE_MODELS = ['sd15', 'sdxl', 'pony', 'illustrious', 'noobai', 'other']
const baseModels = ref([...DEFAULT_BASE_MODELS])

const STATUS_OPTIONS = [
  { value: 'draft', label: '草稿' },
  { value: 'verified', label: '已验证' },
  { value: 'deprecated', label: '已弃用' }
]

const STATUS_LABELS = { draft: '草稿', verified: '已验证', deprecated: '已弃用' }
const STATUS_TYPES = { draft: 'info', verified: 'success', deprecated: 'danger' }

const SECTIONS = [
  { polarity: 'pos', title: '正向提示词' },
  { polarity: 'neg', title: '负向提示词' }
]

const keyword = ref('')
const sortField = ref('')
const sortOrder = ref('')
const loading = ref(false)
const loras = ref([])

const loraDialogVisible = ref(false)
const loraSaving = ref(false)
const loraFormRef = ref()
const loraForm = reactive({
  id: null,
  name: '',
  source: 'third_party',
  baseModel: '',
  triggerWords: '',
  note: ''
})
const loraRules = {
  name: [{ required: true, message: '请输入 LoRA 名称', trigger: 'blur' }]
}

const presetDrawerVisible = ref(false)
const currentLora = ref(null)
const presets = ref([])
const presetLoading = ref(false)

const presetDialogVisible = ref(false)
const presetSaving = ref(false)
const presetForm = reactive({ name: '', status: 'draft', note: '' })

const editorVisible = ref(false)
const currentPresetId = ref(null)
const posItems = ref([])
const negItems = ref([])
const editorForm = reactive({ name: '', status: 'draft', note: '' })
const metaSaving = ref(false)

const newTagId = reactive({ pos: null, neg: null })
const tagOptions = ref([])
const tagLoading = ref(false)

const positiveText = computed(() => posItems.value.map(formatItem).join(', '))
const negativeText = computed(() => negItems.value.map(formatItem).join(', '))

function formatItem(item) {
  const weight = Number(item.weight ?? 1)
  if (Math.abs(weight - 1) < 0.001) {
    return item.prompt
  }
  return `(${item.prompt}:${weight})`
}

function itemsOf(polarity) {
  return polarity === 'pos' ? posItems.value : negItems.value
}

async function loadLoras() {
  loading.value = true
  try {
    loras.value = await fetchLoras(keyword.value, sortField.value, sortOrder.value)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function onSortChange({ prop, order }) {
  sortField.value = order ? prop : ''
  sortOrder.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  loadLoras()
}

function openLoraDialog(row) {
  Object.assign(
    loraForm,
    row
      ? {
          id: row.id,
          name: row.name,
          source: row.source || 'third_party',
          baseModel: row.baseModel || '',
          triggerWords: row.triggerWords || '',
          note: row.note || ''
        }
      : { id: null, name: '', source: 'third_party', baseModel: '', triggerWords: '', note: '' }
  )
  loraDialogVisible.value = true
}

async function submitLora() {
  try {
    await loraFormRef.value.validate()
  } catch (e) {
    return
  }

  loraSaving.value = true
  try {
    const payload = {
      name: loraForm.name,
      source: loraForm.source,
      baseModel: loraForm.baseModel,
      triggerWords: loraForm.triggerWords,
      note: loraForm.note
    }
    if (loraForm.id) {
      await updateLora(loraForm.id, payload)
      ElMessage.success('已保存修改')
    } else {
      await createLora(payload)
      ElMessage.success('已新增 LoRA')
    }
    loraDialogVisible.value = false
    await loadLoras()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loraSaving.value = false
  }
}

async function removeLora(row) {
  try {
    await ElMessageBox.confirm(`确定删除 LoRA「${row.name}」吗？`, '删除 LoRA', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }

  try {
    await deleteLora(row.id)
    ElMessage.success('已删除')
    if (currentLora.value && currentLora.value.id === row.id) {
      presetDrawerVisible.value = false
      currentLora.value = null
    }
    await loadLoras()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openPresetDrawer(row) {
  currentLora.value = row
  presets.value = []
  presetDrawerVisible.value = true
  loadPresets()
}

async function loadPresets() {
  if (!currentLora.value) {
    return
  }
  presetLoading.value = true
  try {
    presets.value = await fetchLoraPresets(currentLora.value.id)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    presetLoading.value = false
  }
}

function openPresetDialog() {
  Object.assign(presetForm, { name: '', status: 'draft', note: '' })
  presetDialogVisible.value = true
}

async function submitPreset() {
  if (!presetForm.name.trim()) {
    ElMessage.warning('预设名称不能为空')
    return
  }

  presetSaving.value = true
  try {
    await createPreset({
      loraId: currentLora.value.id,
      name: presetForm.name,
      status: presetForm.status,
      note: presetForm.note
    })
    ElMessage.success('已创建预设')
    presetDialogVisible.value = false
    await Promise.all([loadPresets(), loadLoras()])
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    presetSaving.value = false
  }
}

async function copyPreset(row) {
  try {
    await duplicatePreset(row.id)
    ElMessage.success(`已复制为「${row.name} 副本」`)
    await Promise.all([loadPresets(), loadLoras()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function removePreset(row) {
  try {
    await ElMessageBox.confirm(`确定删除预设「${row.name}」吗？其中的标签条目也会一起删除。`, '删除预设', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }

  try {
    await deletePreset(row.id)
    ElMessage.success('已删除')
    await Promise.all([loadPresets(), loadLoras()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function openEditor(row) {
  currentPresetId.value = row.id
  try {
    await loadDetail()
    editorVisible.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadDetail() {
  const data = await fetchPresetDetail(currentPresetId.value)
  const preset = data.preset || {}
  Object.assign(editorForm, {
    name: preset.name || '',
    status: preset.status || 'draft',
    note: preset.note || ''
  })
  const items = data.items || []
  posItems.value = items.filter((item) => item.polarity === 'pos')
  negItems.value = items.filter((item) => item.polarity === 'neg')
}

async function saveMeta() {
  if (!editorForm.name.trim()) {
    ElMessage.warning('预设名称不能为空')
    return
  }

  metaSaving.value = true
  try {
    await updatePreset(currentPresetId.value, {
      name: editorForm.name,
      status: editorForm.status,
      note: editorForm.note
    })
    ElMessage.success('已保存基础信息')
    await Promise.all([loadPresets(), loadLoras()])
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    metaSaving.value = false
  }
}

async function searchTagOptions(query) {
  tagLoading.value = true
  try {
    const data = await fetchTags({ keyword: query || '', page: 1, size: 30 })
    tagOptions.value = data.list || []
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    tagLoading.value = false
  }
}

function onTagFocus() {
  if (!tagOptions.value.length) {
    searchTagOptions('')
  }
}

async function addItem(polarity) {
  const tagId = newTagId[polarity]
  if (!tagId) {
    ElMessage.warning('请先选择一个词条')
    return
  }

  try {
    const created = await addPresetItem(currentPresetId.value, { tagId, polarity })
    itemsOf(polarity).push(created)
    newTagId[polarity] = null
    ElMessage.success('已添加')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function onWeightChange(item, value) {
  const weight = Number(value)
  item.weight = weight
  try {
    await updatePresetItem(currentPresetId.value, item.id, { weight })
  } catch (e) {
    ElMessage.error(e.message)
    await loadDetail()
  }
}

async function moveItem(polarity, index, delta) {
  const list = itemsOf(polarity)
  const target = index + delta
  if (target < 0 || target >= list.length) {
    return
  }

  const temp = list[index]
  list[index] = list[target]
  list[target] = temp

  try {
    await reorderPresetItems(
      currentPresetId.value,
      [...posItems.value, ...negItems.value].map((item) => item.id)
    )
  } catch (e) {
    ElMessage.error(e.message)
    await loadDetail()
  }
}

async function togglePolarity(item) {
  const target = item.polarity === 'pos' ? 'neg' : 'pos'
  try {
    const updated = await updatePresetItem(currentPresetId.value, item.id, { polarity: target })
    const source = itemsOf(item.polarity)
    const index = source.findIndex((entry) => entry.id === item.id)
    if (index >= 0) {
      source.splice(index, 1)
    }
    itemsOf(target).push(updated)
    ElMessage.success(target === 'pos' ? '已转为正向' : '已转为负向')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function removeItem(item) {
  try {
    await deletePresetItem(currentPresetId.value, item.id)
    const list = itemsOf(item.polarity)
    const index = list.findIndex((entry) => entry.id === item.id)
    if (index >= 0) {
      list.splice(index, 1)
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function copyText(text) {
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.warning('浏览器拒绝了剪贴板访问')
  }
}

async function loadBaseModels() {
  try {
    const data = await fetchSettings()
    const list = data.loraBaseModels || []
    if (list.length) {
      baseModels.value = list
    }
  } catch (e) {
    baseModels.value = [...DEFAULT_BASE_MODELS]
  }
}

onMounted(() => {
  loadBaseModels()
  loadLoras()
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

.lora-name {
  margin-right: 8px;
  font-weight: 600;
  color: #1f2329;
}

.prompt-text {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #3b3f46;
  word-break: break-all;
}

.muted {
  color: #9aa0a6;
}

.drawer-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.drawer-toolbar .muted {
  font-size: 12px;
}

.editor-meta {
  padding: 4px 0 14px;
  border-bottom: 1px solid #ebeef5;
}

.section {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.section-head h3,
.preview-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.count {
  font-size: 12px;
  color: #9aa0a6;
}

.add-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.item-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 8px;
  background: #fafafa;
}

.item-list li.empty-line {
  justify-content: center;
  color: #a8abb2;
  font-size: 13px;
}

.idx {
  width: 20px;
  text-align: right;
  font-size: 12px;
  color: #9aa0a6;
}

.zh {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #9aa0a6;
}

.opt-prompt {
  font-family: Consolas, monospace;
}

.opt-zh {
  float: right;
  margin-left: 12px;
  font-size: 12px;
  color: #9aa0a6;
}

.preview {
  padding: 16px 0 8px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.preview dl {
  margin: 0;
}

.preview dt {
  margin: 10px 0 4px;
  font-size: 12px;
  color: #8a9099;
}

.preview-text {
  margin: 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f5f6f8;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  color: #3b3f46;
  word-break: break-all;
}
</style>
