<template>
  <div class="wechat-menu-page">
    <el-card class="box-card" v-loading="loading">
      <el-alert
        title="当前未配置公众号发布服务，保存后将作为菜单草稿保留。"
        type="warning"
        show-icon
        :closable="false"
        class="mb20"
      />
      <el-row :gutter="30">
        <el-col :lg="9" :md="10" :sm="24" :xs="24">
          <div class="phone-menu-preview">
            <div class="phone-head">9:36</div>
            <div class="phone-body" />
            <div class="phone-menu-bar">
              <div
                v-for="(item, index) in menuList"
                :key="index"
                class="main-menu-cell"
                :class="{ active: activeMenu === item }"
              >
                <div class="sub-menu-list">
                  <el-button class="add-sub" size="small" @click="addSubMenu(item, index)">+</el-button>
                  <el-button
                    v-for="(child, childIndex) in item.sub_button"
                    :key="childIndex"
                    size="small"
                    class="sub-menu-item"
                    :class="{ active: activeMenu === child }"
                    @click="selectMenu(child, childIndex, index)"
                  >
                    {{ child.name || '二级菜单' }}
                  </el-button>
                </div>
                <el-button class="main-menu-button" @click="selectMenu(item, index, null)">
                  {{ item.name || '一级菜单' }}
                </el-button>
              </div>
              <div v-if="menuList.length < 3" class="main-menu-cell add-main-cell">
                <el-button class="main-menu-button" @click="addMainMenu">+</el-button>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :lg="13" :md="14" :sm="24" :xs="24">
          <div v-if="activeMenu" class="menu-form-panel">
            <div class="menu-form-head">
              <span>菜单信息</span>
              <el-button type="danger" size="small" @click="removeActiveMenu">删除</el-button>
            </div>
            <el-divider />
            <el-alert
              v-if="hasChildren(activeMenu)"
              class="mb15"
              title="已添加子菜单，仅一级菜单名称会生效。"
              type="success"
              show-icon
              :closable="false"
            />
            <el-form ref="formRef" :model="activeMenu" :rules="rules" label-width="100px" class="menu-form">
              <el-form-item label="菜单名称" prop="name">
                <el-input v-model="activeMenu.name" placeholder="请填写菜单名称" class="spwidth" />
              </el-form-item>
              <template v-if="!hasChildren(activeMenu)">
                <el-form-item label="规则状态" prop="type">
                  <el-select v-model="activeMenu.type" placeholder="请选择规则状态" class="spwidth">
                    <el-option value="click" label="关键字" />
                    <el-option value="view" label="跳转网页" />
                    <el-option value="miniprogram" label="小程序" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="activeMenu.type === 'click'" label="关键字" prop="key">
                  <div class="field-picker-row">
                    <el-input v-model="activeMenu.key" placeholder="请填写关键字" />
                    <el-button @click="openKeywordSelector">选择</el-button>
                  </div>
                </el-form-item>
                <template v-if="activeMenu.type === 'miniprogram'">
                  <el-form-item label="appid" prop="appid">
                    <el-input v-model="activeMenu.appid" placeholder="请填写appid" class="spwidth" />
                  </el-form-item>
                  <el-form-item label="备用网页" prop="url">
                    <div class="field-picker-row">
                      <el-input v-model="activeMenu.url" placeholder="请填写备用网页" />
                      <el-button @click="openLinkSelector('url')">选择</el-button>
                    </div>
                  </el-form-item>
                  <el-form-item label="小程序路径" prop="pagepath">
                    <el-input v-model="activeMenu.pagepath" placeholder="请填写小程序路径" class="spwidth" />
                  </el-form-item>
                </template>
                <el-form-item v-if="activeMenu.type === 'view'" label="跳转地址" prop="url">
                  <div class="field-picker-row">
                    <el-input v-model="activeMenu.url" placeholder="请填写跳转地址" />
                    <el-button @click="openLinkSelector('url')">选择</el-button>
                  </div>
                </el-form-item>
              </template>
            </el-form>
          </div>
          <el-empty v-else description="请选择或添加菜单" />
          <div class="menu-actions">
            <el-button type="primary" :loading="saving" @click="submit">保存并发布</el-button>
            <el-button type="danger" plain @click="clearRemoteMenu">删除公众号菜单</el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="keywordDialogVisible" title="选择关键字回复" width="760px" append-to-body destroy-on-close>
      <div class="dialog-toolbar">
        <el-input
          v-model="keywordQuery.keywords"
          clearable
          placeholder="搜索关键字"
          @keyup.enter="searchKeywords"
        />
        <el-select v-model="keywordQuery.type" clearable placeholder="回复类型">
          <el-option label="文本消息" value="text" />
          <el-option label="图片消息" value="image" />
          <el-option label="图文消息" value="news" />
          <el-option label="音频消息" value="voice" />
        </el-select>
        <el-button type="primary" @click="searchKeywords">搜索</el-button>
      </div>
      <el-table
        v-loading="keywordLoading"
        :data="keywordRows"
        size="small"
        height="360"
        highlight-current-row
        @row-dblclick="selectKeyword"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="keywords" label="关键字" min-width="170" show-overflow-tooltip />
        <el-table-column label="回复类型" width="110">
          <template #default="{ row }">{{ replyTypeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ replyContentText(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="selectKeyword(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="keywordQuery.page"
          v-model:page-size="keywordQuery.limit"
          layout="total, prev, pager, next"
          :total="keywordTotal"
          background
          @current-change="loadKeywords"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="linkDialogVisible" title="选择链接" width="860px" append-to-body destroy-on-close>
      <div class="link-picker">
        <aside class="link-picker-tabs">
          <button
            v-for="group in allLinkGroups"
            :key="group.key"
            type="button"
            :class="{ active: activeLinkGroup === group.key }"
            @click="switchLinkGroup(group.key)"
          >
            {{ group.title }}
          </button>
        </aside>
        <section class="link-picker-main">
          <template v-if="!isDynamicLinkGroup && activeLinkGroup !== 'custom'">
            <div class="dialog-toolbar">
              <el-input v-model="linkKeyword" clearable placeholder="搜索链接名称或路径" />
            </div>
            <div class="link-picker-grid">
              <button
                v-for="item in filteredLinkOptions"
                :key="item.id"
                type="button"
                class="link-picker-card"
                :class="{ active: selectedLink === item.url }"
                @click="selectedLink = item.url"
              >
                <strong>{{ item.name }}</strong>
                <span>{{ item.url }}</span>
              </button>
              <div v-if="!filteredLinkOptions.length" class="link-picker-empty">暂无链接</div>
            </div>
          </template>

          <template v-else-if="isDynamicLinkGroup">
            <div class="dialog-toolbar">
              <el-input
                v-model="dynamicLinkQuery.keyword"
                clearable
                :placeholder="`搜索${currentDynamicLinkGroup.title}`"
                @keyup.enter="searchDynamicLinks"
              />
              <el-button type="primary" @click="searchDynamicLinks">搜索</el-button>
            </div>
            <el-table
              v-loading="dynamicLinkLoading"
              :data="dynamicLinkRows"
              size="small"
              height="360"
              highlight-current-row
              @row-dblclick="selectDynamicLink"
            >
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column v-if="currentDynamicLinkGroup.image" label="图片" width="72">
                <template #default="{ row }">
                  <el-image v-if="row.image" class="dynamic-link-image" :src="assetUrl(row.image)" fit="cover" />
                  <span v-else class="muted">无</span>
                </template>
              </el-table-column>
              <el-table-column prop="name" :label="currentDynamicLinkGroup.nameLabel" min-width="180" show-overflow-tooltip />
              <el-table-column prop="url" label="链接" min-width="260" show-overflow-tooltip />
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="selectDynamicLink(row)">选择</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="currentDynamicLinkGroup.paged" class="dialog-pagination">
              <el-pagination
                v-model:current-page="dynamicLinkQuery.page"
                v-model:page-size="dynamicLinkQuery.limit"
                layout="total, prev, pager, next"
                :total="dynamicLinkTotal"
                background
                @current-change="loadDynamicLinks"
              />
            </div>
          </template>

          <div v-else class="custom-link-box">
            <el-input v-model="customLink" clearable placeholder="请输入自定义链接" />
            <el-alert
              title="公众号网页菜单发布通常要求 https 链接；系统允许先保存站内 H5 路径作为草稿。"
              type="info"
              show-icon
              :closable="false"
            />
          </div>
        </section>
      </div>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLinkSelector">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  articleList,
  bargainList,
  categoryTree,
  combinationList,
  pageDiyList,
  productList,
  seckillStoreList,
  wechatMenuDelete,
  wechatMenuInfo,
  wechatMenuSave,
  wechatReplyList
} from '../api';

const loading = ref(false);
const saving = ref(false);
const menuList = ref([]);
const activeMenu = ref(null);
const activeIndex = ref(null);
const activeParentIndex = ref(null);
const formRef = ref();
const keywordDialogVisible = ref(false);
const keywordLoading = ref(false);
const keywordRows = ref([]);
const keywordTotal = ref(0);
const linkDialogVisible = ref(false);
const linkTargetField = ref('url');
const activeLinkGroup = ref('mall');
const linkKeyword = ref('');
const selectedLink = ref('');
const customLink = ref('');
const dynamicLinkLoading = ref(false);
const dynamicLinkRows = ref([]);
const dynamicLinkTotal = ref(0);

const keywordQuery = reactive({
  page: 1,
  limit: 10,
  keywords: '',
  type: ''
});

const dynamicLinkQuery = reactive({
  page: 1,
  limit: 10,
  keyword: ''
});

const rules = {
  name: [{ required: true, message: '请填写菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择规则状态', trigger: 'change' }],
  key: [{ validator: validateKey, trigger: 'blur' }],
  appid: [{ validator: validateMiniField('请填写appid'), trigger: 'blur' }],
  pagepath: [{ validator: validateMiniField('请填写小程序路径'), trigger: 'blur' }],
  url: [{ validator: validateUrl, trigger: 'blur' }]
};

const linkGroups = [
  {
    key: 'mall',
    title: '商城页面',
    items: [
      { id: 47, name: '商城首页', url: '/pages/index/index' },
      { id: 46, name: '商城分类', url: '/pages/goods_cate/goods_cate' },
      { id: 45, name: '购物车', url: '/pages/order_addcart/order_addcart' },
      { id: 26, name: '我的订单', url: '/pages/users/order_list/index' },
      { id: 24, name: '退款列表', url: '/pages/users/user_return_list/index' },
      { id: 42, name: '文章列表', url: '/pages/news/news_list/index' },
      { id: 37, name: '精品推荐', url: '/pages/activity/promotionList/index?type=1&name=精品推荐' },
      { id: 38, name: '热门榜单', url: '/pages/activity/promotionList/index?type=2&name=热门榜单' },
      { id: 39, name: '首发新品', url: '/pages/activity/promotionList/index?type=3&name=首发新品' },
      { id: 40, name: '促销单品', url: '/pages/activity/promotionList/index?type=4&name=促销单品' }
    ]
  },
  {
    key: 'user',
    title: '个人中心',
    items: [
      { id: 44, name: '个人中心', url: '/pages/user/index' },
      { id: 28, name: '个人资料', url: '/pages/infos/user_info/index' },
      { id: 29, name: '我的账户', url: '/pages/users/user_money/index' },
      { id: 30, name: '地址列表', url: '/pages/users/user_address_list/index' },
      { id: 35, name: '收藏页面', url: '/pages/users/user_goods_collection/index' },
      { id: 36, name: '签到页面', url: '/pages/users/user_sgin/index' },
      { id: 50, name: '会员中心', url: '/pages/infos/user_vip/index' },
      { id: 11, name: '充值页面', url: '/pages/users/user_payment/index' },
      { id: 20, name: '提现页面', url: '/pages/users/user_cash/index' }
    ]
  },
  {
    key: 'distribution',
    title: '分销',
    items: [
      { id: 21, name: '我的推广', url: '/pages/promoter/user_spread_user/index' },
      { id: 19, name: '分销海报', url: '/pages/promoter/user_spread_code/index' },
      { id: 18, name: '推广人列表', url: '/pages/promoter/promoter-list/index' },
      { id: 17, name: '推广人订单', url: '/pages/promoter/promoter-order/index' },
      { id: 16, name: '推广人排行', url: '/pages/promoter/promoter_rank/index' },
      { id: 15, name: '佣金排行', url: '/pages/promoter/commission_rank/index' },
      { id: 48, name: '佣金记录', url: '/pages/promoter/user_spread_money/index?type=2' },
      { id: 49, name: '提现记录', url: '/pages/promoter/user_spread_money/index?type=1' }
    ]
  },
  {
    key: 'marketing',
    title: '营销链接',
    items: [
      { id: 41, name: '优惠券列表', url: '/pages/users/user_get_coupon/index' },
      { id: 25, name: '我的优惠券', url: '/pages/users/user_coupon/index' },
      { id: 32, name: '秒杀列表', url: '/pages/activity/goods_seckill/index' },
      { id: 31, name: '砍价列表', url: '/pages/activity/goods_bargain/index' },
      { id: 22, name: '砍价记录', url: '/pages/activity/bargain/index' },
      { id: 34, name: '拼团列表', url: '/pages/activity/goods_combination/index' },
      { id: 27, name: '积分详情', url: '/pages/users/user_integral/index' }
    ]
  }
];

const dynamicLinkGroups = [
  { key: 'product', title: '商品', nameLabel: '商品名称', image: true, paged: true },
  { key: 'productCategory', title: '商品分类', nameLabel: '分类名称', image: true, paged: false },
  { key: 'seckill', title: '秒杀商品', nameLabel: '秒杀商品', image: true, paged: true },
  { key: 'bargain', title: '砍价商品', nameLabel: '砍价商品', image: true, paged: true },
  { key: 'combination', title: '拼团商品', nameLabel: '拼团商品', image: true, paged: true },
  { key: 'article', title: '文章', nameLabel: '文章标题', image: true, paged: true },
  { key: 'micro', title: '微页面', nameLabel: '页面名称', image: false, paged: true }
];

const allLinkGroups = computed(() => [
  ...linkGroups,
  ...dynamicLinkGroups,
  { key: 'custom', title: '自定义链接' }
]);
const currentDynamicLinkGroup = computed(() => dynamicLinkGroups.find((item) => item.key === activeLinkGroup.value));
const isDynamicLinkGroup = computed(() => Boolean(currentDynamicLinkGroup.value));
const filteredLinkOptions = computed(() => {
  const keyword = linkKeyword.value.trim().toLowerCase();
  const group = linkGroups.find((item) => item.key === activeLinkGroup.value);
  const list = group?.items || [];
  if (!keyword) return list;
  return list.filter((item) => item.name.toLowerCase().includes(keyword) || item.url.toLowerCase().includes(keyword));
});

onMounted(loadMenus);

async function loadMenus() {
  loading.value = true;
  try {
    const data = await wechatMenuInfo();
    menuList.value = normalizeList(data?.menu?.button || []);
    activeMenu.value = null;
    activeIndex.value = null;
    activeParentIndex.value = null;
  } finally {
    loading.value = false;
  }
}

function normalizeList(list) {
  return (list || []).slice(0, 3).map((item) => ({
    name: item.name || '',
    type: item.type || 'click',
    appid: item.appid || '',
    url: item.url || '',
    key: item.key || '',
    pagepath: item.pagepath || '',
    sub_button: (item.sub_button || []).slice(0, 5).map((child) => ({
      name: child.name || '',
      type: child.type || 'click',
      appid: child.appid || '',
      url: child.url || '',
      key: child.key || '',
      pagepath: child.pagepath || ''
    }))
  }));
}

function defaultMainMenu() {
  return {
    name: '',
    type: 'click',
    appid: '',
    url: '',
    key: '',
    pagepath: '',
    sub_button: []
  };
}

function defaultChildMenu() {
  return {
    name: '',
    type: 'click',
    appid: '',
    url: '',
    key: '',
    pagepath: ''
  };
}

async function addMainMenu() {
  if (!(await checkActive())) return;
  const item = defaultMainMenu();
  menuList.value.push(item);
  selectMenu(item, menuList.value.length - 1, null);
}

async function addSubMenu(parent, parentIndex) {
  if (!(await checkActive())) return;
  if (parent.sub_button.length >= 5) {
    ElMessage.warning('二级菜单最多添加 5 个');
    return;
  }
  const item = defaultChildMenu();
  parent.sub_button.push(item);
  selectMenu(item, parent.sub_button.length - 1, parentIndex);
}

function selectMenu(item, index, parentIndex) {
  activeMenu.value = item;
  activeIndex.value = index;
  activeParentIndex.value = parentIndex;
}

async function checkActive() {
  if (!activeMenu.value || hasChildren(activeMenu.value)) return true;
  return await validateAllMenus(false);
}

async function submit() {
  if (!(await validateAllMenus(true))) return;
  saving.value = true;
  try {
    await wechatMenuSave({ button: cleanMenus(menuList.value) });
    ElMessage.success('提交成功');
    await loadMenus();
  } finally {
    saving.value = false;
  }
}

async function removeActiveMenu() {
  if (!activeMenu.value) {
    ElMessage.warning('请选择菜单');
    return;
  }
  await ElMessageBox.confirm('确定删除当前菜单?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  if (activeParentIndex.value === null) {
    menuList.value.splice(activeIndex.value, 1);
  } else {
    menuList.value[activeParentIndex.value].sub_button.splice(activeIndex.value, 1);
  }
  activeMenu.value = null;
  activeIndex.value = null;
  activeParentIndex.value = null;
}

async function clearRemoteMenu() {
  await ElMessageBox.confirm('确定删除当前本地保存的微信菜单?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await wechatMenuDelete();
  ElMessage.success('删除成功');
  await loadMenus();
}

async function openKeywordSelector() {
  keywordDialogVisible.value = true;
  keywordQuery.page = 1;
  keywordQuery.keywords = activeMenu.value?.key || '';
  await loadKeywords();
}

async function loadKeywords() {
  keywordLoading.value = true;
  try {
    const data = await wechatReplyList({
      page: keywordQuery.page,
      limit: keywordQuery.limit,
      keywords: keywordQuery.keywords || undefined,
      type: keywordQuery.type || undefined
    });
    keywordRows.value = data?.list || [];
    keywordTotal.value = Number(data?.total || 0);
  } finally {
    keywordLoading.value = false;
  }
}

function searchKeywords() {
  keywordQuery.page = 1;
  loadKeywords();
}

function selectKeyword(row) {
  if (!activeMenu.value) return;
  activeMenu.value.key = row.keywords || '';
  keywordDialogVisible.value = false;
}

function replyTypeText(type) {
  return {
    text: '文本消息',
    image: '图片消息',
    news: '图文消息',
    voice: '音频消息'
  }[type] || type || '-';
}

function replyContentText(row) {
  const data = parseReplyData(row.data);
  if (row.type === 'text') return data.content || '-';
  if (row.type === 'image') return data.srcUrl || data.mediaId || '图片消息';
  if (row.type === 'voice') return data.srcUrl || data.mediaId || '音频消息';
  if (row.type === 'news') return data.articleData?.title || `图文ID：${data.articleId || '-'}`;
  return row.data || '-';
}

function parseReplyData(value) {
  if (!value) return {};
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch (error) {
    return {};
  }
}

function openLinkSelector(field) {
  if (!activeMenu.value) return;
  linkTargetField.value = field;
  linkKeyword.value = '';
  dynamicLinkQuery.keyword = '';
  dynamicLinkQuery.page = 1;
  selectedLink.value = activeMenu.value[field] || '';
  customLink.value = selectedLink.value;
  activeLinkGroup.value = 'mall';
  dynamicLinkRows.value = [];
  dynamicLinkTotal.value = 0;
  linkDialogVisible.value = true;
}

function confirmLinkSelector() {
  if (!activeMenu.value) return;
  const value = activeLinkGroup.value === 'custom' ? customLink.value : selectedLink.value;
  activeMenu.value[linkTargetField.value] = value || '';
  linkDialogVisible.value = false;
}

function switchLinkGroup(key) {
  activeLinkGroup.value = key;
  linkKeyword.value = '';
  dynamicLinkQuery.keyword = '';
  dynamicLinkQuery.page = 1;
  dynamicLinkRows.value = [];
  dynamicLinkTotal.value = 0;
  if (dynamicLinkGroups.some((item) => item.key === key)) {
    loadDynamicLinks();
  }
}

function searchDynamicLinks() {
  dynamicLinkQuery.page = 1;
  loadDynamicLinks();
}

async function loadDynamicLinks() {
  const type = activeLinkGroup.value;
  dynamicLinkLoading.value = true;
  try {
    if (type === 'product') {
      const data = await productList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        keywords: dynamicLinkQuery.keyword || ''
      });
      dynamicLinkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.storeName || item.name || `商品 ${item.id}`,
        image: item.image,
        url: `/pages/goods/goods_details/index?id=${item.id}`
      }));
      dynamicLinkTotal.value = Number(data?.total || 0);
      return;
    }
    if (type === 'productCategory') {
      const data = await categoryTree({ type: 1, status: -1 });
      const keyword = dynamicLinkQuery.keyword.trim().toLowerCase();
      const rows = flattenCategoryLinks(data || [])
        .filter((item) => !keyword || item.name.toLowerCase().includes(keyword));
      dynamicLinkRows.value = rows;
      dynamicLinkTotal.value = rows.length;
      return;
    }
    if (type === 'article') {
      const data = await articleList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        keywords: dynamicLinkQuery.keyword || undefined
      });
      dynamicLinkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.title || item.name || `文章 ${item.id}`,
        image: item.imageInput || item.image,
        url: `/pages/news/news_details/index?id=${item.id}`
      }));
      dynamicLinkTotal.value = Number(data?.total || 0);
      return;
    }
    if (type === 'seckill') {
      const data = await seckillStoreList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        keywords: dynamicLinkQuery.keyword || ''
      });
      dynamicLinkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_seckill_details/index');
      dynamicLinkTotal.value = Number(data?.total || 0);
      return;
    }
    if (type === 'bargain') {
      const data = await bargainList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        keywords: dynamicLinkQuery.keyword || ''
      });
      dynamicLinkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_bargain_details/index');
      dynamicLinkTotal.value = Number(data?.total || 0);
      return;
    }
    if (type === 'combination') {
      const data = await combinationList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        keywords: dynamicLinkQuery.keyword || ''
      });
      dynamicLinkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_combination_details/index');
      dynamicLinkTotal.value = Number(data?.total || 0);
      return;
    }
    if (type === 'micro') {
      const data = await pageDiyList({
        page: dynamicLinkQuery.page,
        limit: dynamicLinkQuery.limit,
        name: dynamicLinkQuery.keyword || undefined
      });
      dynamicLinkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.name || `微页面 ${item.id}`,
        image: '',
        url: `/pages/activity/small_page/index?id=${item.id}`
      }));
      dynamicLinkTotal.value = Number(data?.total || 0);
    }
  } finally {
    dynamicLinkLoading.value = false;
  }
}

function flattenCategoryLinks(list, result = []) {
  for (const item of list || []) {
    if (item.id) {
      result.push({
        id: item.id,
        name: item.name || `分类 ${item.id}`,
        image: item.extra,
        url: `/pages/goods/goods_list/index?cid=${item.id}&title=${encodeURIComponent(item.name || '')}`
      });
    }
    flattenCategoryLinks(item.child || item.children || [], result);
  }
  return result;
}

function mapActivityLinkRows(list, path) {
  return (list || []).map((item) => ({
    id: item.id,
    name: item.title || item.storeName || item.name || `活动商品 ${item.id}`,
    image: item.image,
    url: `${path}?id=${item.id}`
  }));
}

function selectDynamicLink(row) {
  selectedLink.value = row.url;
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}

async function validateAllMenus(showMessage) {
  try {
    cleanMenus(menuList.value);
    return true;
  } catch (error) {
    if (showMessage) ElMessage.warning(error.message);
    return false;
  }
}

function cleanMenus(list) {
  if (list.length > 3) throw new Error('一级菜单最多 3 个');
  return list.map((item) => cleanMenu(item, false));
}

function cleanMenu(item, child) {
  if (!item.name?.trim()) throw new Error('请输入按钮名称!');
  const result = { name: item.name.trim() };
  if (!child && item.sub_button?.length) {
    if (item.sub_button.length > 5) throw new Error('二级菜单最多 5 个');
    result.sub_button = item.sub_button.map((sub) => cleanMenu(sub, true));
    return result;
  }
  result.type = item.type || 'click';
  if (result.type === 'click') {
    if (!item.key?.trim()) throw new Error('请输入关键字!');
    result.key = item.key.trim();
  } else if (result.type === 'view') {
    if (!isUrl(item.url)) throw new Error('请输入正确的跳转地址!');
    result.url = item.url.trim();
  } else if (result.type === 'miniprogram') {
    if (!item.appid?.trim() || !item.pagepath?.trim() || !item.url?.trim()) {
      throw new Error('请填写完整小程序配置!');
    }
    result.appid = item.appid.trim();
    result.url = item.url.trim();
    result.pagepath = item.pagepath.trim();
  }
  if (!child) result.sub_button = [];
  return result;
}

function hasChildren(item) {
  return Boolean(item?.sub_button?.length);
}

function validateKey(rule, value, callback) {
  if (!activeMenu.value || activeMenu.value.type !== 'click' || activeMenu.value.key?.trim()) {
    callback();
  } else {
    callback(new Error('请填写关键字'));
  }
}

function validateMiniField(message) {
  return (rule, value, callback) => {
    if (!activeMenu.value || activeMenu.value.type !== 'miniprogram' || String(value || '').trim()) {
      callback();
    } else {
      callback(new Error(message));
    }
  };
}

function validateUrl(rule, value, callback) {
  if (!activeMenu.value || activeMenu.value.type === 'click') {
    callback();
  } else if (activeMenu.value.type === 'miniprogram' && String(value || '').trim()) {
    callback();
  } else if (activeMenu.value.type === 'view' && isUrl(value)) {
    callback();
  } else {
    callback(new Error('请填写跳转地址'));
  }
}

function isUrl(value) {
  const text = String(value || '').trim();
  return /^https?:\/\/.+/i.test(text) || text.startsWith('/pages/');
}
</script>

<style scoped>
.mb20 {
  margin-bottom: 20px;
}

.mb15 {
  margin-bottom: 15px;
}

.phone-menu-preview {
  position: relative;
  width: 320px;
  height: 560px;
  max-width: 100%;
  margin: 0 auto 20px;
  border: 1px solid #dcdfe6;
  border-radius: 22px;
  overflow: hidden;
  background: #f4f5f9;
}

.phone-head {
  height: 62px;
  line-height: 62px;
  text-align: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.phone-body {
  height: 438px;
}

.phone-menu-bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 58px;
  display: flex;
  border-top: 1px solid #dcdfe6;
  background: #fff;
}

.main-menu-cell {
  position: relative;
  flex: 1;
  border-right: 1px solid #ebeef5;
}

.main-menu-cell:last-child {
  border-right: 0;
}

.main-menu-button {
  width: 100%;
  height: 58px;
  border: 0;
  border-radius: 0;
}

.sub-menu-list {
  position: absolute;
  bottom: 66px;
  left: 8px;
  right: 8px;
  display: flex;
  flex-direction: column-reverse;
  gap: 6px;
}

.sub-menu-list::after {
  content: "";
  position: absolute;
  bottom: -8px;
  left: 50%;
  margin-left: -5px;
  width: 10px;
  height: 10px;
  background: #fff;
  border-right: 1px solid #dcdfe6;
  border-bottom: 1px solid #dcdfe6;
  transform: rotate(45deg);
}

.sub-menu-item,
.add-sub {
  width: 100%;
  margin-left: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.active :deep(.el-button),
.sub-menu-item.active {
  border-color: #44b549;
  color: #44b549;
}

.menu-form-panel {
  min-height: 360px;
}

.menu-form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  color: #303133;
}

.spwidth {
  width: 360px;
  max-width: 100%;
}

.field-picker-row {
  display: flex;
  gap: 8px;
  width: 360px;
  max-width: 100%;
}

.field-picker-row :deep(.el-input) {
  flex: 1;
}

.menu-actions {
  margin-top: 22px;
  text-align: center;
}

.dialog-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.dialog-toolbar :deep(.el-input) {
  max-width: 280px;
}

.dialog-toolbar :deep(.el-select) {
  width: 150px;
}

.dialog-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.link-picker {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  min-height: 420px;
}

.link-picker-tabs {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  background: #fff;
}

.link-picker-tabs button {
  display: block;
  width: 100%;
  height: 38px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #606266;
  cursor: pointer;
  text-align: left;
  padding: 0 10px;
  font: inherit;
}

.link-picker-tabs button.active,
.link-picker-tabs button:hover {
  background: #ecf5ff;
  color: #409eff;
}

.link-picker-main {
  min-width: 0;
}

.link-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  max-height: 360px;
  overflow: auto;
}

.link-picker-card {
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  padding: 10px;
  text-align: left;
}

.link-picker-card:hover,
.link-picker-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.link-picker-card strong,
.link-picker-card span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-picker-card strong {
  color: #303133;
  font-size: 13px;
  margin-bottom: 6px;
}

.link-picker-card span,
.muted {
  color: #909399;
  font-size: 12px;
}

.link-picker-empty {
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

.custom-link-box {
  display: grid;
  gap: 12px;
  width: min(520px, 100%);
  margin-top: 40px;
}

.dynamic-link-image {
  width: 46px;
  height: 46px;
  display: block;
  background: #f5f7fa;
}
</style>
