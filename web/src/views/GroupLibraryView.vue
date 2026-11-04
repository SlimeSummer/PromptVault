<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>词组库</h1>
        <p class="desc">经验组合的容器：例如"赛博朋克夜街"、"角色全套穿搭"。可整体插入编辑区。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增词组</el-button>
      </div>
    </header>

    <section class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按名称搜索"
        clearable
        style="width: 260px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-segmented v-model="scopeSeg" :options="scopeOptions" @change="load" />

      <el-select
        v-if="scopeSeg === 'lora'"
        v-model="loraId"
        placeholder="选择 LoRA"
        style="width: 180px"
        @change="load"
      >
        <el-option
          v-for="item in loras"
          :key="item.id"
          :value="item.id"
          :label="item.name"
        />
      </el-select>
    </section>

    <el-table v-loading="loading" :data="groups" stripe border class="table">
      <el-table-column prop="name" label="名称" min-width="200">
        <template #default="{ row }">
          <span class="group-name">{{ row.name }}</span>
          <el-tag v-if="row.kind === 'global_negative'" type="danger" effect="plain" size="small">
            全局负面
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="范围" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.scope === 'global' ? 'info' : 'success'" effect="light">
            {{ row.scope === 'global' ? '全局' : row.loraName }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="itemCount" label="条目" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.itemCount || 0 }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="muted">{{ row.note || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="duplicate(row)">复制</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="还没有词组，先新增一个吧" :image-size="80" />
      </template>
    </el-table>

    <!-- 新增 / 编辑抽屉 -->
    <el-drawer
      v-model="editVisible"
      :title="editMode === 'create' ? '新增词组' : '编辑词组'"
      size="480px"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" placeholder="例如：赛博朋克夜街" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="kind">
          <el-radio-group v-model="editForm.kind">
            <el-radio-button label="normal">普通词组</el-radio-button>
            <el-radio-button label="global_negative">全局负面</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="范围" prop="scope">
          <el-radio-group v-model="editForm.scope" @change="onScopeChange">
            <el-radio-button label="global">全局</el-radio-button>
            <el-radio-button label="lora">LoRA 专属</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.scope === 'lora'" label="LoRA" prop="loraId">
          <el-select v-model="editForm.loraId" placeholder="选择 LoRA" style="width: 100%">
            <el-option
              v-for="item in loras"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editForm.note"
            type="textarea"
            :rows="2"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-drawer>

    <!-- 详情抽屉（条目管理） -->
    <el-drawer
      v-model="detailVisible"
      :title="detailGroup?.name ? `${detailGroup.name} - 条目` : '词组详情'"
      size="620px"
      @closed="resetDetail"
    >
      <div v-if="detailGroup" class="detail-head">
        <el-tag size="small" :type="detailGroup.scope === 'global' ? 'info' : 'success'" effect="light">
          {{ detailGroup.scope === 'global' ? '全局' : (detailGroup.loraName || 'LoRA') }}
        </el-tag>
        <span v-if="detailGroup.note" class="muted">{{ detailGroup.note }}</span>
      </div>

      <el-tabs v-model="detailPolarity" class="detail-tabs">
        <el-tab-pane label="正向" name="pos">
          <AddTagRow polarity="pos" :existing-tag-ids="posTagIds" @added="addItem" />
          <el-table :data="posItems" border size="default" class="item-table">
            <el-table-column prop="prompt" label="提示词" min-width="200">
              <template #default="{ row }">
                <code class="prompt-text">{{ row.prompt }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="nameZh" label="中文" width="120">
              <template #default="{ row }">
                <span class="muted">{{ row.nameZh || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="权重" width="130">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.weight"
                  :min="0.1"
                  :max="2.0"
                  :step="0.1"
                  :precision="2"
                  size="small"
                  @change="(w) => updateItem(row, w, row.polarity)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="负向" name="neg">
          <AddTagRow polarity="neg" :existing-tag-ids="negTagIds" @added="addItem" />
          <el-table :data="negItems" border size="default" class="item-table">
            <el-table-column prop="prompt" label="提示词" min-width="200">
              <template #default="{ row }">
                <code class="prompt-text">{{ row.prompt }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="nameZh" label="中文" width="120">
              <template #default="{ row }">
                <span class="muted">{{ row.nameZh || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="权重" width="130">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.weight"
                  :min="0.1"
                  :max="2.0"
                  :step="0.1"
                  :precision="2"
                  size="small"
                  @change="(w) => updateItem(row, w, row.polarity)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="detail-footer">
        <el-button :icon="DocumentCopy" @click="onCopyPreview">复制拼接结果</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy, Plus, Search } from '@element-plus/icons-vue'
import {
  addGroupItem,
  createGroup,
  deleteGroup,
  deleteGroupItem,
  duplicateGroup,
  fetchGroupDetail,
  fetchGroups,
  reorderGroupItems,
  updateGroup,
  updateGroupItem
} from '@/api/group'
import { fetchLoras } from '@/api/lora'
import AddTagRow from '@/components/AddTagRow.vue'

const scopeOptions = [
  { label: '全部', value: '' },
  { label: '全局', value: 'global' },
  { label: 'LoRA 专属', value: 'lora' }
]

const scopeSeg = ref('')
const loraId = ref(null)
const keyword = ref('')

const loading = ref(false)
const groups = ref([])
const loras = ref([])
const categories = ref([])

// --- 编辑抽屉 ---
const editVisible = ref(false)
const editMode = ref('create')
const saving = ref(false)
const editFormRef = ref()
const editForm = reactive({ id: null, name: '', kind: 'normal', scope: 'global', loraId: null, note: '' })
const editRules = {
  name: [{ required: true, message: '请输入词组名称', trigger: 'blur' }],
  loraId: [{ required: true, message: '请选择 LoRA', trigger: 'change' }]
}

// --- 详情抽屉 ---
const detailVisible = ref(false)
const detailGroup = ref(null)
const detailPolarity = ref('pos')
const detailItems = ref([])

const posItems = computed(() => detailItems.value.filter((i) => i.polarity === 'pos'))
const negItems = computed(() => detailItems.value.filter((i) => i.polarity === 'neg'))
const posTagIds = computed(() => posItems.value.map((i) => i.tagId))
const negTagIds = computed(() => negItems.value.map((i) => i.tagId))

async function load() {
  loading.value = true
  try {
    const scope = scopeSeg.value || null
    groups.value = await fetchGroups(scope, scope === 'lora' ? loraId.value : null, keyword.value)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadLoras() {
  try {
    loras.value = await fetchLoras()
  } catch (e) {
    // 列表失败不阻断主流程
  }
}

function openCreate() {
  editMode.value = 'create'
  Object.assign(editForm, { id: null, name: '', kind: 'normal', scope: 'global', loraId: null, note: '' })
  editVisible.value = true
}

function openEdit(row) {
  editMode.value = 'edit'
  Object.assign(editForm, {
    id: row.id,
    name: row.name,
    kind: row.kind || 'normal',
    scope: row.scope || 'global',
    loraId: row.loraId || null,
    note: row.note || ''
  })
  editVisible.value = true
}

function onScopeChange(val) {
  if (val === 'global') {
    editForm.loraId = null
  } else if (val === 'lora' && !editForm.loraId && loras.value.length > 0) {
    editForm.loraId = loras.value[0].id
  }
}

async function submitEdit() {
  try {
    await editFormRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    if (editMode.value === 'create') {
      await createGroup(editForm)
      ElMessage.success('已新增词组')
    } else {
      await updateGroup(editForm.id, {
        name: editForm.name,
        kind: editForm.kind,
        scope: editForm.scope,
        loraId: editForm.scope === 'lora' ? editForm.loraId : null,
        note: editForm.note
      })
      ElMessage.success('已保存修改')
    }
    editVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function resetEditForm() {
  editFormRef.value?.clearValidate()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除词组「${row.name}」吗？`, '删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteGroup(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function duplicate(row) {
  try {
    await duplicateGroup(row.id)
    ElMessage.success('已复制')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

// --- 详情 ---

async function openDetail(row) {
  detailGroup.value = row
  detailVisible.value = true
  await loadDetail(row.id)
}

async function loadDetail(id) {
  try {
    const data = await fetchGroupDetail(id)
    detailItems.value = data.items || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function resetDetail() {
  detailGroup.value = null
  detailItems.value = []
  detailPolarity.value = 'pos'
}

async function addItem({ tagId, polarity }) {
  try {
    const item = await addGroupItem(detailGroup.value.id, { tagId, polarity, weight: 1.0 })
    detailItems.value.push(item)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function updateItem(row, weight, polarity) {
  try {
    const updated = await updateGroupItem(detailGroup.value.id, row.id, { weight, polarity })
    const idx = detailItems.value.findIndex((i) => i.id === updated.id)
    if (idx >= 0) detailItems.value[idx] = updated
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(`从词组中移除「${row.prompt}」吗？`, '移除条目', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteGroupItem(detailGroup.value.id, row.id)
    detailItems.value = detailItems.value.filter((i) => i.id !== row.id)
    ElMessage.success('已移除')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function onCopyPreview() {
  const parts = detailItems.value
    .filter((i) => i.polarity === 'pos')
    .map((i) => formatPrompt(i.prompt, i.weight))
  const text = parts.join(', ')
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success(`正向拼接已复制（${parts.length} 条）`),
    () => ElMessage.error('复制失败')
  )
}

function formatPrompt(prompt, weight) {
  if (!weight || Math.abs(weight - 1.0) < 0.001) return prompt
  return `(${prompt}:${weight.toFixed(1)})`
}

onMounted(async () => {
  await Promise.all([load(), loadLoras()])
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 6px;
}

.page-head .desc {
  margin: 0;
  font-size: 13px;
  color: #8b909a;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.table {
  width: 100%;
}

.group-name {
  font-weight: 600;
  margin-right: 8px;
}

.muted {
  color: #8b909a;
  font-size: 13px;
}

.detail-head {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}

.detail-tabs {
  margin-top: 4px;
}

.add-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.picker-search {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.prompt-text {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  color: #3b3f46;
}

.item-table {
  width: 100%;
  margin-bottom: 12px;
}

.detail-footer {
  margin-top: 12px;
  text-align: right;
}
</style>
