<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>设置</h1>
        <p class="desc">分类与底模都从这里维护，保存后即作用于全局词库和 LoRA 档案。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
      </div>
    </header>

    <el-card v-loading="loading" class="panel" shadow="never">
      <template #header>
        <div class="panel-head">
          <div>
            <h2>词条分类</h2>
            <p>词库里的“分类”下拉由这里驱动。显示名称可改；标识用于数据存储，创建后不可修改。</p>
          </div>
          <el-button size="small" :icon="Plus" @click="addCategory">新增分类</el-button>
        </div>
      </template>

      <el-table :data="categories" size="small" class="inner-table">
        <el-table-column label="显示名称" min-width="160">
          <template #default="{ row, $index }">
            <el-input v-model="row.label" size="small" maxlength="20" placeholder="如 发型" />
          </template>
        </el-table-column>

        <el-table-column label="标识" min-width="150">
          <template #default="{ row }">
            <el-input
              v-model="row.value"
              size="small"
              :disabled="!row.isNew"
              placeholder="字母/数字/下划线"
            />
          </template>
        </el-table-column>

        <el-table-column label="词条数" width="90" align="center">
          <template #default="{ row }">
            <span class="muted">{{ countOf(row.value) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="right">
          <template #default="{ row, $index }">
            <el-button link :disabled="$index === 0" @click="moveCategory($index, -1)">上移</el-button>
            <el-button
              link
              :disabled="$index === categories.length - 1"
              @click="moveCategory($index, 1)"
            >
              下移
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="countOf(row.value) > 0"
              @click="removeCategory($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="至少保留一个分类" :image-size="60" />
        </template>
      </el-table>

      <p class="tip">
        被词条使用的分类不能删除（删除按钮会置灰）；需要先到词库把这些词条改到其他分类。
      </p>
    </el-card>

    <el-card v-loading="loading" class="panel" shadow="never">
      <template #header>
        <div class="panel-head">
          <div>
            <h2>词组分类</h2>
            <p>词组库里的“类别”下拉由这里驱动。显示名称可改；标识用于数据存储，创建后不可修改。</p>
          </div>
          <el-button size="small" :icon="Plus" @click="addGroupCategory">新增分类</el-button>
        </div>
      </template>

      <el-table :data="groupCategories" size="small" class="inner-table">
        <el-table-column label="显示名称" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.label" size="small" maxlength="20" placeholder="如 场景" />
          </template>
        </el-table-column>

        <el-table-column label="标识" min-width="150">
          <template #default="{ row }">
            <el-input
              v-model="row.value"
              size="small"
              :disabled="!row.isNew"
              placeholder="字母/数字/下划线"
            />
          </template>
        </el-table-column>

        <el-table-column label="词组数" width="90" align="center">
          <template #default="{ row }">
            <span class="muted">{{ groupCountOf(row.value) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="right">
          <template #default="{ row, $index }">
            <el-button link :disabled="$index === 0" @click="moveGroupCategory($index, -1)">上移</el-button>
            <el-button
              link
              :disabled="$index === groupCategories.length - 1"
              @click="moveGroupCategory($index, 1)"
            >
              下移
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="groupCountOf(row.value) > 0"
              @click="removeGroupCategory($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="至少保留一个分类" :image-size="60" />
        </template>
      </el-table>

      <p class="tip">
        被词组使用的分类不能删除（删除按钮会置灰）；需要先到词组库把这些词组改到其他类别。
      </p>
    </el-card>

    <el-card v-loading="loading" class="panel" shadow="never">
      <template #header>
        <div class="panel-head">
          <div>
            <h2>LoRA 底模</h2>
            <p>LoRA 档案里的“底模”下拉选项。移除某个底模不会修改已保存的 LoRA 档案。</p>
          </div>
        </div>
      </template>

      <div class="chip-list">
        <el-tag
          v-for="(model, index) in baseModels"
          :key="model"
          closable
          effect="plain"
          @close="removeBaseModel(index)"
        >
          {{ model }}
        </el-tag>
        <span v-if="!baseModels.length" class="muted">至少保留一个底模选项</span>
      </div>

      <div class="add-row">
        <el-input
          v-model="newBaseModel"
          size="small"
          maxlength="40"
          placeholder="如 anima"
          style="width: 220px"
          @keyup.enter="addBaseModel"
        />
        <el-button size="small" @click="addBaseModel">添加</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { fetchTagStats } from '@/api/tag'
import { fetchGroups } from '@/api/group'
import { fetchSettings, saveSettings } from '@/api/setting'

const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const groupCategories = ref([])
const baseModels = ref([])
const newBaseModel = ref('')
const counts = ref({})
const groupCounts = ref({})

function countOf(value) {
  return counts.value[value] || 0
}

function groupCountOf(value) {
  return groupCounts.value[value] || 0
}

async function load() {
  loading.value = true
  try {
    const data = await fetchSettings()
    categories.value = (data.tagCategories || []).map((item) => ({
      value: item.value,
      label: item.label,
      isNew: false
    }))
    groupCategories.value = (data.groupCategories || []).map((item) => ({
      value: item.value,
      label: item.label,
      isNew: false
    }))
    baseModels.value = [...(data.loraBaseModels || [])]
    const [stats, groups] = await Promise.all([fetchTagStats(), fetchGroups()])
    counts.value = stats.categories || {}
    const nextGroupCounts = {}
    for (const group of groups || []) {
      const key = group.category || 'other'
      nextGroupCounts[key] = (nextGroupCounts[key] || 0) + 1
    }
    groupCounts.value = nextGroupCounts
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function addCategory() {
  categories.value.push({ value: '', label: '', isNew: true })
}

function moveCategory(index, delta) {
  const target = index + delta
  if (target < 0 || target >= categories.value.length) {
    return
  }
  const temp = categories.value[index]
  categories.value[index] = categories.value[target]
  categories.value[target] = temp
}

function removeCategory(index) {
  const row = categories.value[index]
  if (countOf(row.value) > 0) {
    ElMessage.warning(`分类「${row.label || row.value}」下还有 ${countOf(row.value)} 条词条，请先移动这些词条`)
    return
  }
  categories.value.splice(index, 1)
}

function addGroupCategory() {
  groupCategories.value.push({ value: '', label: '', isNew: true })
}

function moveGroupCategory(index, delta) {
  const target = index + delta
  if (target < 0 || target >= groupCategories.value.length) {
    return
  }
  const temp = groupCategories.value[index]
  groupCategories.value[index] = groupCategories.value[target]
  groupCategories.value[target] = temp
}

function removeGroupCategory(index) {
  const row = groupCategories.value[index]
  if (groupCountOf(row.value) > 0) {
    ElMessage.warning(`分类「${row.label || row.value}」下还有 ${groupCountOf(row.value)} 个词组，请先移动这些词组`)
    return
  }
  groupCategories.value.splice(index, 1)
}

function addBaseModel() {
  const value = newBaseModel.value.trim()
  if (!value) {
    ElMessage.warning('请输入底模名称')
    return
  }
  if (baseModels.value.includes(value)) {
    ElMessage.warning('该底模已存在')
    return
  }
  baseModels.value.push(value)
  newBaseModel.value = ''
}

function removeBaseModel(index) {
  baseModels.value.splice(index, 1)
}

function validateCategoryList(list, entityName) {
  if (!list.length) {
    return `至少保留一个${entityName}分类`
  }
  const values = new Set()
  for (const row of list) {
    const value = (row.value || '').trim()
    const label = (row.label || '').trim()
    if (!label) {
      return `${entityName}分类显示名称不能为空`
    }
    if (label.length > 20) {
      return `${entityName}分类显示名称最多 20 个字符`
    }
    if (!/^[a-zA-Z0-9_-]{1,40}$/.test(value)) {
      return `${entityName}分类标识只能用字母、数字、下划线或短横线：${value || '（空）'}`
    }
    if (values.has(value)) {
      return `${entityName}分类标识重复：${value}`
    }
    values.add(value)
  }
  return ''
}

function validate() {
  const tagMessage = validateCategoryList(categories.value, '词条')
  if (tagMessage) {
    return tagMessage
  }
  const groupMessage = validateCategoryList(groupCategories.value, '词组')
  if (groupMessage) {
    return groupMessage
  }

  if (!baseModels.value.length) {
    return '至少保留一个底模选项'
  }
  if (new Set(baseModels.value).size !== baseModels.value.length) {
    return '底模名称重复'
  }
  return ''
}

async function save() {
  const message = validate()
  if (message) {
    ElMessage.warning(message)
    return
  }

  saving.value = true
  try {
    const data = await saveSettings({
      tagCategories: categories.value.map((row) => ({
        value: row.value.trim(),
        label: row.label.trim()
      })),
      groupCategories: groupCategories.value.map((row) => ({
        value: row.value.trim(),
        label: row.label.trim()
      })),
      loraBaseModels: baseModels.value
    })
    categories.value = data.tagCategories.map((item) => ({
      value: item.value,
      label: item.label,
      isNew: false
    }))
    groupCategories.value = data.groupCategories.map((item) => ({
      value: item.value,
      label: item.label,
      isNew: false
    }))
    baseModels.value = [...data.loraBaseModels]
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
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

.panel {
  margin-bottom: 18px;
  border-radius: 10px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h2 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.panel-head p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #8a9099;
}

.inner-table {
  width: 100%;
}

.muted {
  color: #9aa0a6;
}

.tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8a9099;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 28px;
}

.add-row {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
</style>
