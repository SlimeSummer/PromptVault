<template>
  <div class="image-panel">
    <div class="panel-toolbar">
      <el-upload
        :show-file-list="false"
        accept="image/*"
        :http-request="onUpload"
      >
        <el-button type="primary" :loading="uploading">上传图片</el-button>
      </el-upload>
      <span class="muted">最多 20 张，支持点击放大预览</span>
    </div>

    <div v-if="images.length" class="image-grid">
      <div v-for="img in images" :key="img.id" class="image-card">
        <el-image
          :src="imageUrl(img.id)"
          fit="cover"
          class="thumb"
          :preview-src-list="previewList"
          :initial-index="images.indexOf(img)"
          preview-teleported
        />
        <el-button
          class="delete-btn"
          type="danger"
          size="small"
          circle
          @click="onDelete(img)"
        >
          ×
        </el-button>
      </div>
    </div>
    <el-empty v-else description="还没有参考图，先上传一张" :image-size="60" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteImage, fetchImages, imageUrl, uploadImage } from '@/api/image'

const props = defineProps({
  refType: { type: String, required: true },
  refId: { type: Number, required: true },
  title: { type: String, default: '' }
})

const images = ref([])
const uploading = ref(false)
const loading = ref(false)

const previewList = computed(() => images.value.map((img) => imageUrl(img.id)))

async function load() {
  if (!props.refId) {
    images.value = []
    return
  }
  loading.value = true
  try {
    images.value = await fetchImages(props.refType, props.refId)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function onUpload({ file }) {
  uploading.value = true
  try {
    await uploadImage(props.refType, props.refId, file)
    ElMessage.success('已上传')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    uploading.value = false
  }
}

async function onDelete(img) {
  try {
    await ElMessageBox.confirm('确定删除这张参考图吗？', '删除图片', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteImage(img.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

watch(() => props.refId, load)

onMounted(load)
</script>

<style scoped>
.panel-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.muted {
  font-size: 12px;
  color: #9aa0a6;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-card {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e9ec;
  background: #f7f8fa;
}

.thumb {
  width: 100%;
  height: 100%;
  display: block;
}

.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  padding: 0;
  font-size: 13px;
  line-height: 1;
  opacity: 0;
  transition: opacity 0.15s;
}

.image-card:hover .delete-btn {
  opacity: 1;
}
</style>
