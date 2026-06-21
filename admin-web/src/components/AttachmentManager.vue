<template>
  <div class="attachment-page">
    <el-tabs v-model="activeType" class="attachment-tabs" @tab-change="handleTypeChange">
      <el-tab-pane label="图片" name="pic" />
      <el-tab-pane label="视频" name="video" />
    </el-tabs>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="5" class="attachment-tree-col">
        <div class="attachment-tree-panel">
          <el-tree
            :data="treeData"
            :props="treeProps"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="attachment-tree-node" :class="{ special: query.pid === data.id }">
                <span :title="node.label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-col>

      <el-col :span="19">
        <div class="attachment-main-panel">
          <div class="attachment-toolbar">
            <div class="attachment-actions">
              <el-upload
                multiple
                :show-file-list="false"
                :http-request="handleUpload"
                :accept="activeType === 'pic' ? 'image/*' : 'video/*'"
              >
                <el-button type="primary">
                  {{ activeType === 'pic' ? '上传图片' : '上传视频' }}
                </el-button>
              </el-upload>
              <el-button :disabled="!selectedIds.length" @click="handleDeleteSelected">
                {{ activeType === 'pic' ? '删除图片' : '删除视频' }}
              </el-button>
              <el-select
                v-model="movePid"
                :disabled="!selectedIds.length"
                placeholder="图片移动至"
                class="attachment-move-select"
                clearable
              >
                <el-option :value="0" label="全部图片" />
                <el-option
                  v-for="item in flatCategories"
                  :key="item.id"
                  :value="item.id"
                  :label="item.label"
                />
              </el-select>
              <el-button :disabled="!selectedIds.length || movePid === null || movePid === undefined" @click="handleMove">
                移动分类
              </el-button>
            </div>

            <div class="attachment-view-switch">
              <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="switchView('grid')">
                <el-icon><Menu /></el-icon>
              </el-button>
              <el-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="switchView('list')">
                <el-icon><Operation /></el-icon>
              </el-button>
            </div>
          </div>

          <div v-if="viewMode === 'grid'" class="attachment-grid">
            <div v-if="!rows.length" class="attachment-empty">
              <el-icon><Picture /></el-icon>
              <span>素材为空</span>
            </div>
            <div
              v-for="item in rows"
              :key="item.attId"
              class="attachment-card"
              :class="{ selected: selectedIds.includes(item.attId) }"
              @click="toggleSelect(item)"
            >
              <span v-if="selectedIds.includes(item.attId)" class="attachment-check">{{ selectedIds.indexOf(item.attId) + 1 }}</span>
              <img v-if="!isVideo(item)" :src="resourceUrl(item.sattDir)" :alt="item.name" />
              <video v-else :src="resourceUrl(item.sattDir)" muted />
              <div class="attachment-name" :title="item.name">{{ item.name }}</div>
              <div class="attachment-operate">
                <el-button link type="danger" @click.stop="handleDeleteOne(item)">删除</el-button>
              </div>
            </div>
          </div>

          <el-table
            v-else
            :data="rows"
            size="small"
            border
            class="attachment-table"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column label="图片名称" min-width="260">
              <template #default="{ row }">
                <div class="attachment-row-file">
                  <img v-if="!isVideo(row)" :src="resourceUrl(row.sattDir)" alt="" />
                  <video v-else :src="resourceUrl(row.sattDir)" muted />
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="attSize" label="大小" min-width="90" />
            <el-table-column prop="createTime" label="上传时间" min-width="170" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleDeleteOne(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="block">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.limit"
              :page-sizes="[18, 20, 40, 60]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              background
              @size-change="loadList"
              @current-change="loadList"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Menu, Operation, Picture } from '@element-plus/icons-vue';
import {
  attachmentDelete,
  attachmentList,
  attachmentMove,
  categoryTree,
  uploadFile,
  uploadImage
} from '../api';

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';
const VIDEO_TYPES = 'video/mp4,mp4';

const activeType = ref('pic');
const viewMode = ref('grid');
const loading = ref(false);
const uploadLoading = ref(false);
const categories = ref([]);
const rows = ref([]);
const total = ref(0);
const selectedIds = ref([]);
const movePid = ref(null);

const query = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const treeProps = {
  children: 'child',
  label: 'name'
};

const treeData = computed(() => [
  {
    id: 0,
    name: activeType.value === 'pic' ? '全部图片' : '全部视频',
    child: categories.value
  }
]);

const flatCategories = computed(() => flattenCategories(categories.value));

function flattenCategories(items, level = 0) {
  const result = [];
  for (const item of items || []) {
    result.push({
      id: item.id,
      label: `${'　'.repeat(level)}${item.name}`
    });
    result.push(...flattenCategories(item.child || [], level + 1));
  }
  return result;
}

async function loadCategories() {
  categories.value = await categoryTree({ type: 2, status: -1 });
}

async function loadList() {
  loading.value = true;
  try {
    const data = await attachmentList({ ...query });
    rows.value = data?.list || [];
    total.value = data?.total || 0;
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
}

function handleTypeChange() {
  query.page = 1;
  query.pid = 0;
  query.attType = activeType.value === 'pic' ? IMAGE_TYPES : VIDEO_TYPES;
  movePid.value = null;
  loadList();
}

function handleNodeClick(data) {
  query.pid = data.id;
  query.page = 1;
  loadList();
}

function switchView(mode) {
  viewMode.value = mode;
  query.page = 1;
  query.limit = mode === 'grid' ? 18 : 20;
  loadList();
}

function toggleSelect(item) {
  const index = selectedIds.value.indexOf(item.attId);
  if (index >= 0) {
    selectedIds.value.splice(index, 1);
  } else {
    selectedIds.value.push(item.attId);
  }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map((item) => item.attId);
}

async function handleUpload(option) {
  const file = option.file;
  if (!file) return;
  uploadLoading.value = true;
  try {
    const form = new FormData();
    form.append('multipart', file);
    const params = {
      model: activeType.value === 'pic' ? 'product' : 'video',
      pid: query.pid || 0
    };
    if (activeType.value === 'pic') {
      await uploadImage(form, params);
    } else {
      await uploadFile(form, params);
    }
    ElMessage.success('上传成功');
    await loadList();
    option.onSuccess?.();
  } catch (error) {
    option.onError?.(error);
  } finally {
    uploadLoading.value = false;
  }
}

async function handleDeleteOne(row) {
  await confirmDelete();
  await attachmentDelete(String(row.attId));
  ElMessage.success('删除成功');
  await loadList();
}

async function handleDeleteSelected() {
  await confirmDelete();
  await attachmentDelete(selectedIds.value.join(','));
  ElMessage.success('删除成功');
  await loadList();
}

async function handleMove() {
  await attachmentMove({
    attrId: selectedIds.value.join(','),
    pid: movePid.value || 0
  });
  ElMessage.success('移动成功');
  movePid.value = null;
  await loadList();
}

function confirmDelete() {
  return ElMessageBox.confirm('删除当前素材?', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });
}

function resourceUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path) || path.startsWith('/')) {
    return path;
  }
  return `/${path}`;
}

function isVideo(item) {
  return item?.attType === 'video/mp4' || /\.mp4$/i.test(item?.sattDir || '');
}

onMounted(async () => {
  await loadCategories();
  await loadList();
});
</script>
