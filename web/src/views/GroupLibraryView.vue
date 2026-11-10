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
      <SearchBox
        v-model="keyword"
        :suggest="fetchGroupSuggestions"
        width="300px"
        placeholder="按名称搜索，回车确认"
        @search="load"
      />

      <el-segmented v-model="scopeSeg" :options="scopeOptions" @change="load" />

      <el-select
        v-model="categoryFilter"
        placeholder="全部类别"
        clearable
        style="width: 140px"
        @change="load"
      >
        <el-option
          v-for="item in groupCategories"
          :key="item.value"
          :value="item.value"
          :label="item.label"
        />
      </el-select>

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
      <el-table-column prop="name" label="名称" min-width="200" sortable>
        <template #default="{ row }">
          <span class="group-name">{{ row.name }}</span>
          <el-tag v-if="row.kind === 'global_negative'" type="danger" effect="plain" size="small">
            全局负面
          </el-tag>
          <el-tag v-if="row.incomingRefCount > 0" type="warning" effect="plain" size="small">
            被引用 {{ row.incomingRefCount }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="范围" width="100" align="center" sortable :sort-method="sortByScope">
        <template #default="{ row }">
          <el-tag size="small" :type="row.scope === 'global' ? 'info' : 'success'" effect="light">
            {{ row.scope === 'global' ? '全局' : row.loraName }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="itemCount" label="条目" width="80" align="center" sortable>
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.itemCount || 0 }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="category"
        label="类别"
        width="100"
        align="center"
        sortable
        :sort-method="sortByCategoryLabel"
      >
        <template #default="{ row }">
          <el-tag size="small" effect="plain" type="info">{{ categoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip sortable>
        <template #default="{ row }">
          <span class="muted">{{ row.note || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openImages(row)">图片</el-button>
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
        <el-form-item label="类别" prop="category">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option
              v-for="item in groupCategories"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
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

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="detailGroup?.name ? `${detailGroup.name} - 条目` : '词组详情'"
      size="680px"
      @closed="resetDetail"
    >
      <div v-if="detailGroup" class="detail-head">
        <el-tag size="small" :type="detailGroup.scope === 'global' ? 'info' : 'success'" effect="light">
          {{ detailGroup.scope === 'global' ? '全局' : (detailGroup.loraName || 'LoRA') }}
        </el-tag>
        <el-tag size="small" type="info" effect="plain">{{ categoryLabel(detailGroup.category) }}</el-tag>
        <span v-if="detailGroup.note" class="muted">{{ detailGroup.note }}</span>
      </div>

      <!-- 引用词组库管理区 -->
      <div class="ref-section">
        <div class="ref-section-header">
          <span class="ref-section-title">📎 引用的词组库</span>
          <el-button size="small" type="primary" plain @click="openAddRef">+ 添加引用</el-button>
        </div>
        <div v-if="detailRefs.length" class="ref-list">
          <div
            v-for="ref in detailRefs"
            :key="ref.id"
            class="ref-item"
          >
            <el-link class="ref-name" @click="navigateDetail(ref.refGroupId)">
              {{ ref.refGroupName }}
            </el-link>
            <el-tag v-if="ref.refItemCount != null" size="small" effect="plain" type="info">
              {{ ref.refItemCount }} 条
            </el-tag>
            <el-input-number
              v-model="ref.priority"
              :min="0"
              size="small"
              class="ref-priority"
              controls-position="right"
              @change="(v) => onUpdateRef(ref)"
            />
            <el-button
              link
              type="danger"
              size="small"
              @click="onRemoveRef(ref)"
            >
              移除
            </el-button>
          </div>
        </div>
        <div v-else class="muted ref-empty">
          还没有引用任何词组库。可以添加其他词组库来自动同步它们的条目。
        </div>
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

          <!-- 引用条目：按来源分组 -->
          <template v-if="groupedRefItems('pos').length">
            <div
              v-for="group in groupedRefItems('pos')"
              :key="group.sourceGroupId"
              class="inherit-block"
            >
              <div class="inherit-title">
                引用自
                <el-link type="primary" @click="navigateDetail(group.sourceGroupId)">
                  {{ group.sourceGroupName }}
                </el-link>
              </div>
              <el-table :data="group.items" border size="small" class="item-table">
                <el-table-column prop="prompt" label="提示词" min-width="180">
                  <template #default="{ row }">
                    <code class="prompt-text">{{ row.prompt }}</code>
                  </template>
                </el-table-column>
                <el-table-column prop="nameZh" label="中文" width="110">
                  <template #default="{ row }">
                    <span class="muted">{{ row.nameZh || '—' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="权重" width="70" align="center">
                  <template #default="{ row }">{{ row.weight }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
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

          <template v-if="groupedRefItems('neg').length">
            <div
              v-for="group in groupedRefItems('neg')"
              :key="group.sourceGroupId"
              class="inherit-block"
            >
              <div class="inherit-title">
                引用自
                <el-link type="primary" @click="navigateDetail(group.sourceGroupId)">
                  {{ group.sourceGroupName }}
                </el-link>
              </div>
              <el-table :data="group.items" border size="small" class="item-table">
                <el-table-column prop="prompt" label="提示词" min-width="180">
                  <template #default="{ row }">
                    <code class="prompt-text">{{ row.prompt }}</code>
                  </template>
                </el-table-column>
                <el-table-column prop="nameZh" label="中文" width="110">
                  <template #default="{ row }">
                    <span class="muted">{{ row.nameZh || '—' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="权重" width="70" align="center">
                  <template #default="{ row }">{{ row.weight }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="detail-footer">
        <span v-if="detailEffectiveItems.length !== (posItems.length + negItems.length)" class="muted inherit-hint">
          含引用条目，复制为合并后的生效内容（自身优先）
        </span>
        <el-button :icon="DocumentCopy" @click="copyPolarity('pos')">复制正向</el-button>
        <el-button :icon="DocumentCopy" @click="copyPolarity('neg')">复制负向</el-button>
      </div>
    </el-drawer>

    <!-- 参考图片 -->
    <el-dialog v-model="imageDialog.visible" :title="imageDialog.title" width="560px">
      <ImagePanel
        v-if="imageDialog.visible"
        ref-type="group"
        :ref-id="imageDialog.refId"
      />
    </el-dialog>

    <!-- 添加引用弹窗 -->
    <el-dialog v-model="addRefVisible" title="添加引用词组库" width="560px" append-to-body>
      <p class="muted">添加后会自动同步该词组库的条目；原库改动后这里会自动更新。</p>
      <SearchBox
        v-model="addRefKeyword"
        :suggest="fetchGroupSuggestions"
        placeholder="搜索要引用的词组名称"
        @search="loadAddRefCandidates"
      />
      <el-table
        v-loading="addRefLoading"
        :data="addRefCandidates"
        border
        size="small"
        height="280"
        highlight-current-row
        class="import-table"
        @current-change="(row) => (addRefSourceId = row ? row.id : null)"
      >
        <el-table-column prop="name" label="词组" min-width="200" />
        <el-table-column label="范围" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scope === 'global' ? 'info' : 'success'">
              {{ row.scope === 'global' ? '全局' : (row.loraName || 'LoRA') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类别" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的词组（范围需一致）" :image-size="60" />
        </template>
      </el-table>
      <template #footer>
        <el-button @click="addRefVisible = false">取消</el-button>
        <el-button type="primary" :loading="addRefSaving" :disabled="!addRefSourceId" @click="doAddRef">
          添加引用
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy, Plus } from '@element-plus/icons-vue'
import {
  addGroupRef,
  addGroupItem,
  createGroup,
  deleteGroup,
  deleteGroupItem,
  duplicateGroup,
  fetchGroupCategories,
  fetchGroupDetail,
  fetchGroups,
  fetchGroupSuggestions,
  removeGroupRef,
  updateGroup,
  updateGroupItem,
  updateGroupRef
} from '@/api/group'
import { fetchLoras } from '@/api/lora'
import AddTagRow from '@/components/AddTagRow.vue'
import ImagePanel from '@/components/ImagePanel.vue'
import SearchBox from '@/components/SearchBox.vue'

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
const groupCategories = ref([])
const categoryFilter = ref('')

const FALLBACK_CATEGORIES = [
  { value: 'character', label: '人物' },
  { value: 'outfit', label: '穿搭' },
  { value: 'scene', label: '场景' },
  { value: 'event', label: '事件' },
  { value: 'quality', label: '质量' },
  { value: 'style', label: '风格' },
  { value: 'action', label: '动作' },
  { value: 'expression', label: '表情' },
  { value: 'lighting', label: '光影' },
  { value: 'other', label: '其他' }
]

function categoryLabel(value) {
  const hit = groupCategories.value.find((c) => c.value === value)
  return hit ? hit.label : (value || '其他')
}

function sortByCategoryLabel(a, b) {
  return categoryLabel(a.category).localeCompare(categoryLabel(b.category), 'zh-CN')
}

function scopeLabel(row) {
  return row.scope === 'global' ? '全局' : (row.loraName || 'LoRA')
}

function sortByScope(a, b) {
  return scopeLabel(a).localeCompare(scopeLabel(b), 'zh-CN')
}

// --- 编辑抽屉 ---
const editVisible = ref(false)
const editMode = ref('create')
const saving = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  name: '',
  kind: 'normal',
  category: 'other',
  scope: 'global',
  loraId: null,
  note: ''
})
const editRules = {
  name: [{ required: true, message: '请输入词组名称', trigger: 'blur' }],
  loraId: [{ required: true, message: '请选择 LoRA', trigger: 'change' }]
}

// --- 详情抽屉 ---
const detailVisible = ref(false)
const detailGroup = ref(null)
const detailPolarity = ref('pos')
const detailItems = ref([])
const detailRefs = ref([])
const detailRefItems = ref([])
const detailEffectiveItems = ref([])
const detailIncomingRefs = ref([])

// --- 添加引用弹窗 ---
const addRefVisible = ref(false)
const addRefKeyword = ref('')
const addRefSourceId = ref(null)
const addRefCandidates = ref([])
const addRefLoading = ref(false)
const addRefSaving = ref(false)

const imageDialog = reactive({ visible: false, refId: null, title: '' })

function openImages(row) {
  imageDialog.refId = row.id
  imageDialog.title = `参考图片 · ${row.name}`
  imageDialog.visible = true
}

const posItems = computed(() => detailItems.value.filter((i) => i.polarity === 'pos'))
const negItems = computed(() => detailItems.value.filter((i) => i.polarity === 'neg'))
const posTagIds = computed(() => posItems.value.map((i) => i.tagId))
const negTagIds = computed(() => negItems.value.map((i) => i.tagId))

/** 引用条目按来源分组（去重：自身条目优先覆盖引用条目） */
function groupedRefItems(polarity) {
  const ownTagIds = new Set(
    (polarity === 'pos' ? posItems.value : negItems.value).map((i) => i.tagId)
  )
  const filtered = detailRefItems.value.filter(
    (i) => i.polarity === polarity && !ownTagIds.has(i.tagId)
  )
  const groups = []
  const seenSources = new Set()
  for (const item of filtered) {
    const key = item.sourceGroupId + '|' + polarity
    if (seenSources.has(key)) continue
    seenSources.add(key)
    // 把同一来源的条目分在一起
    groups.push({
      sourceGroupId: item.sourceGroupId,
      sourceGroupName: item.sourceGroupName,
      items: filtered.filter(
        (i) => i.sourceGroupId === item.sourceGroupId && i.polarity === polarity
      )
    })
  }
  return groups
}

async function load() {
  loading.value = true
  try {
    const scope = scopeSeg.value || null
    groups.value = await fetchGroups(
      scope,
      scope === 'lora' ? loraId.value : null,
      keyword.value,
      categoryFilter.value
    )
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadGroupCategories() {
  try {
    groupCategories.value = await fetchGroupCategories()
  } catch (e) {
    groupCategories.value = FALLBACK_CATEGORIES
  }
  if (!groupCategories.value || groupCategories.value.length === 0) {
    groupCategories.value = FALLBACK_CATEGORIES
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
  Object.assign(editForm, {
    id: null,
    name: '',
    kind: 'normal',
    category: 'other',
    scope: 'global',
    loraId: null,
    note: ''
  })
  editVisible.value = true
}

function openEdit(row) {
  editMode.value = 'edit'
  Object.assign(editForm, {
    id: row.id,
    name: row.name,
    kind: row.kind || 'normal',
    category: row.category || 'other',
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
    const payload = {
      name: editForm.name,
      kind: editForm.kind,
      category: editForm.category,
      scope: editForm.scope,
      loraId: editForm.scope === 'lora' ? editForm.loraId : null,
      note: editForm.note
    }
    if (editMode.value === 'create') {
      await createGroup(payload)
      ElMessage.success('已新增词组')
    } else {
      await updateGroup(editForm.id, payload)
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
    // 被引用时后端返回带引用明细的错误消息
    if (e.code === 1 && row.incomingRefCount > 0) {
      try {
        await ElMessageBox.alert(
          e.message,
          '无法删除：被其他词组引用',
          { type: 'error', confirmButtonText: '知道了' }
        )
      } catch (_) {}
    } else {
      ElMessage.error(e.message)
    }
  }
}

async function duplicate(row) {
  try {
    await duplicateGroup(row.id)
    ElMessage.success('已复制（仅复制自身条目，引用关系需手动添加）')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

// --- 详情 ---

async function openDetail(row) {
  detailVisible.value = true
  await navigateDetail(row.id)
}

async function navigateDetail(id) {
  try {
    const data = await fetchGroupDetail(id)
    detailGroup.value = data.group
    detailItems.value = data.items || []
    detailRefs.value = data.refs || []
    detailRefItems.value = data.refItems || []
    detailEffectiveItems.value = data.effectiveItems || []
    detailIncomingRefs.value = data.incomingRefs || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function resetDetail() {
  detailGroup.value = null
  detailItems.value = []
  detailRefs.value = []
  detailRefItems.value = []
  detailEffectiveItems.value = []
  detailIncomingRefs.value = []
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

/** 复制生效拼接结果：用后端返回的合并后 effectiveItems */
function copyPolarity(polarity) {
  const effective = detailEffectiveItems.value.filter((i) => i.polarity === polarity)
  const parts = effective.map((i) => formatPrompt(i.prompt, i.weight))
  const text = parts.join(', ')
  const label = polarity === 'pos' ? '正向' : '负向'
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success(`${label}拼接已复制（${parts.length} 条）`),
    () => ElMessage.error('复制失败')
  )
}

function formatPrompt(prompt, weight) {
  if (!weight || Math.abs(weight - 1.0) < 0.001) return prompt
  return `(${prompt}:${weight.toFixed(1)})`
}

// --- 引用管理 ---

function openAddRef() {
  addRefKeyword.value = ''
  addRefSourceId.value = null
  addRefCandidates.value = []
  addRefVisible.value = true
  loadAddRefCandidates()
}

async function loadAddRefCandidates() {
  addRefLoading.value = true
  try {
    const data = await fetchGroups(
      detailGroup.value.scope,
      detailGroup.value.scope === 'lora' ? detailGroup.value.loraId : null,
      addRefKeyword.value
    )
    // 排除自身和已引用的
    const existingRefIds = new Set(detailRefs.value.map((r) => r.refGroupId))
    addRefCandidates.value = data.filter(
      (g) => g.id !== detailGroup.value.id && !existingRefIds.has(g.id)
    )
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    addRefLoading.value = false
  }
}

async function doAddRef() {
  if (!addRefSourceId.value) return
  addRefSaving.value = true
  try {
    await addGroupRef(detailGroup.value.id, addRefSourceId.value)
    ElMessage.success('已添加引用')
    addRefVisible.value = false
    await navigateDetail(detailGroup.value.id)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    addRefSaving.value = false
  }
}

async function onUpdateRef(ref) {
  try {
    await updateGroupRef(detailGroup.value.id, ref.id, ref.priority, ref.sortOrder)
  } catch (e) {
    ElMessage.error(e.message)
    await navigateDetail(detailGroup.value.id)
  }
}

async function onRemoveRef(ref) {
  try {
    await ElMessageBox.confirm(
      `移除对「${ref.refGroupName}」的引用吗？`,
      '移除引用',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await removeGroupRef(detailGroup.value.id, ref.id)
    ElMessage.success('已移除引用')
    await navigateDetail(detailGroup.value.id)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await Promise.all([load(), loadLoras(), loadGroupCategories()])
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
  margin-bottom: 12px;
  align-items: center;
}

.ref-section {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f7f8fa;
  border: 1px solid #e8e9ec;
}

.ref-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ref-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #5c6270;
}

.ref-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ref-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 4px;
  background: #fff;
  border: 1px solid #e8e9ec;
}

.ref-name {
  flex: 1;
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ref-priority {
  width: 80px;
}

.ref-empty {
  padding: 6px 0;
}

.detail-tabs {
  margin-top: 4px;
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

.inherit-block {
  margin-bottom: 12px;
}

.inherit-title {
  font-size: 12px;
  color: #8b909a;
  margin-bottom: 6px;
}

.detail-footer {
  margin-top: 12px;
  text-align: right;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.inherit-hint {
  margin-right: 4px;
}
</style>
