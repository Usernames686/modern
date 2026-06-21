<template>
  <main
    v-if="!isAuthed"
    class="page-account"
    :style="{ backgroundImage: `url(${loginPic.backgroundImage || fallbackBg})` }"
  >
    <section class="container" :class="fullWidth > 768 ? 'containerSamll' : 'containerBig'">
      <div v-if="fullWidth > 768" class="swiperPross">
        <img :src="activeBanner" alt="" />
      </div>

      <div class="index_from page-account-container">
        <div class="page-account-top">
          <div class="page-account-top-logo">
            <img :src="loginPic.loginLogo || fallbackLogo" alt="logo" />
          </div>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="on"
          label-position="left"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="account">
            <el-input
              v-model="loginForm.account"
              :prefix-icon="User"
              placeholder="用户名"
              name="username"
              type="text"
              tabindex="1"
              autocomplete="on"
              @blur="handleAccountBlur"
            />
          </el-form-item>

          <el-form-item prop="pwd">
            <el-input
              v-model="loginForm.pwd"
              :prefix-icon="Lock"
              :type="passwordType"
              placeholder="密码"
              name="pwd"
              tabindex="2"
              autocomplete="on"
            />
            <button class="show-pwd" type="button" @click="showPwd">
              <el-icon><View v-if="passwordType === 'password'" /><Hide v-else /></el-icon>
            </button>
          </el-form-item>

          <div class="acea-row">
            <el-button
              :loading="loginLoading"
              type="primary"
              class="login-submit"
              :disabled="loginDisabled"
              @click="handleLogin"
            >
              登录
            </el-button>
          </div>
        </el-form>
      </div>
    </section>
  </main>

  <el-container v-else class="legacy-layout">
    <el-aside width="220px" class="legacy-aside">
      <div class="legacy-logo">
        <img :src="loginPic.logo || loginPic.loginLogo || fallbackLogo" alt="" />
      </div>
      <el-menu :default-active="activeMenuId" class="legacy-menu">
        <LegacyMenuItems :items="menuTree" @select="handleMenuSelect" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="legacy-header">
        <div class="breadcrumb">{{ breadcrumbText }}</div>
        <div class="header-user">
          <span>{{ adminInfo?.realName || adminInfo?.account }}</span>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <div class="tags-view">
        <span class="tag active">{{ currentViewTitle }}</span>
      </div>

      <el-main class="legacy-main">
        <DashboardHome v-if="currentPath === '/dashboard' || currentPath === '/'" />
        <section v-else-if="currentPath.startsWith('/store/list/creatProduct')" class="direct-form-page">
          <div class="direct-form-header">
            <h2>{{ productDirectTitle }}</h2>
            <el-button @click="navigateTo('/store/index')">返回列表</el-button>
          </div>
          <ProductDetailDrawer
            v-if="productDirectReadonly && productDirectId"
            :product-id="productDirectId"
          />
          <ProductBasicEditor
            v-else
            :product-id="productDirectId"
            @success="navigateTo('/store/index')"
            @cancel="navigateTo('/store/index')"
          />
        </section>
        <ProductList v-else-if="currentMenu?.component === '/store/index'" />
        <ProductCategoryList v-else-if="currentMenu?.component === '/store/sort'" />
        <ProductRuleList v-else-if="currentMenu?.component === '/store/attr'" />
        <ProductCommentList v-else-if="currentMenu?.component === '/store/comment'" />
        <OrderList v-else-if="currentMenu?.component === '/order/index'" />
        <UserList v-else-if="currentMenu?.component === '/user/index'" />
        <UserGroupList v-else-if="currentMenu?.component === '/user/label'" type="tag" />
        <UserGroupList v-else-if="currentMenu?.component === '/user/group'" type="group" />
        <UserGradeList v-else-if="currentMenu?.component === '/user/grade'" />
        <AttachmentManager v-else-if="currentMenu?.component === '/maintain/picture'" />
        <CouponForm
          v-else-if="currentPath.startsWith('/marketing/coupon/list/save')"
          :copy-id="Number(currentPath.split('/').filter(Boolean)[3] || 0) || null"
          @back="navigateTo('/marketing/coupon/list')"
          @saved="navigateTo('/marketing/coupon/list')"
        />
        <CouponList v-else-if="currentMenu?.component === '/marketing/coupon/list'" />
        <CouponUserRecord v-else-if="currentMenu?.component === '/marketing/coupon/record'" />
        <IntegralManager
          v-else-if="
            currentPath === '/marketing/integral/integralconfig'
            || currentPath === '/marketing/integral/integrallog'
          "
          :path="currentPath"
        />
        <MarketingWorkbench
          v-else-if="
            currentPath === '/marketing/index'
            || currentPath === '/marketing/coupon/index'
            || currentPath === '/marketing/seckill/index'
            || currentPath === '/marketing/bargain/index'
            || currentPath === '/marketing/groupBuy/index'
          "
          :path="currentPath"
        />
        <SeckillConfigManager v-else-if="currentPath === '/marketing/seckill/config'" />
        <SeckillProductList
          v-else-if="currentPath === '/marketing/seckill/list' || currentPath.startsWith('/marketing/seckill/list/')"
          :path="currentPath"
        />
        <SeckillProductForm
          v-else-if="currentPath.startsWith('/marketing/seckill/creatSeckill')"
          :path="currentPath"
        />
        <BargainProductForm
          v-else-if="currentPath.startsWith('/marketing/bargain/creatBargain')"
          :path="currentPath"
        />
        <BargainManager
          v-else-if="
            currentPath === '/marketing/bargain/bargainGoods'
            || currentPath === '/marketing/bargain/bargainList'
          "
          :path="currentPath"
        />
        <CombinationProductForm
          v-else-if="currentPath.startsWith('/marketing/groupBuy/creatGroup')"
          :path="currentPath"
        />
        <CombinationManager
          v-else-if="
            currentPath === '/marketing/groupBuy/groupGoods'
            || currentPath === '/marketing/groupBuy/groupList'
          "
          :path="currentPath"
        />
        <ActivityStyleManager
          v-else-if="
            currentPath === '/marketing/border/list'
            || currentPath.startsWith('/marketing/border/add')
            || currentPath === '/marketing/atmosphere/list'
            || currentPath.startsWith('/marketing/atmosphere/add')
          "
          :path="currentPath"
        />
        <VideoChannelManager
          v-else-if="
            currentPath === '/marketing/videoChannel/list'
            || currentPath === '/marketing/videoChannel/draftList'
          "
          :path="currentPath"
        />
        <FinancialRechargeList v-else-if="currentPath === '/financial/record/charge'" />
        <FinancialMonitorList v-else-if="currentPath === '/financial/record/monitor'" />
        <FinancialBrokerageList v-else-if="currentPath === '/financial/brokerage'" />
        <FinancialExtractList v-else-if="currentPath === '/financial/commission/template'" />
        <DistributionPromoterList v-else-if="currentPath === '/distribution/index' || currentPath === '/promotion/manager'" />
        <FinancialBrokerageList
          v-else-if="currentPath === '/promotion/record'"
          :initial-type="1"
          :lock-type="true"
        />
        <DistributionConfig v-else-if="currentPath === '/distribution/distributionconfig'" />
        <StorePickupPointList v-else-if="currentMenu?.component === '/operation/deliverGoods/takeGoods/deliveryAddress'" />
        <StoreStaffList v-else-if="currentMenu?.component === '/operation/deliverGoods/takeGoods/collateUser'" />
        <StoreWriteOffOrderList v-else-if="currentMenu?.component === '/operation/deliverGoods/takeGoods/collateOrder'" />
        <ExpressCompanyList v-else-if="currentMenu?.component === '/maintain/logistics/companyList'" />
        <CityDataList v-else-if="currentMenu?.component === '/maintain/logistics/cityList'" />
        <ShippingTemplateList v-else-if="currentMenu?.component === '/operation/deliverGoods/freightSet'" />
        <ArticleCategoryList v-else-if="currentMenu?.component === '/content/classifManager'" />
        <ArticleManager
          v-else-if="currentMenu?.component === '/content/articleManager' || currentPath.startsWith('/content/articleCreat')"
          :mode="currentPath.startsWith('/content/articleCreat') ? 'form' : 'list'"
          :article-id="currentPath.split('/').filter(Boolean)[2] || ''"
        />
        <PageDiyManager v-else-if="currentPath === '/design/devise' || currentPath.startsWith('/design/devise/')" />
        <ThemeColorManager v-else-if="currentPath === '/design/theme' || currentPath === '/operation/design/theme'" />
        <PageLayoutManager v-else-if="currentPath === '/design/viewDesign' || currentPath === '/operation/design/viewDesign'" />
        <StatisticProduct
          v-else-if="
            currentPath === '/statistic/product'
            || currentPath === '/statistic/product/visualization'
            || currentPath === '/statistic/product/tableData'
          "
        />
        <StatisticUser v-else-if="currentPath === '/statistic/statuser'" />
        <StatisticTrade v-else-if="currentPath === '/statistic/transaction'" />
        <SystemSetting v-else-if="currentMenu?.component === '/operation/setting'" />
        <SystemNotification v-else-if="currentMenu?.component === '/operation/notification'" />
        <SystemRoleManager
          v-else-if="
            currentMenu?.component === '/operation/roleManager/identityManager'
            || currentPath === '/operation//roleManager/identityManager'
          "
        />
        <SystemAdminManager
          v-else-if="
            currentMenu?.component === '/operation/roleManager/adminList'
            || currentPath === '/operation//roleManager/adminList'
          "
        />
        <SystemMenuRules
          v-else-if="
            currentMenu?.component === '/operation/roleManager/promiseRules'
            || currentPath === '/operation//roleManager/promiseRules'
          "
        />
        <SmsManager
          v-else-if="
            currentPath === '/operation/onePass'
            || currentPath === '/operation/onePassConfig'
            || currentPath.startsWith('/operation/systemSms/')
          "
          :path="currentPath"
        />
        <ConfigCategoryList v-else-if="currentMenu?.component === '/maintain/devconfiguration/configCategory'" />
        <CombinedDataManager v-else-if="currentMenu?.component === '/maintain/devconfiguration/combineddata'" />
        <FormConfigManager v-else-if="currentMenu?.component === '/maintain/devconfiguration/formConfig'" />
        <ClearCacheManager v-else-if="currentPath === '/maintain/clearCache'" />
        <AuthCrmebInfo v-else-if="currentPath === '/maintain/authCRMEB'" />
        <SiteList v-else-if="currentPath === '/sites/index'" />
        <ScheduleJobManager
          v-else-if="currentMenu?.component === '/maintain/schedule/list' || currentMenu?.component === '/maintain/schedule/logList'"
          :initial-tab="currentMenu?.component === '/maintain/schedule/logList' ? 'logs' : 'jobs'"
        />
        <WechatReplyForm
          v-else-if="
            currentPath === '/appSetting/publicAccount/wxReply/follow'
            || currentPath === '/appSetting/publicAccount/wxReply/replyIndex'
            || currentPath === '/appSetting/publicAccount/wxReply/default'
            || currentPath.startsWith('/appSetting/publicAccount/wxReply/keyword/save')
          "
          :path="currentPath"
        />
        <WechatKeywordReplyList v-else-if="currentPath === '/appSetting/publicAccount/wxReply/keyword'" />
        <WechatMenuManager
          v-else-if="
            currentPath === '/appSetting/publicAccount/wxMenus'
            || currentPath === '/appSetting//publicAccount/wxMenus'
          "
        />
        <WechatTemplateManager
          v-else-if="
            currentPath === '/appSetting/publicAccount/wxTemplate'
            || currentPath === '/appSetting/publicAccount/template'
            || currentPath === '/appSetting/publicAccount/wxSubscribe'
            || currentPath === '/appSetting//publicAccount/template/1'
            || currentPath === '/appSetting/publicRoutine/template/0'
            || currentPath === '/appSetting/publicRoutine/routineTemplate'
            || currentPath === '/appSetting/publicRoutine/publicRoutineTemplate'
          "
          :path="currentPath"
        />
        <MobileOrderCancellation v-else-if="currentPath.startsWith('/javaMobile/orderCancellation')" />
        <MobileOrderList v-else-if="currentPath.startsWith('/javaMobile/orderList')" />
        <MobileOrderDetail v-else-if="currentPath.startsWith('/javaMobile/orderDetail')" />
        <MobileOrderDelivery v-else-if="currentPath.startsWith('/javaMobile/orderDelivery')" />
        <MobileOrderStatisticsDetail v-else-if="currentPath.startsWith('/javaMobile/orderStatisticsDetail')" />
        <MobileOrderStatistics v-else-if="currentPath === '/javaMobile/orderStatistics' || currentPath === '/javaMobile/index'" />
        <section v-else class="module-placeholder">
          <h2>{{ currentMenu?.name || '主页' }}</h2>
          <p>当前入口暂无可直接展示的页面，请从左侧菜单选择具体功能。</p>
          <el-table :data="statusRows" border>
            <el-table-column prop="name" label="项目" width="180" />
            <el-table-column prop="value" label="状态" />
          </el-table>
        </section>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Hide, Lock, User, View } from '@element-plus/icons-vue';
import fallbackBg from './assets/imgs/bg.jpg';
import fallbackLogo from './assets/imgs/index_logo.png';
import fallbackBanner from './assets/imgs/pc1.jpg';
import LegacyMenuItems from './components/LegacyMenuItems.vue';
import DashboardHome from './components/DashboardHome.vue';
import ProductList from './components/ProductList.vue';
import ProductBasicEditor from './components/ProductBasicEditor.vue';
import ProductDetailDrawer from './components/ProductDetailDrawer.vue';
import ProductCategoryList from './components/ProductCategoryList.vue';
import ProductRuleList from './components/ProductRuleList.vue';
import ProductCommentList from './components/ProductCommentList.vue';
import OrderList from './components/OrderList.vue';
import UserList from './components/UserList.vue';
import UserGroupList from './components/UserGroupList.vue';
import UserGradeList from './components/UserGradeList.vue';
import AttachmentManager from './components/AttachmentManager.vue';
import CouponForm from './components/CouponForm.vue';
import CouponList from './components/CouponList.vue';
import CouponUserRecord from './components/CouponUserRecord.vue';
import IntegralManager from './components/IntegralManager.vue';
import MarketingWorkbench from './components/MarketingWorkbench.vue';
import SeckillConfigManager from './components/SeckillConfigManager.vue';
import SeckillProductList from './components/SeckillProductList.vue';
import SeckillProductForm from './components/SeckillProductForm.vue';
import BargainProductForm from './components/BargainProductForm.vue';
import BargainManager from './components/BargainManager.vue';
import CombinationProductForm from './components/CombinationProductForm.vue';
import CombinationManager from './components/CombinationManager.vue';
import ActivityStyleManager from './components/ActivityStyleManager.vue';
import VideoChannelManager from './components/VideoChannelManager.vue';
import FinancialBrokerageList from './components/FinancialBrokerageList.vue';
import FinancialExtractList from './components/FinancialExtractList.vue';
import FinancialMonitorList from './components/FinancialMonitorList.vue';
import FinancialRechargeList from './components/FinancialRechargeList.vue';
import DistributionConfig from './components/DistributionConfig.vue';
import DistributionPromoterList from './components/DistributionPromoterList.vue';
import StorePickupPointList from './components/StorePickupPointList.vue';
import StoreStaffList from './components/StoreStaffList.vue';
import StoreWriteOffOrderList from './components/StoreWriteOffOrderList.vue';
import ExpressCompanyList from './components/ExpressCompanyList.vue';
import CityDataList from './components/CityDataList.vue';
import ShippingTemplateList from './components/ShippingTemplateList.vue';
import ArticleCategoryList from './components/ArticleCategoryList.vue';
import ArticleManager from './components/ArticleManager.vue';
import PageDiyManager from './components/PageDiyManager.vue';
import ThemeColorManager from './components/ThemeColorManager.vue';
import PageLayoutManager from './components/PageLayoutManager.vue';
import StatisticProduct from './components/StatisticProduct.vue';
import StatisticTrade from './components/StatisticTrade.vue';
import StatisticUser from './components/StatisticUser.vue';
import SystemSetting from './components/SystemSetting.vue';
import SystemNotification from './components/SystemNotification.vue';
import SystemRoleManager from './components/SystemRoleManager.vue';
import SystemAdminManager from './components/SystemAdminManager.vue';
import SystemMenuRules from './components/SystemMenuRules.vue';
import SmsManager from './components/SmsManager.vue';
import ConfigCategoryList from './components/ConfigCategoryList.vue';
import CombinedDataManager from './components/CombinedDataManager.vue';
import FormConfigManager from './components/FormConfigManager.vue';
import ClearCacheManager from './components/ClearCacheManager.vue';
import AuthCrmebInfo from './components/AuthCrmebInfo.vue';
import SiteList from './components/SiteList.vue';
import ScheduleJobManager from './components/ScheduleJobManager.vue';
import MobileOrderCancellation from './components/MobileOrderCancellation.vue';
import MobileOrderStatistics from './components/MobileOrderStatistics.vue';
import MobileOrderList from './components/MobileOrderList.vue';
import MobileOrderDetail from './components/MobileOrderDetail.vue';
import MobileOrderDelivery from './components/MobileOrderDelivery.vue';
import MobileOrderStatisticsDetail from './components/MobileOrderStatisticsDetail.vue';
import WechatKeywordReplyList from './components/WechatKeywordReplyList.vue';
import WechatMenuManager from './components/WechatMenuManager.vue';
import WechatReplyForm from './components/WechatReplyForm.vue';
import WechatTemplateManager from './components/WechatTemplateManager.vue';
import { TOKEN_KEY, accountDetection, getAdminInfo, getLoginPic, getMenus, health, login, logout } from './api';

const loginFormRef = ref();
const loginLoading = ref(false);
const loginDisabled = ref(false);
const isAuthed = ref(Boolean(localStorage.getItem(TOKEN_KEY)));
const adminInfo = ref(null);
const healthInfo = ref(null);
const menuTree = ref([]);
const currentMenu = ref(null);
const currentPath = ref(window.location.pathname);
const passwordType = ref('password');
const fullWidth = ref(document.body.clientWidth);
const errorsNumber = ref(0);
const bannerIndex = ref(0);
let bannerTimer;

const loginPic = reactive({
  banner: [],
  backgroundImage: '',
  logo: '',
  loginLogo: '',
  siteName: ''
});

const loginForm = reactive({
  account: 'admin',
  pwd: '123456',
  captchaVO: {}
});

const loginRules = {
  account: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const activeMenuId = computed(() => currentMenu.value?.id ? String(currentMenu.value.id) : '');
const breadcrumbText = computed(() => {
  if (currentPath.value.startsWith('/javaMobile/orderList')) return '移动端管理 / 订单列表';
  if (currentPath.value.startsWith('/javaMobile/orderDetail')) return '移动端管理 / 订单详情';
  if (currentPath.value.startsWith('/javaMobile/orderDelivery')) return '移动端管理 / 订单发货';
  if (currentPath.value.startsWith('/javaMobile/orderStatisticsDetail')) return '移动端管理 / 订单数据统计';
  if (currentPath.value.startsWith('/javaMobile/orderCancellation')) return '移动端管理 / 订单核销';
  if (currentPath.value.startsWith('/javaMobile/orderStatistics')) return '移动端管理 / 订单统计';
  const path = findMenuPath(menuTree.value, currentMenu.value?.id);
  return path.length ? path.map((item) => item.name).join(' / ') : '首页 / 主页';
});
const currentViewTitle = computed(() => {
  if (currentPath.value.startsWith('/javaMobile/orderList')) return '订单列表';
  if (currentPath.value.startsWith('/javaMobile/orderDetail')) return '订单详情';
  if (currentPath.value.startsWith('/javaMobile/orderDelivery')) return '订单发货';
  if (currentPath.value.startsWith('/javaMobile/orderStatisticsDetail')) return '订单数据统计';
  if (currentPath.value.startsWith('/javaMobile/orderCancellation')) return '订单核销';
  if (currentPath.value.startsWith('/javaMobile/orderStatistics')) return '订单统计';
  if (currentPath.value.startsWith('/store/list/creatProduct')) return productDirectTitle.value;
  return currentMenu.value?.name || '主页';
});
const bannerList = computed(() => loginPic.banner?.length ? loginPic.banner : [{ pic: fallbackBanner }]);
const activeBanner = computed(() => bannerList.value[bannerIndex.value % bannerList.value.length]?.pic || fallbackBanner);
const productDirectParts = computed(() => currentPath.value.split('/').filter(Boolean));
const productDirectId = computed(() => Number(productDirectParts.value[3] || 0) || 0);
const productDirectReadonly = computed(() => productDirectParts.value[4] === '1');
const productDirectTitle = computed(() => {
  if (!productDirectId.value) return '添加商品';
  return productDirectReadonly.value ? `商品详情-${productDirectId.value}` : `编辑商品-${productDirectId.value}`;
});
const statusRows = computed(() => [
  { name: '模块状态', value: '请选择子菜单进入具体功能' },
  { name: '页面路径', value: currentMenu.value?.component || '-' },
  { name: '权限标识', value: currentMenu.value?.perms || '-' },
  { name: '后端服务', value: healthInfo.value?.application || '-' },
  { name: '当前账号', value: adminInfo.value?.account || '-' }
]);

const migratedRouteMeta = {
  '/dashboard': { name: '控制台' },
  '/store/index': { name: '商品列表' },
  '/store/list/creatProduct': { name: '添加商品' },
  '/store/sort': { name: '商品分类' },
  '/store/attr': { name: '商品规格' },
  '/store/comment': { name: '商品评论' },
  '/order/index': { name: '订单列表' },
  '/user/index': { name: '用户列表' },
  '/user/label': { name: '用户标签' },
  '/user/group': { name: '用户分组' },
  '/user/grade': { name: '用户等级' },
  '/maintain/picture': { name: '素材管理' },
  '/marketing/coupon/list': { name: '优惠券列表' },
  '/marketing/coupon/list/save': { name: '添加优惠券' },
  '/marketing/coupon/record': { name: '领取记录' },
  '/marketing/index': { name: '营销中心' },
  '/marketing/coupon/index': { name: '优惠券' },
  '/marketing/integral/integralconfig': { name: '积分配置' },
  '/marketing/integral/integrallog': { name: '积分日志' },
  '/marketing/seckill/index': { name: '秒杀管理' },
  '/marketing/seckill/config': { name: '秒杀配置' },
  '/marketing/seckill/list': { name: '秒杀商品' },
  '/marketing/seckill/creatSeckill': { name: '添加秒杀商品' },
  '/marketing/bargain/index': { name: '砍价管理' },
  '/marketing/bargain/bargainGoods': { name: '砍价商品' },
  '/marketing/bargain/bargainList': { name: '砍价列表' },
  '/marketing/groupBuy/index': { name: '拼团管理' },
  '/marketing/groupBuy/groupGoods': { name: '拼团商品' },
  '/marketing/groupBuy/groupList': { name: '拼团列表' },
  '/marketing/border/list': { name: '活动边框列表' },
  '/marketing/border/add': { name: '添加活动边框' },
  '/marketing/atmosphere/list': { name: '氛围列表' },
  '/marketing/atmosphere/add': { name: '添加活动氛围' },
  '/marketing/videoChannel/list': { name: '视频号商品列表' },
  '/marketing/videoChannel/draftList': { name: '视频号草稿列表' },
  '/financial/record/charge': { name: '充值记录' },
  '/financial/record/monitor': { name: '资金监控' },
  '/financial/brokerage': { name: '佣金记录' },
  '/financial/commission/template': { name: '申请提现' },
  '/distribution/index': { name: '分销员管理' },
  '/distribution/distributionconfig': { name: '分销配置' },
  '/promotion/manager': { name: '推广管理' },
  '/promotion/record': { name: '推广记录' },
  '/operation/deliverGoods/takeGoods/deliveryAddress': { name: '提货点' },
  '/operation/deliverGoods/takeGoods/collateUser': { name: '核销员' },
  '/operation/deliverGoods/takeGoods/collateOrder': { name: '核销订单' },
  '/maintain/logistics/companyList': { name: '物流公司' },
  '/maintain/logistics/cityList': { name: '城市数据' },
  '/operation/deliverGoods/freightSet': { name: '运费模板' },
  '/content/classifManager': { name: '文章分类' },
  '/content/articleManager': { name: '文章管理' },
  '/content/articleCreat': { name: '添加文章' },
  '/design/devise': { name: '微页面' },
  '/design/theme': { name: '一键换色' },
  '/design/viewDesign': { name: '页面设计' },
  '/operation/design/theme': { name: '一键换色' },
  '/operation/design/viewDesign': { name: '页面设计' },
  '/statistic/product': { name: '商品统计' },
  '/statistic/product/visualization': { name: '商品统计' },
  '/statistic/product/tableData': { name: '商品排行' },
  '/statistic/statuser': { name: '用户统计' },
  '/statistic/transaction': { name: '交易统计' },
  '/operation/setting': { name: '系统设置' },
  '/operation/notification': { name: '消息通知' },
  '/operation/roleManager/identityManager': { name: '角色管理' },
  '/operation/roleManager/adminList': { name: '管理员列表' },
  '/operation/roleManager/promiseRules': { name: '权限规则' },
  '/operation//roleManager/identityManager': { name: '角色管理' },
  '/operation//roleManager/adminList': { name: '管理员列表' },
  '/operation//roleManager/promiseRules': { name: '权限规则' },
  '/operation/onePass': { name: '一号通' },
  '/operation/onePassConfig': { name: '一号通配置' },
  '/operation/systemSms/config': { name: '短信账户' },
  '/operation/systemSms/template': { name: '短信模板' },
  '/operation/systemSms/pay': { name: '短信购买' },
  '/operation/systemSms/message': { name: '短信开关' },
  '/operation/systemSms/record': { name: '短信发送记录' },
  '/operation/systemSms/record': { name: '短信发送记录' },
  '/maintain/devconfiguration/configCategory': { name: '配置分类' },
  '/maintain/devconfiguration/combineddata': { name: '组合数据' },
  '/maintain/devconfiguration/formConfig': { name: '表单配置' },
  '/maintain/clearCache': { name: '缓存清除' },
  '/maintain/authCRMEB': { name: '申请授权' },
  '/sites/index': { name: '站点列表' },
  '/maintain/schedule/list': { name: '定时任务' },
  '/maintain/schedule/logList': { name: '任务日志' },
  '/appSetting/publicAccount/wxReply/follow': { name: '微信关注回复' },
  '/appSetting/publicAccount/wxReply/replyIndex': { name: '无匹配回复' },
  '/appSetting/publicAccount/wxReply/default': { name: '无匹配回复' },
  '/appSetting/publicAccount/wxReply/keyword': { name: '关键字回复' },
  '/appSetting/publicAccount/wxReply/keyword/save': { name: '添加关键字' },
  '/appSetting/publicAccount/wxMenus': { name: '微信菜单' },
  '/appSetting//publicAccount/wxMenus': { name: '微信菜单' },
  '/appSetting/publicAccount/wxTemplate': { name: '模板消息' },
  '/appSetting/publicAccount/template': { name: '模板消息' },
  '/appSetting/publicAccount/wxSubscribe': { name: '小程序订阅消息' },
  '/appSetting//publicAccount/template/1': { name: '微信模板' },
  '/appSetting/publicRoutine/template/0': { name: '小程序订阅消息' },
  '/appSetting/publicRoutine/routineTemplate': { name: '我的模板' },
  '/appSetting/publicRoutine/publicRoutineTemplate': { name: '公共模板' }
};

const parentRouteFallbacks = {
  '/store': '/store/index',
  '/order': '/order/index',
  '/user': '/user/index',
  '/content': '/content/classifManager',
  '/appSetting': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/wxAccount': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/publicAccount': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/publicAccount/wxReply': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/publicRoutine': '/appSetting/publicRoutine/template/0',
  '/marketing': '/marketing/index',
  '/marketing/integral': '/marketing/integral/integralconfig',
  '/marketing/seckill': '/marketing/seckill/index',
  '/marketing/bargain': '/marketing/bargain/index',
  '/marketing/groupBuy': '/marketing/groupBuy/index',
  '/marketing/coupon': '/marketing/coupon/index',
  '/marketing/videoChannel': '/marketing/videoChannel/list',
  '/distribution': '/distribution/distributionconfig',
  '/promotion': '/promotion/manager',
  '/financial': '/financial/commission/template',
  '/financial/commission': '/financial/commission/template',
  '/financial/record': '/financial/record/charge',
  '/design': '/design/devise',
  '/statistic': '/statistic/product',
  '/javaMobile': '/javaMobile/index',
  '/operation': '/operation/setting',
  '/operation/design': '/operation/design/viewDesign',
  '/operation/roleManager': '/operation/roleManager/identityManager',
  '/operation/deliverGoods': '/operation/deliverGoods/takeGoods/collateUser',
  '/operation/deliverGoods/takeGoods': '/operation/deliverGoods/takeGoods/collateUser',
  '/maintain': '/maintain/devconfiguration/configCategory',
  '/maintain//devconfiguration': '/maintain/devconfiguration/configCategory',
  '/maintain/devconfiguration': '/maintain/devconfiguration/configCategory',
  '/maintain/logistics': '/maintain/logistics/cityList',
  '/operation/maintain/schedule': '/maintain/schedule/list',
  '/maintain/schedule': '/maintain/schedule/list',
  '/sites': '/sites/index'
};

const routeAliases = {
  '/order/list': '/order/index',
  '/store/creatStore': '/store/list/creatProduct',
  '/store/creatStore/index': '/store/list/creatProduct',
  '/store/storeAttr/index': '/store/attr',
  '/store/storeComment/index': '/store/comment',
  '/store/sort/index': '/store/sort',
  '/user/list/index': '/user/index',
  '/user/list/edit': '/user/index',
  '/user/list/userDetails': '/user/index',
  '/user/list/level': '/user/grade',
  '/user/grade/creatGrade': '/user/grade',
  '/user/grade/index': '/user/grade',
  '/user/group/index': '/user/group',
  '/user/notice/index': '/operation/notification',
  '/content/article/list': '/content/articleManager',
  '/content/article/edit': '/content/articleCreat',
  '/content/articleclass/list': '/content/classifManager',
  '/page/design': '/design/devise',
  '/page/design/index': '/design/devise',
  '/page/design/creatDevise': '/design/devise',
  '/page/design/viewDesign': '/design/viewDesign',
  '/page/design/theme': '/design/theme',
  '/design/index': '/design/devise',
  '/design/devise/index': '/design/devise',
  '/design/devise/creatDevise': '/design/devise',
  '/design/theme/index': '/design/theme',
  '/design/viewDesign/index': '/design/viewDesign',
  '/financial/commission/index': '/financial/commission/template',
  '/financial/commission/withdrawal': '/financial/commission/template',
  '/financial/commission/withdrawal/index': '/financial/commission/template',
  '/financial/record/index': '/financial/record/charge',
  '/financial/record/charge/index': '/financial/record/charge',
  '/financial/record/monitor/index': '/financial/record/monitor',
  '/financial/brokerage/index': '/financial/brokerage',
  '/financial/operating/index': '/financial/record/monitor',
  '/distribution/config/index': '/distribution/distributionconfig',
  '/sms/index': '/operation/onePass',
  '/sms/smsConfig/index': '/operation/systemSms/config',
  '/sms/smsConfig/config': '/operation/onePassConfig',
  '/sms/smsTemplate/index': '/operation/systemSms/template',
  '/sms/smsPay/index': '/operation/systemSms/pay',
  '/sms/smsMessage/index': '/operation/systemSms/message',
  '/sms/smsRecord/index': '/operation/systemSms/record',
  '/systemSetting/setting/index': '/operation/setting',
  '/systemSetting/notification/index': '/operation/notification',
  '/systemSetting/administratorAuthority/identityManager/index': '/operation/roleManager/identityManager',
  '/systemSetting/administratorAuthority/identityManager/edit': '/operation/roleManager/identityManager',
  '/systemSetting/administratorAuthority/adminList/index': '/operation/roleManager/adminList',
  '/systemSetting/administratorAuthority/adminList/edit': '/operation/roleManager/adminList',
  '/systemSetting/administratorAuthority/permissionRules/index': '/operation/roleManager/promiseRules',
  '/systemSetting/deliverGoods/takeGoods/index': '/operation/deliverGoods/takeGoods/collateUser',
  '/systemSetting/deliverGoods/takeGoods/deliveryAddress/index': '/operation/deliverGoods/takeGoods/deliveryAddress',
  '/systemSetting/deliverGoods/takeGoods/deliveryAddress/addPoint': '/operation/deliverGoods/takeGoods/deliveryAddress',
  '/systemSetting/deliverGoods/takeGoods/collateOrder/index': '/operation/deliverGoods/takeGoods/collateOrder',
  '/systemSetting/deliverGoods/takeGoods/collateUser/index': '/operation/deliverGoods/takeGoods/collateUser',
  '/systemSetting/deliverGoods/takeGoods/collateUser/addClerk': '/operation/deliverGoods/takeGoods/collateUser',
  '/systemSetting/deliverGoods/freightSet/index': '/operation/deliverGoods/freightSet',
  '/systemSetting/deliverGoods/freightSet/creatTemplates': '/operation/deliverGoods/freightSet',
  '/systemSetting/logistics/companyList/index': '/maintain/logistics/companyList',
  '/systemSetting/logistics/cityList/index': '/maintain/logistics/cityList',
  '/maintain/devconfig/configCategroy': '/maintain/devconfiguration/configCategory',
  '/maintain/devconfig/configCategotyEdit': '/maintain/devconfiguration/configCategory',
  '/maintain/devconfig/combinedData': '/maintain/devconfiguration/combineddata',
  '/maintain/devconfig/combinedDataEdit': '/maintain/devconfiguration/combineddata',
  '/maintain/devconfig/combineDataList': '/maintain/devconfiguration/combineddata',
  '/maintain/devconfig/combineEdit': '/maintain/devconfiguration/combineddata',
  '/maintain/devconfig/configList': '/maintain/devconfiguration/configCategory',
  '/maintain/formConfig/index': '/maintain/devconfiguration/formConfig',
  '/maintain/formConfig/edit': '/maintain/devconfiguration/formConfig',
  '/maintain/picture/index': '/maintain/picture',
  '/maintain/logistics/companyList/index': '/maintain/logistics/companyList',
  '/maintain/logistics/cityList/index': '/maintain/logistics/cityList',
  '/maintain/schedule/index': '/maintain/schedule/list',
  '/marketing/spike': '/marketing/seckill/index',
  '/marketing/spike/index': '/marketing/seckill/index',
  '/marketing/spike/config': '/marketing/seckill/config',
  '/marketing/spike/config/index': '/marketing/seckill/config',
  '/marketing/spike/googs': '/marketing/seckill/list',
  '/marketing/spike/googs/index': '/marketing/seckill/list',
  '/marketing/spike/goods': '/marketing/seckill/list',
  '/marketing/spike/goods/index': '/marketing/seckill/list',
  '/marketing/seckill/seckillConfig/index': '/marketing/seckill/config',
  '/marketing/seckill/seckillList/index': '/marketing/seckill/list',
  '/marketing/coupon/list/index': '/marketing/coupon/list',
  '/marketing/coupon/list/creatCoupon': '/marketing/coupon/list/save',
  '/marketing/coupon/record/index': '/marketing/coupon/record',
  '/marketing/coupon/couponTemplate/index': '/marketing/coupon/list',
  '/marketing/integral/config/index': '/marketing/integral/integralconfig',
  '/marketing/integral/integralLog/index': '/marketing/integral/integrallog',
  '/marketing/bargain/bargainGoods/index': '/marketing/bargain/bargainGoods',
  '/marketing/bargain/bargainGoods/creatBargain': '/marketing/bargain/creatBargain',
  '/marketing/bargain/bargainList/index': '/marketing/bargain/bargainList',
  '/marketing/groupBuy/list': '/marketing/groupBuy/groupList',
  '/marketing/groupBuy/list/list': '/marketing/groupBuy/groupList',
  '/marketing/groupBuy/groupGoods/index': '/marketing/groupBuy/groupGoods',
  '/marketing/groupBuy/groupGoods/creatGroup': '/marketing/groupBuy/creatGroup',
  '/marketing/groupBuy/groupList/index': '/marketing/groupBuy/groupList',
  '/marketing/seckill/seckillList/creatSeckill': '/marketing/seckill/creatSeckill',
  '/marketing/atmosphere/atmosphereList/list': '/marketing/atmosphere/list',
  '/marketing/atmosphere/atmosphereList/addAtmosphere': '/marketing/atmosphere/add',
  '/marketing/border/index': '/marketing/border/list',
  '/appSetting/wxAccount/index': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/wxAccount/reply/index': '/appSetting/publicAccount/wxReply/keyword',
  '/appSetting/wxAccount/reply/follow/index': '/appSetting/publicAccount/wxReply/follow',
  '/appSetting/wxAccount/reply/keyword/index': '/appSetting/publicAccount/wxReply/keyword',
  '/appSetting/wxAccount/wxMenus': '/appSetting/publicAccount/wxMenus',
  '/appSetting/wxAccount/wxTemplate/index': '/appSetting/publicAccount/wxTemplate'
};

const routePrefixAliases = [
  { from: '/store/creatStore/index/', to: '/store/list/creatProduct/' },
  { from: '/store/creatStore/', to: '/store/list/creatProduct/' },
  { from: '/page/design/creatDevise/', to: '/design/devise/' },
  { from: '/design/devise/creatDevise/', to: '/design/devise/' },
  { from: '/marketing/coupon/list/creatCoupon/', to: '/marketing/coupon/list/save/' },
  { from: '/marketing/spike/config/edit', to: '/marketing/seckill/config' },
  { from: '/marketing/spike/googs/creatSeckill', to: '/marketing/seckill/creatSeckill' },
  { from: '/marketing/spike/goods/creatSeckill', to: '/marketing/seckill/creatSeckill' },
  { from: '/marketing/spike/googs/', to: '/marketing/seckill/list/' },
  { from: '/marketing/spike/goods/', to: '/marketing/seckill/list/' },
  { from: '/marketing/seckill/seckillList/creatSeckill/', to: '/marketing/seckill/creatSeckill/' },
  { from: '/marketing/bargain/bargainGoods/creatBargain/', to: '/marketing/bargain/creatBargain/' },
  { from: '/marketing/groupBuy/groupGoods/creatGroup/', to: '/marketing/groupBuy/creatGroup/' },
  { from: '/marketing/atmosphere/atmosphereList/addAtmosphere/', to: '/marketing/atmosphere/add/' }
];

async function loadLoginPic() {
  try {
    const data = await getLoginPic();
    Object.assign(loginPic, {
      banner: data?.banner || [],
      backgroundImage: data?.backgroundImage || '',
      logo: data?.logo || '',
      loginLogo: data?.loginLogo || '',
      siteName: data?.siteName || ''
    });
    if (data?.siteName) localStorage.setItem('singleAdminSiteName', data.siteName);
  } catch {
    Object.assign(loginPic, { banner: [{ pic: fallbackBanner }] });
  }
}

async function handleAccountBlur() {
  if (!loginForm.account) return;
  errorsNumber.value = await accountDetection(loginForm.account).catch(() => 0);
}

function showPwd() {
  passwordType.value = passwordType.value === 'password' ? 'text' : 'password';
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (Number(errorsNumber.value) > 3) {
    ElMessage.warning('当前未启用行为验证码，请继续使用账号密码登录。');
  }
  loginLoading.value = true;
  try {
    const data = await login({ ...loginForm });
    localStorage.setItem(TOKEN_KEY, data.token);
    isAuthed.value = true;
    loginDisabled.value = true;
    await bootstrap();
    ElMessage.success('登录成功');
  } catch (error) {
    await handleAccountBlur();
    ElMessage.error(error.message || '登录失败');
  } finally {
    loginLoading.value = false;
  }
}

async function bootstrap() {
  if (!localStorage.getItem(TOKEN_KEY)) return;
  try {
    const [info, runtime, menus] = await Promise.all([getAdminInfo(), health(), getMenus()]);
    adminInfo.value = info;
    healthInfo.value = runtime;
    menuTree.value = menus || [];
    syncRoute(window.location.pathname, true);
  } catch (error) {
    clearSession();
    if (isAuthed.value) {
      ElMessage.warning(error.message || '登录已过期');
    }
    throw error;
  }
}

async function handleLogout() {
  try {
    await logout();
  } finally {
    clearSession();
  }
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  isAuthed.value = false;
  loginDisabled.value = false;
  adminInfo.value = null;
  healthInfo.value = null;
  menuTree.value = [];
  currentMenu.value = null;
}

function handleUnauthorized() {
  clearSession();
  ElMessage.warning('登录已过期');
}

function handleResize() {
  fullWidth.value = document.body.clientWidth;
}

function handleMenuSelect(menu) {
  if (menu?.component) navigateTo(menu.component);
}

function firstLeaf(items) {
  for (const item of items || []) {
    if (item.childList?.length) {
      const child = firstLeaf(item.childList);
      if (child) return child;
    } else {
      return item;
    }
  }
  return null;
}

function findMenuPath(items, id, parents = []) {
  if (!id) return [];
  for (const item of items || []) {
    const next = [...parents, item];
    if (item.id === id) return next;
    const childPath = findMenuPath(item.childList, id, next);
    if (childPath.length) return childPath;
  }
  return [];
}

function findMenuByPath(items, path) {
  if (!path || path === '/') return null;
  for (const item of items || []) {
    if (item.component === path) return item;
    const child = findMenuByPath(item.childList, path);
    if (child) return child;
  }
  return null;
}

function legacyMenuForPath(path) {
  const exact = findMenuByPath(menuTree.value, path);
  if (exact) return exact;
  if (migratedRouteMeta[path]) {
    return {
      id: `migrated-${path}`,
      name: migratedRouteMeta[path].name,
      component: path
    };
  }
  if (path?.startsWith('/javaMobile/orderList')) {
    return { id: 'legacy-order-list', name: '订单列表', component: path };
  }
  if (path?.startsWith('/javaMobile/orderDetail')) {
    return { id: 'legacy-order-detail', name: '订单详情', component: path };
  }
  if (path?.startsWith('/javaMobile/orderDelivery')) {
    return { id: 'legacy-order-delivery', name: '订单发货', component: path };
  }
  if (path?.startsWith('/javaMobile/orderStatisticsDetail')) {
    return { id: 'legacy-order-statistics-detail', name: '订单数据统计', component: path };
  }
  if (path?.startsWith('/javaMobile/')) {
    return findMenuByPath(menuTree.value, '/javaMobile/orderStatistics') || firstLeaf(menuTree.value) || menuTree.value[0] || null;
  }
  if (path?.startsWith('/content/articleCreat')) {
    return {
      id: `migrated-${path}`,
      name: path === '/content/articleCreat' ? '添加文章' : '编辑文章',
      component: '/content/articleManager'
    };
  }
  if (path?.startsWith('/store/list/creatProduct')) {
    const parts = path.split('/').filter(Boolean);
    const id = Number(parts[3] || 0) || 0;
    return {
      id: `migrated-${path}`,
      name: id ? (parts[4] === '1' ? '商品详情' : '编辑商品') : '添加商品',
      component: '/store/index'
    };
  }
  if (path?.startsWith('/marketing/coupon/list/save')) {
    return {
      id: `migrated-${path}`,
      name: path === '/marketing/coupon/list/save' ? '添加优惠券' : '复制优惠券',
      component: '/marketing/coupon/list'
    };
  }
  if (path?.startsWith('/appSetting/publicAccount/wxReply/keyword/save')) {
    return {
      id: `migrated-${path}`,
      name: path === '/appSetting/publicAccount/wxReply/keyword/save' ? '添加关键字' : '编辑关键字',
      component: path
    };
  }
  if (path?.startsWith('/marketing/seckill/list/')) {
    return {
      id: `migrated-${path}`,
      name: '秒杀商品',
      component: path
    };
  }
  if (path?.startsWith('/marketing/seckill/creatSeckill')) {
    const parts = path.split('/').filter(Boolean);
    const isEdit = parts[3] === 'updeta' && parts[5];
    return {
      id: `migrated-${path}`,
      name: isEdit ? `编辑秒杀商品-${parts[5]}` : '添加秒杀商品',
      component: '/marketing/seckill/list'
    };
  }
  if (path?.startsWith('/marketing/bargain/creatBargain')) {
    const parts = path.split('/').filter(Boolean);
    const isEdit = Number(parts[3] || 0) > 0;
    return {
      id: `migrated-${path}`,
      name: isEdit ? `编辑砍价商品-${parts[3]}` : '添加砍价商品',
      component: '/marketing/bargain/bargainGoods'
    };
  }
  if (path?.startsWith('/marketing/groupBuy/creatGroup')) {
    const parts = path.split('/').filter(Boolean);
    const isEdit = Number(parts[3] || 0) > 0;
    return {
      id: `migrated-${path}`,
      name: isEdit ? `编辑拼团商品-${parts[3]}` : '添加拼团商品',
      component: '/marketing/groupBuy/groupGoods'
    };
  }
  if (path?.startsWith('/marketing/border/add')) {
    return {
      id: `migrated-${path}`,
      name: path === '/marketing/border/add' ? '添加活动边框' : '编辑活动边框',
      component: path
    };
  }
  if (path?.startsWith('/marketing/atmosphere/add')) {
    return {
      id: `migrated-${path}`,
      name: path === '/marketing/atmosphere/add' ? '添加活动氛围' : '编辑活动氛围',
      component: path
    };
  }
  return currentMenu.value || firstLeaf(menuTree.value) || menuTree.value[0] || null;
}

function normalizeRoutePath(path) {
  const rawPath = path || '/';
  if (routeAliases[rawPath]) return routeAliases[rawPath];
  const prefixAlias = routePrefixAliases.find((item) => rawPath.startsWith(item.from));
  if (prefixAlias) {
    return prefixAlias.to + rawPath.slice(prefixAlias.from.length);
  }
  return parentRouteFallbacks[rawPath] || rawPath;
}

function syncRoute(path) {
  const nextPath = normalizeRoutePath(path);
  if (nextPath !== path && window.location.pathname === path) {
    window.history.replaceState({}, '', nextPath);
  }
  currentPath.value = nextPath;
  currentMenu.value = legacyMenuForPath(currentPath.value);
}

function navigateTo(path) {
  if (!path) return;
  const nextPath = normalizeRoutePath(path);
  if (window.location.pathname !== nextPath) {
    window.history.pushState({}, '', nextPath);
  }
  syncRoute(nextPath);
}

function handlePopState() {
  syncRoute(window.location.pathname);
}

function handleLegacyNavigate(event) {
  const path = event?.detail?.path;
  if (typeof path === 'string') navigateTo(path);
}

onMounted(async () => {
  window.addEventListener('resize', handleResize);
  window.addEventListener('crmeb:unauthorized', handleUnauthorized);
  window.addEventListener('popstate', handlePopState);
  window.addEventListener('crmeb:navigate', handleLegacyNavigate);
  await loadLoginPic();
  await handleAccountBlur();
  bannerTimer = window.setInterval(() => {
    bannerIndex.value += 1;
  }, 3000);
  bootstrap().catch(clearSession);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('crmeb:unauthorized', handleUnauthorized);
  window.removeEventListener('popstate', handlePopState);
  window.removeEventListener('crmeb:navigate', handleLegacyNavigate);
  window.clearInterval(bannerTimer);
});
</script>
