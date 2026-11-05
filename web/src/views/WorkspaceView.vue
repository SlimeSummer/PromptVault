<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>编辑区</h1>
        <p class="desc">
          分区不写死：跟随设置页的「词条分类」自动生成，新增 / 改名 / 排序都会同步。支持拖拽排序、权重调整与正负向分开导出。
        </p>
      </div>
      <div class="head-actions">
        <el-select
          v-model="currentId"
          placeholder="选择编辑区"
          style="width: 200px"
          @change="loadDetail"
        >
          <el-option
            v-for="item in workspaces"
            :key="item.id"
            :value="item.id"
            :label="item.name"
          />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建编辑区</el-button>
      </div>
    </header>

    <el-empty v-if="!currentId" description="还没有编辑区，先新建一个吧" :image-size="90">
      <el-button type="primary" @click="openCreate">新建编辑区</el-button>
    </el-empty>

    <template v-else>
      <section v-if="detail.workspace" class="meta-bar">
        <div class="meta-main">
          <span class="ws-name">{{ detail.workspace.name }}</span>
          <el-tag v-if="detail.workspace.loraName" size="small" type="success" effect="light">
            {{ detail.workspace.loraName }}
          </el-tag>
          <el-select
            v-model="presetId"
            placeholder="不使用预设"
            clearable
            size="small"
            style="width: 180px"
            :disabled="!detail.workspace.loraId"
            @change="onPresetChange"
          >
            <el-option
              v-for="item in presets"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
          <span v-if="detail.workspace.note" class="muted">{{ detail.workspace.note }}</span>
        </div>
        <div class="meta-actions">
          <el-button size="small" @click="openEdit">编辑</el-button>
          <el-button size="small" @click="openAddTag">+ 添加标签</el-button>
          <el-button size="small" type="primary" plain @click="openAddGroup">+ 添加词组</el-button>
          <el-button size="small" @click="openSaveArchive">保存存档</el-button>
          <el-button size="small" @click="openArchives">读取存档</el-button>
          <el-button size="small" @click="previewVisible = true">预览</el-button>
          <el-button size="small" @click="copyText(detail.positiveText, '正向提示词')">复制正向</el-button>
          <el-button size="small" @click="copyText(detail.negativeText, '负向提示词')">复制负向</el-button>
          <el-button size="small" type="danger" plain @click="removeWorkspace">删除</el-button>
        </div>
      </section>

      <div class="stats">
        <span>正向 {{ detail.positiveCount }} 条</span>
        <span>负向 {{ detail.negativeCount }} 条</span>
        <span class="muted">负向 = 全局负面词组 → 预设 → 编辑区，自动去重</span>
      </div>

      <div v-loading="loading" class="sections">
        <section
          v-for="section in detail.sections"
          :key="section.value"
          class="section-card"
          @dragover.prevent
          @drop="onDropSection(section, $event)"
        >
          <header class="section-head">
            <span class="section-title">{{ section.label }}</span>
            <el-tag size="small" effect="plain">{{ section.count }}</el-tag>
          </header>
          <div class="block-list">
            <div v-if="!section.blocks.length" class="empty-hint">暂无内容</div>
            <article
              v-for="block in section.blocks"
              :key="block.key"
              class="block"
              :class="{ locked: block.locked }"
              :draggable="!block.locked"
              @dragstart="onDragStart(section, block, $event)"
              @dragend="onDragEnd"
              @dragover.prevent.stop
              @drop.stop="onDropBlock(section, block, $event)"
            >
              <div class="block-head">
                <span v-if="!block.locked" class="handle">≡</span>
                <span class="block-source">
                  {{ block.itemType === 'preset' ? block.sourceName : (block.groupName || '词条') }}
                </span>
                <el-tag v-if="block.itemType === 'group'" size="small" type="warning" effect="plain">
                  词组
                </el-tag>
                <span class="spacer"></span>
                <el-button v-if="block.unbindable" link size="small" @click="unbind(block)">解绑</el-button>
                <el-button v-if="!block.locked" link type="danger" size="small" @click="removeBlock(block)">
                  移除
                </el-button>
              </div>
              <div class="tag-lines">
                <div
                  v-for="(tag, index) in block.tags"
                  :key="`${block.key}-${tag.tagId}-${index}`"
                  class="tag-line"
                >
                  <code>{{ tag.prompt }}</code>
                  <span v-if="tag.nameZh" class="zh">{{ tag.nameZh }}</span>
                  <el-input-number
                    v-if="block.itemType === 'tag'"
                    v-model="tag.weight"
                    :min="0.1"
                    :max="2"
                    :step="0.1"
                    :precision="2"
                    size="small"
                    class="weight"
                    @change="onWeightChange(block, tag)"
                  />
                  <span v-else-if="tag.weight !== 1" class="weight-text">×{{ tag.weight }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>

      <section class="negative-card">
        <header class="section-head">
          <span class="section-title">负向</span>
          <el-tag size="small" type="danger" effect="plain">{{ detail.negativeCount }}</el-tag>
        </header>
        <div v-if="detail.negative.length" class="negative-list">
          <div v-for="(item, index) in detail.negative" :key="index" class="negative-line">
            <el-tag size="small" type="danger" effect="plain">{{ item.source }}</el-tag>
            <code>{{ item.prompt }}</code>
            <span v-if="item.nameZh" class="zh">{{ item.nameZh }}</span>
            <span v-if="item.weight !== 1" class="weight-text">×{{ item.weight }}</span>
            <span class="spacer"></span>
            <el-button
              v-if="item.removable"
              link
              type="danger"
              size="small"
              @click="removeNegative(item)"
            >
              移除
            </el-button>
          </div>
        </div>
        <div v-else class="empty-hint">暂无负向内容</div>
      </section>
    </template>

    <!-- 新建 / 编辑编辑区 -->
    <el-dialog
      v-model="wsDialog.visible"
      :title="wsDialog.mode === 'create' ? '新建编辑区' : '编辑编辑区'"
      width="460px"
    >
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="wsDialog.form.name" maxlength="100" placeholder="例如：赛博夜街 · 全身" />
        </el-form-item>
        <el-form-item label="LoRA">
          <el-select
            v-model="wsDialog.form.loraId"
            clearable
            placeholder="不绑定 LoRA（仅全局内容）"
            style="width: 100%"
            @change="onFormLoraChange"
          >
            <el-option
              v-for="item in loras"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预设">
          <el-select
            v-model="wsDialog.form.presetId"
            clearable
            placeholder="不使用预设"
            style="width: 100%"
            :disabled="!wsDialog.form.loraId"
          >
            <el-option
              v-for="item in formPresets"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="wsDialog.form.note" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wsDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitWorkspace">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加标签 -->
    <el-dialog v-model="tagDialog.visible" title="添加标签" width="640px">
      <div class="dialog-toolbar">
        <SearchBox
          v-model="tagDialog.keyword"
          :suggest="suggestTags"
          width="320px"
          placeholder="搜索提示词 / 中文，回车确认"
          @search="searchTags"
        />
        <el-radio-group v-model="tagDialog.polarity" size="small">
          <el-radio-button label="pos">正向</el-radio-button>
          <el-radio-button label="neg">负向</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="tagDialog.tags" size="small" max-height="380" border>
        <el-table-column prop="prompt" label="提示词" min-width="220">
          <template #default="{ row }">
            <code class="prompt-text">{{ row.prompt }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="nameZh" label="中文" width="120">
          <template #default="{ row }">
            <span class="muted">{{ row.nameZh || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">{{ tagCategoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="addTag(row)">添加</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">没有匹配的词条</span>
        </template>
      </el-table>
    </el-dialog>

    <!-- 添加词组 -->
    <el-dialog v-model="groupDialog.visible" title="添加词组" width="700px">
      <div class="dialog-toolbar">
        <SearchBox
          v-model="groupDialog.keyword"
          :suggest="() => []"
          width="320px"
          placeholder="按名称搜索词组，回车确认"
          @search="searchGroups"
        />
        <span class="muted">只显示全局词组与当前 LoRA 的词组</span>
      </div>
      <el-table :data="groupDialog.groups" size="small" max-height="380" border>
        <el-table-column prop="name" label="词组" min-width="180" />
        <el-table-column label="范围" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scope === 'global' ? 'info' : 'success'" effect="light">
              {{ row.scope === 'global' ? '全局' : (row.loraName || 'LoRA') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类别" width="90" align="center">
          <template #default="{ row }">{{ groupCategoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="条目" width="70" align="center" />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="addGroup(row)">添加</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">没有匹配的词组</span>
        </template>
      </el-table>
    </el-dialog>

    <!-- 预览 / 复制 -->
    <el-dialog v-model="previewVisible" title="拼接预览" width="720px">
      <div class="preview-block">
        <div class="preview-head">
          <span>正向提示词</span>
          <el-button size="small" @click="copyText(detail.positiveText, '正向提示词')">复制</el-button>
        </div>
        <el-input type="textarea" :rows="5" readonly :model-value="detail.positiveText" />
      </div>
      <div class="preview-block">
        <div class="preview-head">
          <span>负向提示词</span>
          <el-button size="small" @click="copyText(detail.negativeText, '负向提示词')">复制</el-button>
        </div>
        <el-input type="textarea" :rows="4" readonly :model-value="detail.negativeText" />
      </div>
    </el-dialog>

    <!-- 保存存档 -->
    <el-dialog v-model="saveArchiveDialog.visible" title="保存存档" width="460px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input
            v-model="saveArchiveDialog.name"
            maxlength="100"
            placeholder="例如：赛博夜街 · 版本 A"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="saveArchiveDialog.note"
            type="textarea"
            :rows="3"
            placeholder="可选，例如这次改了什么"
          />
        </el-form-item>
      </el-form>
      <p class="tip">存档独立保存，不会新增编辑区；之后可在「读取存档」里恢复或导出 JSON。</p>
      <template #footer>
        <el-button @click="saveArchiveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saveArchiveDialog.saving" @click="submitSaveArchive">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 读取存档 / 导入导出 -->
    <el-dialog v-model="archiveDialog.visible" title="读取存档" width="900px">
      <div class="dialog-toolbar">
        <el-button size="small" :icon="Upload" @click="pickImportFile">导入 JSON</el-button>
        <el-button size="small" :icon="Refresh" @click="loadArchives">刷新</el-button>
        <input
          ref="importInput"
          type="file"
          accept=".json,application/json"
          style="display: none"
          @change="onImportFile"
        />
        <span class="muted">存档与编辑区分离管理，数量再多也不会挤爆下拉框</span>
      </div>
      <el-table :data="archiveDialog.list" size="small" border max-height="420">
        <el-table-column prop="name" label="存档" min-width="180" />
        <el-table-column prop="sourceWorkspaceName" label="来源编辑区" width="130" />
        <el-table-column prop="itemCount" label="条目" width="70" align="center" />
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="保存时间" width="160" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="doRestoreArchive(row)">读取</el-button>
            <el-button link @click="exportArchive(row)">导出</el-button>
            <el-button link type="danger" @click="removeArchive(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">暂无存档，先点「保存存档」</span>
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Upload } from '@element-plus/icons-vue'
import SearchBox from '@/components/SearchBox.vue'
import {
  addWorkspaceItem,
  createWorkspace,
  deleteArchive,
  deleteWorkspace,
  deleteWorkspaceItem,
  fetchArchiveDetail,
  fetchArchives,
  fetchWorkspaceDetail,
  fetchWorkspaces,
  importArchive,
  reorderWorkspaceItems,
  restoreArchive,
  saveArchive,
  unbindWorkspaceGroup,
  updateWorkspace,
  updateWorkspaceItem
} from '@/api/workspace'
import { fetchLoraPresets, fetchLoras } from '@/api/lora'
import { fetchGroupCategories, fetchGroups } from '@/api/group'
import { fetchTagCategories } from '@/api/setting'
import { fetchTags, fetchTagSuggestions } from '@/api/tag'

const workspaces = ref([])
const currentId = ref(null)
const loading = ref(false)
const saving = ref(false)
const loras = ref([])
const presets = ref([])
const presetId = ref(null)
const tagCategories = ref([])
const groupCategories = ref([])
const previewVisible = ref(false)
const importInput = ref(null)

const detail = reactive({
  workspace: null,
  sections: [],
  negative: [],
  positiveCount: 0,
  negativeCount: 0,
  positiveText: '',
  negativeText: ''
})

const wsDialog = reactive({
  visible: false,
  mode: 'create',
  form: { name: '', loraId: null, presetId: null, note: '' }
})
const formPresets = ref([])

const tagDialog = reactive({ visible: false, keyword: '', polarity: 'pos', tags: [] })
const groupDialog = reactive({ visible: false, keyword: '', groups: [] })

const saveArchiveDialog = reactive({ visible: false, name: '', note: '', saving: false })
const archiveDialog = reactive({ visible: false, list: [] })

const dragState = reactive({ section: '', itemId: null })

onMounted(async () => {
  await Promise.all([loadLoras(), loadTagCategories(), loadGroupCategories()])
  await loadWorkspaces()
})

async function loadLoras() {
  try {
    loras.value = (await fetchLoras()) || []
  } catch (e) {
    loras.value = []
  }
}

async function loadTagCategories() {
  try {
    tagCategories.value = (await fetchTagCategories()) || []
  } catch (e) {
    tagCategories.value = []
  }
}

async function loadGroupCategories() {
  try {
    groupCategories.value = (await fetchGroupCategories()) || []
  } catch (e) {
    groupCategories.value = []
  }
}

async function loadWorkspaces(selectId = null) {
  try {
    workspaces.value = (await fetchWorkspaces()) || []
    if (selectId != null) {
      currentId.value = selectId
    }
    if (!workspaces.value.some((item) => item.id === currentId.value)) {
      currentId.value = workspaces.value.length ? workspaces.value[0].id : null
    }
    if (currentId.value) {
      await loadDetail()
    } else {
      resetDetail()
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadDetail() {
  if (!currentId.value) {
    resetDetail()
    return
  }
  loading.value = true
  try {
    const data = await fetchWorkspaceDetail(currentId.value)
    detail.workspace = data.workspace
    detail.sections = data.sections || []
    detail.negative = data.negative || []
    detail.positiveCount = data.positiveCount || 0
    detail.negativeCount = data.negativeCount || 0
    detail.positiveText = data.positiveText || ''
    detail.negativeText = data.negativeText || ''
    presetId.value = data.workspace?.presetId || null
    await loadPresets(data.workspace?.loraId)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function resetDetail() {
  detail.workspace = null
  detail.sections = []
  detail.negative = []
  detail.positiveCount = 0
  detail.negativeCount = 0
  detail.positiveText = ''
  detail.negativeText = ''
  presets.value = []
  presetId.value = null
}

async function loadPresets(loraId) {
  if (!loraId) {
    presets.value = []
    return
  }
  try {
    presets.value = (await fetchLoraPresets(loraId)) || []
  } catch (e) {
    presets.value = []
  }
}

function openCreate() {
  wsDialog.mode = 'create'
  wsDialog.form = { name: '', loraId: null, presetId: null, note: '' }
  formPresets.value = []
  wsDialog.visible = true
}

async function openEdit() {
  if (!detail.workspace) {
    return
  }
  const ws = detail.workspace
  wsDialog.mode = 'edit'
  wsDialog.form = {
    name: ws.name,
    loraId: ws.loraId || null,
    presetId: ws.presetId || null,
    note: ws.note || ''
  }
  await loadFormPresets(wsDialog.form.loraId)
  wsDialog.visible = true
}

async function loadFormPresets(loraId) {
  if (!loraId) {
    formPresets.value = []
    return
  }
  try {
    formPresets.value = (await fetchLoraPresets(loraId)) || []
  } catch (e) {
    formPresets.value = []
  }
}

function onFormLoraChange(value) {
  wsDialog.form.presetId = null
  loadFormPresets(value)
}

async function submitWorkspace() {
  if (!wsDialog.form.name.trim()) {
    ElMessage.warning('请填写编辑区名称')
    return
  }
  saving.value = true
  try {
    if (wsDialog.mode === 'create') {
      const created = await createWorkspace({ ...wsDialog.form })
      ElMessage.success('编辑区已创建')
      wsDialog.visible = false
      await loadWorkspaces(created.id)
    } else {
      await updateWorkspace(currentId.value, { ...wsDialog.form })
      ElMessage.success('编辑区已保存')
      wsDialog.visible = false
      await loadWorkspaces(currentId.value)
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function onPresetChange(value) {
  const ws = detail.workspace
  if (!ws) {
    return
  }
  try {
    await updateWorkspace(currentId.value, {
      name: ws.name,
      loraId: ws.loraId,
      presetId: value || null,
      note: ws.note
    })
    await loadWorkspaces(currentId.value)
  } catch (e) {
    ElMessage.error(e.message)
    await loadDetail()
  }
}

async function removeWorkspace() {
  const ws = detail.workspace
  if (!ws) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除编辑区「${ws.name}」？只删除组装结果，不影响词库和词组库。`,
      '删除编辑区',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteWorkspace(currentId.value)
    ElMessage.success('编辑区已删除')
    currentId.value = null
    await loadWorkspaces()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function openAddTag() {
  tagDialog.keyword = ''
  tagDialog.polarity = 'pos'
  tagDialog.tags = []
  tagDialog.visible = true
  await searchTags()
}

async function searchTags() {
  try {
    const data = await fetchTags({ keyword: tagDialog.keyword, page: 1, size: 20 })
    tagDialog.tags = data.list || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function suggestTags(keyword) {
  return fetchTagSuggestions(keyword).then((list) =>
    (list || []).map((item) => ({
      value: item.prompt,
      sub: item.nameZh || item.category || ''
    }))
  )
}

async function addTag(row) {
  try {
    await addWorkspaceItem(currentId.value, {
      itemType: 'tag',
      refId: row.id,
      polarity: tagDialog.polarity,
      weight: 1.0
    })
    ElMessage.success(`已添加「${row.prompt}」`)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function openAddGroup() {
  groupDialog.keyword = ''
  groupDialog.groups = []
  groupDialog.visible = true
  await searchGroups()
}

async function searchGroups() {
  try {
    const loraId = detail.workspace?.loraId
    const keyword = groupDialog.keyword
    const globals = (await fetchGroups('global', null, keyword)) || []
    const scoped = loraId ? ((await fetchGroups('lora', loraId, keyword)) || []) : []
    groupDialog.groups = [...globals, ...scoped]
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function addGroup(row) {
  try {
    await addWorkspaceItem(currentId.value, { itemType: 'group', refId: row.id })
    ElMessage.success(`已添加词组「${row.name}」`)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function onWeightChange(block, tag) {
  try {
    await updateWorkspaceItem(currentId.value, block.workspaceItemId, {
      polarity: block.polarity,
      weight: tag.weight,
      sectionOverride: block.sectionOverride
    })
  } catch (e) {
    ElMessage.error(e.message)
    await loadDetail()
  }
}

async function removeBlock(block) {
  const label = block.groupName || block.tags?.[0]?.prompt || '该条目'
  try {
    await ElMessageBox.confirm(`从编辑区移除「${label}」？`, '移除条目', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteWorkspaceItem(currentId.value, block.workspaceItemId)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function unbind(block) {
  try {
    await ElMessageBox.confirm(
      `解绑词组「${block.groupName}」？会把引用链里的条目拆成独立词条，之后可单独调权重。`,
      '解绑词组',
      { type: 'info' }
    )
  } catch (e) {
    return
  }
  try {
    const result = await unbindWorkspaceGroup(currentId.value, block.workspaceItemId)
    ElMessage.success(`已解绑：新增 ${result.added} 条，跳过 ${result.skipped} 条`)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function onDragStart(section, block, event) {
  if (block.locked || !block.workspaceItemId) {
    event.preventDefault()
    return
  }
  dragState.section = section.value
  dragState.itemId = block.workspaceItemId
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(block.workspaceItemId))
}

function onDragEnd() {
  dragState.section = ''
  dragState.itemId = null
}

function onDropBlock(section, block, event) {
  event.preventDefault()
  if (block.locked || dragState.section !== section.value || !dragState.itemId) {
    return
  }
  if (block.workspaceItemId === dragState.itemId) {
    return
  }
  commitDrop(section, block.workspaceItemId)
}

function onDropSection(section, event) {
  event.preventDefault()
  if (dragState.section !== section.value || !dragState.itemId) {
    return
  }
  commitDrop(section, null)
}

async function commitDrop(section, targetId) {
  const ids = section.blocks
    .filter((block) => !block.locked && block.workspaceItemId)
    .map((block) => block.workspaceItemId)
  const from = ids.indexOf(dragState.itemId)
  if (from < 0) {
    return
  }
  const moved = dragState.itemId
  ids.splice(from, 1)
  if (targetId != null) {
    let to = ids.indexOf(targetId)
    if (to < 0) {
      to = ids.length
    }
    ids.splice(to, 0, moved)
  } else {
    ids.push(moved)
  }
  onDragEnd()
  try {
    await reorderWorkspaceItems(currentId.value, ids)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
    await loadDetail()
  }
}

async function copyText(text, label) {
  if (!text) {
    ElMessage.warning(`${label}为空`)
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label}`)
  } catch (e) {
    ElMessage.error('复制失败，请打开预览手动复制')
  }
}

// --- 存档 ---

function archiveDefaultName() {
  const ws = detail.workspace
  const pad = (value) => String(value).padStart(2, '0')
  const now = new Date()
  const stamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}-${pad(now.getMinutes())}`
  const base = ws?.name || '编辑区'
  return `${base} · ${stamp}`.slice(0, 100)
}

function openSaveArchive() {
  if (!detail.workspace) {
    ElMessage.warning('请先选择编辑区')
    return
  }
  saveArchiveDialog.name = archiveDefaultName()
  saveArchiveDialog.note = detail.workspace.note || ''
  saveArchiveDialog.visible = true
}

async function submitSaveArchive() {
  const name = saveArchiveDialog.name.trim()
  if (!name) {
    ElMessage.warning('请填写存档名称')
    return
  }
  saveArchiveDialog.saving = true
  try {
    await saveArchive({
      workspaceId: currentId.value,
      name,
      note: saveArchiveDialog.note
    })
    ElMessage.success('已保存存档')
    saveArchiveDialog.visible = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saveArchiveDialog.saving = false
  }
}

async function openArchives() {
  archiveDialog.visible = true
  await loadArchives()
}

async function loadArchives() {
  try {
    archiveDialog.list = (await fetchArchives()) || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doRestoreArchive(row) {
  if (!currentId.value) {
    ElMessage.warning('请先选择要读取到的编辑区')
    return
  }
  try {
    await ElMessageBox.confirm(
      `读取存档「${row.name}」会覆盖当前编辑区的 LoRA / 预设与全部条目，确定继续？`,
      '读取存档',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const result = await restoreArchive(row.id, currentId.value)
    ElMessage.success(`已读取存档：恢复 ${result.added} 条，跳过 ${result.skipped} 条`)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function exportArchive(row) {
  try {
    const data = await fetchArchiveDetail(row.id)
    const payload = data.payload || data
    const blob = new Blob([JSON.stringify(payload, null, 2)], {
      type: 'application/json;charset=utf-8'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${String(row.name || 'archive').replace(/[\\/:*?"<>|]/g, '_')}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('已导出 JSON 存档')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function pickImportFile() {
  importInput.value?.click()
}

async function onImportFile(event) {
  const file = event.target.files && event.target.files[0]
  if (!file) {
    return
  }
  try {
    const text = await file.text()
    const payload = JSON.parse(text)
    const created = await importArchive(payload)
    ElMessage.success(`已导入存档「${created.name}」，可在列表中读取`)
    await loadArchives()
  } catch (e) {
    ElMessage.error(e.message || '导入失败：JSON 格式不正确')
  } finally {
    event.target.value = ''
  }
}

async function removeArchive(row) {
  try {
    await ElMessageBox.confirm(`删除存档「${row.name}」？不会影响编辑区与词库。`, '删除存档', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteArchive(row.id)
    ElMessage.success('存档已删除')
    await loadArchives()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

// --- 负向 ---

async function removeNegative(item) {
  try {
    await ElMessageBox.confirm(`从编辑区移除负向「${item.prompt}」？`, '移除负向条目', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteWorkspaceItem(currentId.value, item.workspaceItemId)
    ElMessage.success('已移除')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function tagCategoryLabel(value) {
  const hit = tagCategories.value.find((item) => item.value === value)
  return hit ? hit.label : (value || '—')
}

function groupCategoryLabel(value) {
  const hit = groupCategories.value.find((item) => item.value === value)
  return hit ? hit.label : (value || '—')
}
</script>

<style scoped>
.page {
  max-width: 1280px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
}

.desc {
  margin: 0;
  max-width: 720px;
  color: #8a8f99;
  font-size: 13px;
  line-height: 1.6;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(20, 24, 31, 0.06);
}

.meta-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ws-name {
  font-size: 16px;
  font-weight: 600;
}

.meta-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.stats {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 2px;
  font-size: 13px;
  color: #4b515b;
}

.sections {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
  align-items: start;
}

.section-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 3px rgba(20, 24, 31, 0.06);
  min-height: 96px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f1f4;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
}

.block-list {
  min-height: 40px;
}

.block {
  border: 1px solid #eceef2;
  border-radius: 8px;
  padding: 8px;
  margin-bottom: 8px;
  background: #fafbfc;
  cursor: grab;
}

.block:last-child {
  margin-bottom: 0;
}

.block.locked {
  cursor: default;
  background: #f7f8fa;
  border-style: dashed;
}

.block-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.handle {
  color: #a0a5ae;
  font-size: 13px;
  letter-spacing: 1px;
}

.block-source {
  font-size: 12px;
  color: #5c626c;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spacer {
  flex: 1;
}

.tag-lines {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tag-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

code {
  padding: 1px 6px;
  border-radius: 4px;
  background: #f1f2f5;
  font-size: 12px;
  color: #303540;
  word-break: break-all;
}

.zh {
  font-size: 12px;
  color: #909399;
}

.weight {
  width: 92px;
}

.weight-text {
  font-size: 12px;
  color: #909399;
}

.empty-hint {
  padding: 12px 0;
  text-align: center;
  font-size: 12px;
  color: #a8adb7;
}

.negative-card {
  margin-top: 16px;
  background: #fff;
  border-radius: 10px;
  padding: 12px 16px;
  border-left: 3px solid #f56c6c;
  box-shadow: 0 1px 3px rgba(20, 24, 31, 0.06);
}

.negative-list {
  display: flex;
  flex-direction: column;
}

.negative-line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px dashed #f2f2f5;
  flex-wrap: wrap;
}

.negative-line:last-child {
  border-bottom: none;
}

.dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.prompt-text {
  font-size: 12px;
}

.preview-block {
  margin-bottom: 16px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: #4b515b;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
