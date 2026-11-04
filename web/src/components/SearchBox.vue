<template>
  <div class="search-box" :style="{ width }">
    <el-autocomplete
      v-model="inner"
      :fetch-suggestions="query"
      :trigger-on-focus="false"
      :highlight-first-item="false"
      value-key="value"
      clearable
      :placeholder="placeholder"
      style="flex: 1"
      @select="onCommit"
      @clear="onCommit"
      @keyup.enter="onCommit"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template #default="{ item }">
        <span class="s-main">{{ item.value }}</span>
        <span v-if="item.sub" class="s-sub">{{ item.sub }}</span>
      </template>
    </el-autocomplete>
    <el-button v-if="showButton" :icon="Search" @click="onCommit">{{ buttonText }}</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '输入关键字，回车确认' },
  width: { type: String, default: '340px' },
  /** 返回 [{ value, sub }] 或字符串数组 */
  suggest: { type: Function, default: null },
  showButton: { type: Boolean, default: true },
  buttonText: { type: String, default: '搜索' }
})
const emit = defineEmits(['update:modelValue', 'search'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

async function query(keyword, callback) {
  const kw = (keyword || '').trim()
  if (!props.suggest || kw === '') {
    callback([])
    return
  }
  try {
    const list = await props.suggest(kw)
    callback((list || []).map((item) => (typeof item === 'string' ? { value: item } : item)))
  } catch (e) {
    callback([])
  }
}

function onCommit() {
  emit('search')
}
</script>

<style scoped>
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.s-main {
  font-size: 13px;
}

.s-sub {
  margin-left: 8px;
  font-size: 12px;
  color: #8b909a;
}
</style>
