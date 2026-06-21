<template>
  <div class="wechat-reply-page">
    <el-card class="box-card" v-loading="loading">
      <el-row :gutter="28">
        <el-col :lg="8" :md="10" :sm="24" :xs="24">
          <div class="phone-preview">
            <div class="phone-top">9:36</div>
            <div class="phone-screen">
              <div v-if="form.type !== 'news'" class="reply-bubble-row">
                <div class="reply-avatar">客</div>
                <div class="reply-bubble">
                  <span v-if="form.type === 'text'">{{ form.contents.content || '请填写规则内容' }}</span>
                  <img v-else-if="form.type === 'image' && form.contents.srcUrl" :src="assetUrl(form.contents.srcUrl)" alt="" />
                  <span v-else-if="form.type === 'image'">图片消息</span>
                  <span v-else-if="form.type === 'voice'">音频消息：{{ form.contents.mediaId || '待上传' }}</span>
                  <span v-else>请选择消息类型</span>
                </div>
              </div>
              <div v-else class="news-preview">
                <img v-if="form.contents.articleData.imageInput" :src="assetUrl(form.contents.articleData.imageInput)" alt="" />
                <div v-else class="news-preview-empty">图文封面</div>
                <p>{{ form.contents.articleData.title || '图文消息标题' }}</p>
              </div>
            </div>
            <div class="phone-bottom" />
          </div>
        </el-col>

        <el-col :lg="14" :md="14" :sm="24" :xs="24">
          <div class="reply-form-panel">
            <el-button v-if="isKeywordForm" class="mb20" @click="goList">返回</el-button>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" @submit.prevent>
              <el-form-item v-if="isKeywordForm" label="关键字：" prop="keywords">
                <div class="keyword-box">
                  <el-tag
                    v-for="item in keywordTags"
                    :key="item"
                    type="success"
                    closable
                    class="keyword-tag"
                    @close="removeKeyword(item)"
                  >
                    {{ item }}
                  </el-tag>
                  <el-input
                    v-model="keywordInput"
                    class="keyword-input"
                    size="small"
                    placeholder="输入后回车"
                    @keyup.enter="addKeyword"
                    @change="addKeyword"
                  />
                </div>
              </el-form-item>

              <el-form-item label="规则状态：">
                <el-radio-group v-model="form.status">
                  <el-radio :label="true">启用</el-radio>
                  <el-radio :label="false">禁用</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="消息类型：" prop="type">
                <el-select v-model="form.type" placeholder="请选择消息类型" class="reply-control" @change="resetContentByType">
                  <el-option label="文字消息" value="text" />
                  <el-option label="图片消息" value="image" />
                  <el-option label="图文消息" value="news" />
                  <el-option label="声音消息" value="voice" />
                </el-select>
              </el-form-item>

              <el-form-item v-if="form.type === 'text'" label="规则内容：" prop="content">
                <el-input v-model="form.contents.content" class="reply-control" placeholder="请填写规则内容" />
              </el-form-item>

              <el-form-item v-if="form.type === 'news'" label="图文标题：" prop="articleTitle">
                <el-input v-model="form.contents.articleData.title" class="reply-control" placeholder="请填写图文消息标题" />
              </el-form-item>
              <el-form-item v-if="form.type === 'news'" label="图文封面：">
                <div class="media-picker-line">
                  <el-upload action="#" :show-file-list="false" :http-request="uploadArticleImage" accept="image/*">
                    <div class="upLoadPicBox">
                      <div v-if="form.contents.articleData.imageInput" class="pictrue">
                        <img :src="assetUrl(form.contents.articleData.imageInput)" alt="" />
                      </div>
                      <div v-else class="upLoad">+</div>
                    </div>
                  </el-upload>
                  <div class="media-picker-actions">
                    <el-button type="primary" plain @click="openAttachmentSelector('articleImage')">选择素材</el-button>
                    <el-button v-if="form.contents.articleData.imageInput" @click="clearArticleImage">清除</el-button>
                  </div>
                </div>
              </el-form-item>

              <el-form-item
                v-if="form.type === 'image' || form.type === 'voice'"
                :label="form.type === 'image' ? '图片地址：' : '语音地址：'"
                prop="mediaId"
              >
                <div class="upload-line">
                  <el-input v-model="form.contents.mediaId" readonly class="reply-media-input" placeholder="上传后自动填入" />
                  <el-upload
                    action="#"
                    :show-file-list="false"
                    :http-request="form.type === 'image' ? uploadMediaImage : uploadVoice"
                    :accept="form.type === 'image' ? 'image/*' : '.mp3,.wma,.wav,.amr,audio/*'"
                  >
                    <el-button type="primary">点击上传</el-button>
                  </el-upload>
                  <el-button type="primary" plain @click="openAttachmentSelector(form.type)">选择素材</el-button>
                  <el-button v-if="form.contents.mediaId" @click="clearMedia">清除</el-button>
                </div>
                <div class="form-tip">
                  {{ form.type === 'image' ? '文件最大5Mb，支持bmp/png/jpeg/jpg/gif格式' : '文件最大5Mb，支持mp3/wma/wav/amr格式' }}
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="saving" @click="submit">保存并发布</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="attachmentDialogVisible" :title="attachmentTitle" width="860px" append-to-body destroy-on-close>
      <div class="attachment-picker">
        <div class="attachment-picker-tree">
          <el-tree
            :data="attachmentTree"
            :props="attachmentTreeProps"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleAttachmentCategory"
          />
        </div>
        <div class="attachment-picker-main">
          <div class="attachment-picker-toolbar">
            <span class="muted">选择已有本地素材，不调用真实微信公众号素材接口</span>
          </div>
          <div v-loading="attachmentLoading" class="attachment-picker-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.id"
              type="button"
              class="attachment-picker-card"
              @click="selectAttachment(item)"
            >
              <img v-if="attachmentMode !== 'voice'" :src="assetUrl(item.sattDir || item.attDir)" alt="" />
              <div v-else class="voice-file-card">音频</div>
              <span :title="item.name">{{ item.name || item.realName || item.attName || '-' }}</span>
            </button>
            <div v-if="!attachmentRows.length" class="attachment-picker-empty">素材为空</div>
          </div>
          <div class="block-pagination">
            <el-pagination
              v-model:current-page="attachmentQuery.page"
              :page-size="attachmentQuery.limit"
              layout="total, prev, pager, next"
              :total="attachmentTotal"
              background
              @current-change="loadAttachments"
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  attachmentList,
  categoryTree,
  uploadFile,
  uploadImage,
  wechatReplyInfo,
  wechatReplyInfoByKeywords,
  wechatReplySave,
  wechatReplyUpdate
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';
const VOICE_TYPES = 'mp3,wma,wav,amr,MP3,WMA,WAV,AMR';
const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const formRef = ref();
const keywordInput = ref('');
const keywordTags = ref([]);
const attachmentDialogVisible = ref(false);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const attachmentMode = ref('image');
const form = reactive(defaultForm());

const isKeywordForm = computed(() => props.path.includes('/keyword/save'));
const replyId = computed(() => {
  const match = props.path.match(/\/keyword\/save\/(\d+)/);
  return match ? Number(match[1]) : null;
});
const fixedKeyword = computed(() => props.path.includes('/follow') ? 'subscribe' : 'default');
const attachmentTitle = computed(() => (attachmentMode.value === 'voice' ? '选择语音素材' : '选择图片素材'));
const attachmentTree = computed(() => [
  {
    id: 0,
    name: attachmentMode.value === 'voice' ? '全部语音' : '全部图片',
    child: attachmentCategories.value
  }
]);

const attachmentTreeProps = {
  children: 'child',
  label: 'name'
};

const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const rules = {
  keywords: [{ validator: validateKeywords, trigger: 'change' }],
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  content: [{ validator: validateContent, trigger: 'blur' }],
  mediaId: [{ validator: validateMedia, trigger: 'change' }],
  articleTitle: [{ validator: validateArticle, trigger: 'blur' }]
};

onMounted(loadDetail);
watch(() => props.path, loadDetail);

async function loadDetail() {
  loading.value = true;
  try {
    Object.assign(form, defaultForm());
    keywordTags.value = [];
    keywordInput.value = '';
    let info = null;
    if (replyId.value) {
      info = await wechatReplyInfo({ id: replyId.value });
    } else if (!isKeywordForm.value) {
      info = await wechatReplyInfoByKeywords({ keywords: fixedKeyword.value });
    }
    applyInfo(info);
  } finally {
    loading.value = false;
  }
}

function applyInfo(info) {
  if (!info) {
    form.keywords = isKeywordForm.value ? '' : fixedKeyword.value;
    form.type = 'text';
    form.status = true;
    return;
  }
  const contents = parseData(info.data);
  Object.assign(form, {
    id: info.id || null,
    status: Boolean(info.status),
    type: info.type || 'text',
    keywords: info.keywords || '',
    data: info.data || '',
    contents: {
      content: contents.content || '',
      mediaId: contents.mediaId || '',
      srcUrl: contents.srcUrl || '',
      articleId: contents.articleId || null,
      articleData: contents.articleData || {}
    }
  });
  keywordTags.value = isKeywordForm.value && form.keywords ? form.keywords.split(',').filter(Boolean) : [];
}

async function submit() {
  if (isKeywordForm.value) {
    form.keywords = keywordTags.value.join(',');
  } else {
    form.keywords = fixedKeyword.value;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id,
      keywords: form.keywords,
      type: form.type,
      status: form.status,
      data: JSON.stringify(form.contents)
    };
    if (form.id) {
      await wechatReplyUpdate({ id: form.id }, payload);
      ElMessage.success('操作成功');
    } else {
      await wechatReplySave(payload);
      ElMessage.success('操作成功');
    }
    if (isKeywordForm.value) {
      goList();
    } else {
      await loadDetail();
    }
  } finally {
    saving.value = false;
  }
}

function addKeyword() {
  const text = keywordInput.value.trim();
  if (!text) return;
  if (!keywordTags.value.includes(text)) {
    keywordTags.value.push(text);
  }
  keywordInput.value = '';
  form.keywords = keywordTags.value.join(',');
}

function removeKeyword(item) {
  keywordTags.value = keywordTags.value.filter((tag) => tag !== item);
  form.keywords = keywordTags.value.join(',');
}

function resetContentByType(type) {
  form.contents.content = '';
  form.contents.mediaId = '';
  form.contents.srcUrl = '';
  form.contents.articleId = null;
  form.contents.articleData = {};
  if (type === 'news') {
    form.contents.articleData = { title: '', imageInput: '' };
  }
}

async function uploadMediaImage(options) {
  const result = await uploadByApi(uploadImage, options.file, { model: 'wechat', pid: 0 });
  form.contents.mediaId = result?.sattDir || result?.src || result?.url || result?.path || '';
  form.contents.srcUrl = form.contents.mediaId;
}

async function uploadArticleImage(options) {
  const result = await uploadByApi(uploadImage, options.file, { model: 'wechat', pid: 0 });
  form.contents.articleData.imageInput = result?.sattDir || result?.src || result?.url || result?.path || '';
}

async function uploadVoice(options) {
  const result = await uploadByApi(uploadFile, options.file, { model: 'wechat', pid: 0 });
  form.contents.mediaId = result?.sattDir || result?.src || result?.url || result?.path || '';
  form.contents.srcUrl = form.contents.mediaId;
}

async function openAttachmentSelector(mode) {
  attachmentMode.value = mode;
  attachmentDialogVisible.value = true;
  attachmentQuery.page = 1;
  attachmentQuery.pid = 0;
  attachmentQuery.attType = mode === 'voice' ? VOICE_TYPES : IMAGE_TYPES;
  if (!attachmentCategories.value.length) {
    attachmentCategories.value = await categoryTree({ type: 2, status: -1 });
  }
  await loadAttachments();
}

async function loadAttachments() {
  attachmentLoading.value = true;
  try {
    const data = await attachmentList({ ...attachmentQuery });
    attachmentRows.value = data?.list || [];
    attachmentTotal.value = Number(data?.total || 0);
  } finally {
    attachmentLoading.value = false;
  }
}

function handleAttachmentCategory(data) {
  attachmentQuery.pid = data.id;
  attachmentQuery.page = 1;
  loadAttachments();
}

function selectAttachment(item) {
  const path = item.sattDir || item.attDir || '';
  if (!path) return;
  if (attachmentMode.value === 'articleImage') {
    form.contents.articleData.imageInput = path;
  } else {
    form.contents.mediaId = path;
    form.contents.srcUrl = path;
  }
  attachmentDialogVisible.value = false;
  formRef.value?.validateField?.('mediaId');
}

function clearArticleImage() {
  form.contents.articleData.imageInput = '';
}

function clearMedia() {
  form.contents.mediaId = '';
  form.contents.srcUrl = '';
  formRef.value?.validateField?.('mediaId');
}

async function uploadByApi(api, file, params) {
  const data = new FormData();
  data.append('multipart', file);
  const result = await api(data, params);
  ElMessage.success('上传成功');
  return result;
}

function goList() {
  const path = '/appSetting/publicAccount/wxReply/keyword';
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function validateKeywords(rule, value, callback) {
  if (!isKeywordForm.value || keywordTags.value.length > 0) {
    callback();
  } else {
    callback(new Error('请输入后回车'));
  }
}

function validateContent(rule, value, callback) {
  if (form.type !== 'text' || form.contents.content.trim()) {
    callback();
  } else {
    callback(new Error('请填写规则内容'));
  }
}

function validateMedia(rule, value, callback) {
  if (!['image', 'voice'].includes(form.type) || form.contents.mediaId) {
    callback();
  } else {
    callback(new Error('请上传'));
  }
}

function validateArticle(rule, value, callback) {
  if (form.type !== 'news' || form.contents.articleData.title?.trim()) {
    callback();
  } else {
    callback(new Error('请填写图文消息标题'));
  }
}

function parseData(value) {
  try {
    return JSON.parse(value || '{}');
  } catch {
    return {};
  }
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}

function defaultForm() {
  return {
    id: null,
    status: true,
    type: 'text',
    keywords: '',
    data: '',
    contents: {
      content: '',
      mediaId: '',
      srcUrl: '',
      articleId: null,
      articleData: {}
    }
  };
}
</script>

<style scoped>
.wechat-reply-page {
  min-width: 0;
}

.phone-preview {
  width: 280px;
  max-width: 100%;
  margin: 0 auto 20px;
  border: 1px solid #dcdfe6;
  border-radius: 22px;
  background: #f7f8fa;
  overflow: hidden;
}

.phone-top {
  height: 44px;
  line-height: 44px;
  text-align: center;
  font-size: 13px;
  color: #303133;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.phone-screen {
  min-height: 360px;
  padding: 24px 16px;
  background: #eef1f5;
}

.phone-bottom {
  height: 42px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.reply-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.reply-avatar {
  width: 34px;
  height: 34px;
  line-height: 34px;
  text-align: center;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  font-size: 13px;
}

.reply-bubble {
  max-width: 190px;
  min-height: 34px;
  padding: 9px 10px;
  border-radius: 4px;
  background: #fff;
  color: #303133;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

.reply-bubble img {
  max-width: 160px;
  max-height: 150px;
  display: block;
  border-radius: 3px;
}

.news-preview {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.news-preview img,
.news-preview-empty {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}

.news-preview p {
  margin: 0;
  padding: 10px 12px;
  font-size: 13px;
}

.reply-form-panel {
  max-width: 620px;
}

.reply-control {
  width: 90%;
}

.keyword-box {
  width: 90%;
  min-height: 34px;
  padding: 3px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}

.keyword-tag {
  margin: 3px 5px 3px 0;
}

.keyword-input {
  width: 180px;
  margin: 3px 0;
}

.upload-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.media-picker-line {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.media-picker-actions {
  display: flex;
  gap: 8px;
  padding-top: 34px;
}

.reply-media-input {
  width: 360px;
  max-width: 100%;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.mb20 {
  margin-bottom: 20px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.attachment-picker {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  min-height: 420px;
}

.attachment-picker-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
  overflow: auto;
}

.attachment-picker-main {
  min-width: 0;
}

.attachment-picker-toolbar {
  min-height: 32px;
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.attachment-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 12px;
  min-height: 300px;
}

.attachment-picker-card {
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  padding: 8px;
  cursor: pointer;
  text-align: left;
}

.attachment-picker-card:hover {
  border-color: #409eff;
}

.attachment-picker-card img,
.voice-file-card {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
}

.attachment-picker-card span {
  display: block;
  margin-top: 6px;
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-picker-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #909399;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}
</style>
