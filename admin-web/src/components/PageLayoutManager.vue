<template>
  <section class="layout-page" v-loading="loading">
    <el-card shadow="never" class="layout-card">
      <div class="layout-shell">
        <aside class="layout-tabs">
          <button
            v-for="item in tabs"
            :key="item.key"
            type="button"
            :class="{ active: activeTab === item.key }"
            @click="activeTab = item.key"
          >
            {{ item.label }}
          </button>
        </aside>

        <div class="phone-preview">
          <template v-if="activeTab === 'userMenu' || activeTab === 'userBanner'">
            <div class="user-head">
              <div class="user-card">
                <div class="avatar"></div>
                <div>
                  <p>用户信息</p>
                  <span>123456</span>
                </div>
              </div>
            </div>
            <div class="preview-banner" v-if="visibleList('userBanner').length">
              <img :src="assetUrl(visibleList('userBanner')[0].pic)" alt="" />
            </div>
            <h4>我的服务</h4>
            <div class="preview-menu">
              <div v-for="item in visibleList('userMenu').slice(0, 12)" :key="item.id || item.sort">
                <img v-if="item.pic" :src="assetUrl(item.pic)" alt="" />
                <span v-else></span>
                <p>{{ item.name || '未命名' }}</p>
              </div>
            </div>
          </template>
          <template v-else-if="activeTab === 'bottomNavigation'">
            <div class="preview-spacer">首页内容区域</div>
            <div class="preview-foot">
              <div v-for="item in visibleBottomNavigation" :key="item.id || item.sort">
                <img :src="assetUrl(item.checked || item.unchecked)" alt="" />
                <p>{{ item.name || '未命名' }}</p>
              </div>
            </div>
          </template>
          <template v-else-if="activeTab === 'categoryConfig'">
            <div class="category-preview" :class="`style-${categoryConfig.categoryPageConfig}`">
              <div class="category-sidebar" v-if="categoryConfig.isShowCategory">
                <span class="active">推荐</span>
                <span>香水</span>
                <span>护肤</span>
                <span>彩妆</span>
              </div>
              <div class="category-content">
                <div class="category-search">搜索商品</div>
                <div class="category-banner"></div>
                <div class="category-grid">
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="model-header">
              <div>logo区域</div>
              <span>搜索</span>
            </div>
            <div class="preview-banner" v-if="visibleList('indexBanner').length">
              <img :src="assetUrl(visibleList('indexBanner')[0].pic)" alt="" />
            </div>
            <div class="preview-news" v-if="visibleList('indexNews').length">
              <strong>快讯</strong>
              <p>{{ visibleList('indexNews')[0].info || visibleList('indexNews')[0].name }}</p>
            </div>
            <div class="preview-menu">
              <div v-for="item in visibleList('indexMenu').slice(0, 10)" :key="item.id || item.sort">
                <img v-if="item.pic" :src="assetUrl(item.pic)" alt="" />
                <span v-else></span>
                <p>{{ item.name || '未命名' }}</p>
              </div>
            </div>
            <div class="preview-product-tabs" v-if="visibleList('indexTable').length">
              <button
                v-for="item in visibleList('indexTable').slice(0, 4)"
                :key="item.id || item.sort"
                type="button"
              >
                <strong>{{ item.name || '商品 Tab' }}</strong>
                <small>{{ item.info || item.url || '首页商品入口' }}</small>
              </button>
            </div>
          </template>
        </div>

        <section class="config-panel">
          <div class="panel-head">
            <div>
              <h3>{{ currentTitle }}</h3>
              <p>{{ currentDesc }}</p>
            </div>
            <el-button type="primary" :loading="saving" @click="saveCurrent">保存</el-button>
          </div>

          <div v-if="activeTab === 'bottomNavigation'" class="custom-row">
            <span>是否自定义</span>
            <el-switch v-model="isCustom" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
          </div>

          <div v-if="activeTab === 'categoryConfig'" class="category-config-panel">
            <el-form label-width="110px">
              <el-form-item label="分类页样式">
                <el-radio-group v-model="categoryConfig.categoryPageConfig">
                  <el-radio-button label="1">双栏分类</el-radio-button>
                  <el-radio-button label="2">宫格分类</el-radio-button>
                  <el-radio-button label="3">瀑布分类</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="显示分类导航">
                <el-switch
                  v-model="categoryConfig.isShowCategory"
                  active-text="显示"
                  inactive-text="隐藏"
                  :active-value="1"
                  :inactive-value="0"
                />
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activeTab !== 'categoryConfig'" class="item-list">
            <div
              v-for="(item, index) in currentList"
              :key="item.localKey || item.id || index"
              class="layout-item"
              :class="{ dragging: dragState.from === index, 'drag-over': dragState.over === index }"
              @dragover.prevent="handleDragOver(index)"
              @drop.prevent="handleDrop(index)"
            >
              <button
                type="button"
                class="item-sort"
                title="拖拽排序"
                draggable="true"
                @dragstart="handleDragStart(index, $event)"
                @dragend="handleDragEnd"
              >
                <span class="drag-lines"></span>
                <em>{{ index + 1 }}</em>
              </button>
              <el-form label-width="74px" class="item-form">
                <el-form-item label="标题">
                  <el-input v-model="item.name" maxlength="20" />
                </el-form-item>
                <el-form-item label="说明" v-if="activeTab === 'indexNews' || activeTab === 'indexTable'">
                  <el-input v-model="item.info" maxlength="80" />
                </el-form-item>
                <el-form-item label="图片" v-if="activeTab !== 'indexNews' && activeTab !== 'bottomNavigation'">
                  <div class="image-field-row">
                    <el-input v-model="item.pic" placeholder="crmebimage/..." />
                    <el-button type="primary" plain @click="openAttachmentSelector(item, 'pic')">选择素材</el-button>
                    <el-button v-if="item.pic" @click="clearImageField(item, 'pic')">清除</el-button>
                  </div>
                </el-form-item>
                <template v-if="activeTab === 'bottomNavigation'">
                  <el-form-item label="选中图标">
                    <div class="image-field-row">
                      <el-input v-model="item.checked" placeholder="crmebimage/..." />
                      <el-button type="primary" plain @click="openAttachmentSelector(item, 'checked')">选择素材</el-button>
                      <el-button v-if="item.checked" @click="clearImageField(item, 'checked')">清除</el-button>
                    </div>
                  </el-form-item>
                  <el-form-item label="未选图标">
                    <div class="image-field-row">
                      <el-input v-model="item.unchecked" placeholder="crmebimage/..." />
                      <el-button type="primary" plain @click="openAttachmentSelector(item, 'unchecked')">选择素材</el-button>
                      <el-button v-if="item.unchecked" @click="clearImageField(item, 'unchecked')">清除</el-button>
                    </div>
                  </el-form-item>
                </template>
                <el-form-item label="链接">
                  <div class="link-field-row">
                    <el-input
                      :model-value="getItemLink(item)"
                      placeholder="请选择或填写链接"
                      @input="setItemLink(item, $event)"
                    />
                    <el-button type="primary" plain @click="openLinkSelector(item)">选择链接</el-button>
                    <el-button v-if="getItemLink(item)" @click="setItemLink(item, '')">清除</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="状态">
                  <el-switch v-model="item.status" active-text="显示" inactive-text="隐藏" />
                </el-form-item>
              </el-form>
              <el-button v-if="currentList.length > minCount" link type="danger" @click="removeItem(index)">删除</el-button>
            </div>
          </div>
          <el-button v-if="activeTab !== 'categoryConfig'" plain type="primary" @click="addItem">添加配置</el-button>
        </section>
      </div>
    </el-card>

    <el-dialog v-model="attachmentDialogVisible" title="选择素材" width="860px" append-to-body destroy-on-close>
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
            <span class="muted">选择已有素材并回填当前图片字段</span>
          </div>
          <div v-loading="attachmentLoading" class="attachment-picker-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.id"
              type="button"
              class="attachment-picker-card"
              @click="selectAttachment(item)"
            >
              <img :src="assetUrl(item.sattDir || item.attDir)" alt="" />
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

    <el-dialog v-model="linkDialogVisible" title="选择链接" width="860px" append-to-body destroy-on-close>
      <div class="link-picker">
        <aside class="link-picker-tabs">
          <button
            v-for="group in linkGroups"
            :key="group.key"
            type="button"
            :class="{ active: activeLinkGroup === group.key }"
            @click="activeLinkGroup = group.key"
          >
            {{ group.title }}
          </button>
          <button
            v-for="group in dynamicLinkGroups"
            :key="group.key"
            type="button"
            :class="{ active: activeLinkGroup === group.key }"
            @click="switchLinkGroup(group.key)"
          >
            {{ group.title }}
          </button>
          <button
            type="button"
            :class="{ active: activeLinkGroup === 'custom' }"
            @click="switchLinkGroup('custom')"
          >
            自定义链接
          </button>
        </aside>
        <section class="link-picker-main">
          <template v-if="!isDynamicLinkGroup && activeLinkGroup !== 'custom'">
            <div class="link-picker-toolbar">
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
            <div class="link-picker-toolbar">
              <el-input
                v-model="dynamicLinkQuery.keyword"
                clearable
                :placeholder="`搜索${currentDynamicLinkGroup?.title || '内容'}`"
                @keyup.enter="searchDynamicLinks"
              />
              <el-button type="primary" @click="searchDynamicLinks">搜索</el-button>
            </div>
            <el-table
              v-loading="dynamicLinkLoading"
              :data="dynamicLinkRows"
              size="small"
              border
              class="dynamic-link-table"
              @row-click="selectDynamicLink"
            >
              <el-table-column label="" width="54">
                <template #default="{ row }">
                  <el-radio :model-value="selectedLink" :label="row.url">&nbsp;</el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column v-if="currentDynamicLinkGroup?.image" label="图片" width="82">
                <template #default="{ row }">
                  <el-image v-if="row.image" class="dynamic-link-image" :src="assetUrl(row.image)" fit="cover" />
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="name" :label="currentDynamicLinkGroup?.nameLabel || '名称'" min-width="220" show-overflow-tooltip />
              <el-table-column prop="url" label="页面链接" min-width="260" show-overflow-tooltip />
            </el-table>
            <div v-if="currentDynamicLinkGroup?.paged" class="block-pagination">
              <el-pagination
                v-model:current-page="dynamicLinkQuery.page"
                v-model:page-size="dynamicLinkQuery.limit"
                :page-sizes="[10, 20, 40]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="dynamicLinkTotal"
                background
                @size-change="loadDynamicLinks"
                @current-change="loadDynamicLinks"
              />
            </div>
          </template>
          <template v-else>
            <div class="custom-link-box">
              <p class="muted">可填写 H5/uni-app 老页面路径，例如 /pages/index/index，也可填写完整 URL。</p>
              <el-input v-model.trim="customLink" placeholder="请输入跳转路径" />
            </div>
          </template>
        </section>
      </div>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLinkSelector">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  articleList,
  attachmentList,
  bargainList,
  categoryTree,
  combinationList,
  pageDiyList,
  pageLayoutBottomNavigation,
  pageLayoutCategoryConfig,
  pageLayoutIndex,
  pageLayoutSave,
  productList,
  seckillStoreList
} from '../api';

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';
const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const activeTab = ref('userMenu');
const isCustom = ref(0);
const categoryConfig = reactive({
  categoryPageConfig: '1',
  isShowCategory: 1
});
const attachmentDialogVisible = ref(false);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const attachmentTarget = ref(null);
const attachmentField = ref('');
const linkDialogVisible = ref(false);
const linkTarget = ref(null);
const activeLinkGroup = ref('mall');
const linkKeyword = ref('');
const selectedLink = ref('');
const customLink = ref('');
const dynamicLinkLoading = ref(false);
const dynamicLinkRows = ref([]);
const dynamicLinkTotal = ref(0);
const dragState = reactive({
  from: null,
  over: null
});
const state = reactive({
  indexBanner: [],
  indexTable: [],
  indexMenu: [],
  indexNews: [],
  userMenu: [],
  userBanner: [],
  bottomNavigation: []
});

const tabs = [
  { key: 'userMenu', label: '个人中心服务', desc: '配置个人中心“我的服务”入口' },
  { key: 'userBanner', label: '个人中心轮播', desc: '配置个人中心轮播图' },
  { key: 'bottomNavigation', label: '底部菜单', desc: '配置移动端底部导航' },
  { key: 'categoryConfig', label: '分类页配置', desc: '配置商品分类页展示方式' },
  { key: 'indexBanner', label: '首页轮播', desc: '配置首页 banner' },
  { key: 'indexTable', label: '首页商品 Tab', desc: '配置首页超值爆款、商品分组等入口' },
  { key: 'indexMenu', label: '首页导航', desc: '配置首页金刚区导航' },
  { key: 'indexNews', label: '首页新闻', desc: '配置首页滚动新闻' }
];
const currentMeta = computed(() => tabs.find((item) => item.key === activeTab.value) || tabs[0]);
const currentTitle = computed(() => currentMeta.value.label);
const currentDesc = computed(() => currentMeta.value.desc);
const currentList = computed(() => state[activeTab.value] || []);
const minCount = computed(() => {
  if (activeTab.value === 'bottomNavigation') return 4;
  if (activeTab.value === 'indexTable') return 2;
  return 1;
});
const visibleBottomNavigation = computed(() => state.bottomNavigation.filter((item) => item.status));
const attachmentTree = computed(() => [
  {
    id: 0,
    name: '全部图片',
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

const linkGroups = [
  {
    key: 'mall',
    title: '商城页面',
    items: [
      { id: 47, name: '商城首页', url: '/pages/index/index' },
      { id: 46, name: '商城分类', url: '/pages/goods_cate/goods_cate' },
      { id: 45, name: '购物车', url: '/pages/order_addcart/order_addcart' },
      { id: 43, name: '分类商品列表', url: '/pages/goods/goods_list/index' },
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

const filteredLinkOptions = computed(() => {
  const keyword = linkKeyword.value.trim().toLowerCase();
  const group = linkGroups.find((item) => item.key === activeLinkGroup.value);
  const list = group?.items || [];
  if (!keyword) return list;
  return list.filter((item) => item.name.toLowerCase().includes(keyword) || item.url.toLowerCase().includes(keyword));
});
const dynamicLinkGroups = [
  { key: 'product', title: '商品', nameLabel: '商品名称', image: true, paged: true },
  { key: 'productCategory', title: '商品分类', nameLabel: '分类名称', image: true, paged: false },
  { key: 'seckill', title: '秒杀商品', nameLabel: '秒杀商品', image: true, paged: true },
  { key: 'bargain', title: '砍价商品', nameLabel: '砍价商品', image: true, paged: true },
  { key: 'combination', title: '拼团商品', nameLabel: '拼团商品', image: true, paged: true },
  { key: 'article', title: '文章', nameLabel: '文章标题', image: true, paged: true },
  { key: 'micro', title: '微页面', nameLabel: '页面名称', image: false, paged: true }
];
const currentDynamicLinkGroup = computed(() => dynamicLinkGroups.find((item) => item.key === activeLinkGroup.value));
const isDynamicLinkGroup = computed(() => Boolean(currentDynamicLinkGroup.value));
const dynamicLinkQuery = reactive({
  page: 1,
  limit: 10,
  keyword: ''
});

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [layout, bottom, category] = await Promise.all([
      pageLayoutIndex(),
      pageLayoutBottomNavigation(),
      pageLayoutCategoryConfig()
    ]);
    for (const key of ['indexBanner', 'indexTable', 'indexMenu', 'indexNews', 'userMenu', 'userBanner']) {
      state[key] = normalizeList(layout?.[key] || []);
    }
    state.bottomNavigation = normalizeList(bottom?.bottomNavigationList || []);
    isCustom.value = Number(bottom?.isCustom || 0);
    categoryConfig.categoryPageConfig = ['1', '2', '3'].includes(String(category?.categoryPageConfig || '1'))
      ? String(category?.categoryPageConfig || '1')
      : '1';
    categoryConfig.isShowCategory = Number(category?.isShowCategory ?? 1) ? 1 : 0;
  } finally {
    loading.value = false;
  }
}

function normalizeList(list) {
  return list.map((item, index) => ({
    ...item,
    status: item.status !== false,
    sort: item.sort ?? index,
    localKey: `${item.id || 'new'}-${index}-${Math.random().toString(16).slice(2)}`
  }));
}

function visibleList(key) {
  return (state[key] || []).filter((item) => item.status);
}

function addItem() {
  const list = currentList.value;
  const template = list[0] || {};
  const item = {
    tempid: template.tempid || null,
    name: '',
    pic: '',
    url: '',
    link: '',
    checked: '',
    unchecked: '',
    info: '',
    status: true,
    sort: list.length,
    localKey: `new-${Date.now()}`
  };
  list.push(item);
}

function removeItem(index) {
  currentList.value.splice(index, 1);
}

function handleDragStart(index, event) {
  dragState.from = index;
  dragState.over = index;
  event?.dataTransfer?.setData('text/plain', String(index));
  if (event?.dataTransfer) event.dataTransfer.effectAllowed = 'move';
}

function handleDragOver(index) {
  if (dragState.from === null) return;
  dragState.over = index;
}

function handleDrop(index) {
  if (dragState.from === null || dragState.from === index) {
    handleDragEnd();
    return;
  }
  const list = currentList.value;
  const [moved] = list.splice(dragState.from, 1);
  list.splice(index, 0, moved);
  handleDragEnd();
}

function handleDragEnd() {
  dragState.from = null;
  dragState.over = null;
}

async function openAttachmentSelector(item, field) {
  attachmentTarget.value = item;
  attachmentField.value = field;
  attachmentDialogVisible.value = true;
  attachmentQuery.page = 1;
  attachmentQuery.pid = 0;
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
  if (!attachmentTarget.value || !attachmentField.value) return;
  attachmentTarget.value[attachmentField.value] = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
}

function clearImageField(item, field) {
  item[field] = '';
}

function linkField() {
  return activeTab.value === 'bottomNavigation' ? 'link' : 'url';
}

function getItemLink(item) {
  return item?.[linkField()] || item?.link || item?.url || '';
}

function setItemLink(item, value) {
  if (!item) return;
  item[linkField()] = value || '';
}

function openLinkSelector(item) {
  linkTarget.value = item;
  linkKeyword.value = '';
  dynamicLinkQuery.keyword = '';
  dynamicLinkQuery.page = 1;
  selectedLink.value = getItemLink(item);
  customLink.value = selectedLink.value;
  activeLinkGroup.value = 'mall';
  linkDialogVisible.value = true;
}

function confirmLinkSelector() {
  const value = activeLinkGroup.value === 'custom' ? customLink.value : selectedLink.value;
  setItemLink(linkTarget.value, value);
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

async function saveCurrent() {
  const key = activeTab.value;
  if (key === 'categoryConfig') {
    saving.value = true;
    try {
      await pageLayoutSave(key, {
        categoryPageConfig: categoryConfig.categoryPageConfig,
        isShowCategory: categoryConfig.isShowCategory
      });
      ElMessage.success('保存成功');
      await loadData();
    } finally {
      saving.value = false;
    }
    return;
  }
  const list = currentList.value.map((item, index) => ({ ...item, sort: index }));
  const visibleCount = list.filter((item) => item.status).length;
  if (visibleCount < minCount.value) {
    ElMessage.warning(`设置数据不能小于${minCount.value}条`);
    return;
  }
  saving.value = true;
  try {
    const payload = key === 'bottomNavigation'
      ? { bottomNavigationList: list, isCustom: isCustom.value }
      : { [key]: list };
    await pageLayoutSave(key, payload);
    ElMessage.success('保存成功');
    await loadData();
  } finally {
    saving.value = false;
  }
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}
</script>

<style scoped>
.layout-card {
  border-radius: 4px;
}

.layout-shell {
  display: grid;
  grid-template-columns: 150px minmax(280px, 360px) 1fr;
  min-height: 640px;
}

.layout-tabs {
  border-right: 1px solid #ebeef5;
}

.layout-tabs button {
  display: block;
  width: 100%;
  height: 48px;
  border: 0;
  border-left: 3px solid transparent;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font: inherit;
  text-align: left;
  padding: 0 16px;
}

.layout-tabs button.active {
  border-left-color: var(--prev-color-primary, #e93323);
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
}

.phone-preview {
  width: 320px;
  min-height: 580px;
  margin: 0 auto;
  border: 1px solid #ebeef5;
  background: #f7f8fa;
  padding: 14px;
}

.model-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
}

.model-header div,
.model-header span {
  border-radius: 4px;
  background: #fff;
  padding: 8px 12px;
  color: #909399;
}

.model-header span {
  flex: 1;
}

.preview-banner {
  margin: 12px 0;
  height: 86px;
  overflow: hidden;
  border-radius: 4px;
  background: #fff;
}

.preview-banner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-news {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px;
  border-radius: 4px;
  background: #fff;
}

.preview-news p {
  margin: 0;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-menu {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 8px;
  background: #fff;
  border-radius: 4px;
  padding: 12px 8px;
}

.preview-menu div {
  text-align: center;
}

.preview-menu img,
.preview-menu span {
  display: block;
  width: 30px;
  height: 30px;
  margin: 0 auto 6px;
  border-radius: 6px;
  background: #eef0f3;
  object-fit: cover;
}

.preview-menu p,
.preview-foot p {
  margin: 0;
  color: #606266;
  font-size: 12px;
}

.preview-product-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.preview-product-tabs button {
  min-height: 58px;
  border: 0;
  border-radius: 4px;
  background: #fff;
  padding: 8px;
  text-align: left;
}

.preview-product-tabs strong,
.preview-product-tabs small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-product-tabs strong {
  color: #303133;
  font-size: 13px;
}

.preview-product-tabs small {
  margin-top: 5px;
  color: #909399;
  font-size: 11px;
}

.user-head {
  height: 120px;
  border-radius: 4px;
  background: #e93323;
  padding: 18px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.user-card p {
  margin: 0 0 4px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
}

.phone-preview h4 {
  margin: 12px 0 8px;
  font-size: 14px;
}

.preview-spacer {
  display: flex;
  height: 500px;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.preview-foot {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid #ebeef5;
  background: #fff;
  padding: 8px 0;
  text-align: center;
}

.preview-foot img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.category-preview {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 10px;
  min-height: 520px;
}

.category-preview.style-2,
.category-preview.style-3 {
  grid-template-columns: 1fr;
}

.category-sidebar {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 8px 0;
}

.category-sidebar span {
  display: block;
  height: 34px;
  line-height: 34px;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  text-align: center;
  font-size: 12px;
}

.category-sidebar span.active {
  color: var(--prev-color-primary, #e93323);
  font-weight: 600;
}

.category-content {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 12px;
}

.category-search {
  height: 34px;
  line-height: 34px;
  border-radius: 17px;
  background: #fff;
  color: #909399;
  text-align: center;
  font-size: 12px;
}

.category-banner {
  height: 96px;
  border-radius: 6px;
  background: linear-gradient(135deg, #f56c6c, #fbc4c4);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.category-preview.style-2 .category-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.category-preview.style-3 .category-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.category-grid span {
  display: block;
  height: 86px;
  border-radius: 6px;
  background: #fff;
}

.category-preview.style-3 .category-grid span:nth-child(2n) {
  height: 122px;
}

.category-config-panel {
  padding: 18px 18px 2px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.config-panel {
  border-left: 1px solid #ebeef5;
  padding: 0 20px 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0 0 6px;
  font-size: 17px;
}

.panel-head p {
  margin: 0;
  color: #909399;
}

.custom-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.layout-item {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}

.layout-item.dragging {
  opacity: 0.55;
}

.layout-item.drag-over {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
}

.item-sort {
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 2px;
  border: 0;
  border-radius: 4px;
  background: #f5f7fa;
  color: #606266;
  cursor: grab;
}

.item-sort:active {
  cursor: grabbing;
}

.drag-lines {
  width: 14px;
  height: 8px;
  background:
    linear-gradient(#909399, #909399) 0 0 / 14px 1px no-repeat,
    linear-gradient(#909399, #909399) 0 50% / 14px 1px no-repeat,
    linear-gradient(#909399, #909399) 0 100% / 14px 1px no-repeat;
}

.item-sort em {
  font-style: normal;
  font-size: 11px;
  line-height: 1;
}

.item-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.image-field-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.image-field-row :deep(.el-input) {
  flex: 1;
}

.link-field-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.link-field-row :deep(.el-input) {
  flex: 1;
}

.muted {
  color: #909399;
  font-size: 12px;
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

.link-picker-toolbar {
  margin-bottom: 12px;
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

.link-picker-card span {
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

.dynamic-link-table {
  width: 100%;
}

.dynamic-link-image {
  width: 46px;
  height: 46px;
  display: block;
  background: #f5f7fa;
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

.attachment-picker-card img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  background: #f5f7fa;
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

@media (max-width: 1100px) {
  .layout-shell {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .layout-tabs {
    display: flex;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
  }

  .layout-tabs button {
    min-width: 130px;
    border-left: 0;
    border-bottom: 3px solid transparent;
  }

  .config-panel {
    border-left: 0;
    padding: 0;
  }
}
</style>
