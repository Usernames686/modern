import axios from 'axios';
import { ElMessage } from 'element-plus';

export const TOKEN_KEY = 'Authori-zation';

export const http = axios.create({
  baseURL: '/api',
  timeout: 30000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers[TOKEN_KEY] = token;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body?.code === 401) {
      localStorage.removeItem(TOKEN_KEY);
      window.dispatchEvent(new CustomEvent('crmeb:unauthorized'));
      return Promise.reject(new Error(body.message || '登录已过期'));
    }
    if (body?.code && body.code !== 200) {
      ElMessage.error(body.message || '请求失败');
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return body?.data;
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常';
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export function login(data) {
  return http.post('/admin/login', data);
}

export function getLoginPic() {
  return http.get('/admin/getLoginPic');
}

export function accountDetection(account) {
  return http.post('/admin/login/account/detection', { account });
}

export function getAdminInfo() {
  return http.get('/admin/getAdminInfoByToken');
}

export function getMenus() {
  return http.get('/admin/getMenus');
}

export function logout() {
  return http.get('/admin/logout');
}

export function health() {
  return http.get('/admin/health');
}

export function copyrightInfo() {
  return http.get('/admin/copyright/get/info');
}

export function dashboardHomeIndex() {
  return http.get('/admin/statistics/home/index');
}

export function dashboardOperatingData() {
  return http.get('/admin/statistics/home/operating/data');
}

export function dashboardOrderChart(type = 'last30') {
  const pathMap = {
    last30: '/admin/statistics/home/chart/order',
    week: '/admin/statistics/home/chart/order/week',
    month: '/admin/statistics/home/chart/order/month',
    year: '/admin/statistics/home/chart/order/year'
  };
  return http.get(pathMap[type] || pathMap.last30);
}

export function dashboardUserChart() {
  return http.get('/admin/statistics/home/chart/user');
}

export function statisticProductData(params) {
  return http.get('/admin/statistics/product/data', { params });
}

export function statisticProductRanking(params) {
  return http.get('/admin/statistics/product/ranking', { params });
}

export function statisticProductTrend(params) {
  return http.get('/admin/statistics/product/trend', { params });
}

export function statisticUserTotalData() {
  return http.get('/admin/statistics/user/total/data');
}

export function statisticUserAreaData() {
  return http.get('/admin/statistics/user/area');
}

export function statisticUserChannelData() {
  return http.get('/admin/statistics/user/channel');
}

export function statisticUserOverview(params) {
  return http.get('/admin/statistics/user/overview', { params });
}

export function statisticUserSexData() {
  return http.get('/admin/statistics/user/sex');
}

export function statisticUserOverviewList(params) {
  return http.get('/admin/statistics/user/overview/list', { params });
}

export function statisticTradeData() {
  return http.get('/admin/statistics/trade/data');
}

export function statisticTradeOverview(params) {
  return http.get('/admin/statistics/trade/overview', { params });
}

export function statisticTradeTrend(params) {
  return http.get('/admin/statistics/trade/trend', { params });
}

export function getThemeColor() {
  return http.get('/admin/system/config/get/change/color');
}

export function saveThemeColor(data) {
  return http.post('/admin/system/config/save/change/color', data);
}

export function saveSystemConfigUnique(params) {
  return http.post('/admin/system/config/saveuniq', null, { params });
}

export function clearSystemConfigCache() {
  return http.post('/admin/system/config/clear/cache');
}

export function pageLayoutIndex() {
  return http.get('/admin/page/layout/index');
}

export function pageLayoutBottomNavigation() {
  return http.get('/admin/page/layout/bottom/navigation/get');
}

export function pageLayoutCategoryConfig() {
  return http.get('/admin/page/layout/category/config');
}

export function pageLayoutSave(type, data) {
  const pathMap = {
    indexBanner: '/admin/page/layout/index/banner/save',
    indexTable: '/admin/page/layout/index/table/save',
    indexMenu: '/admin/page/layout/index/menu/save',
    indexNews: '/admin/page/layout/index/news/save',
    userMenu: '/admin/page/layout/user/menu/save',
    userBanner: '/admin/page/layout/user/banner/save',
    bottomNavigation: '/admin/page/layout/bottom/navigation/save',
    categoryConfig: '/admin/page/layout/category/config/save'
  };
  return http.post(pathMap[type], data);
}

export function pageDiyList(params) {
  return http.get('/admin/pagediy/list', { params });
}

export function pageDiyInfo(id) {
  return http.get(`/admin/pagediy/info/${id}`);
}

export function pageDiyDefault() {
  return http.get('/admin/pagediy/getdefault');
}

export function pageDiySetDefault(id) {
  return http.get(`/admin/pagediy/setdefault/${id}`);
}

export function pageDiyDelete(params) {
  return http.get('/admin/pagediy/delete', { params });
}

export function pageDiyCopy(id) {
  return http.post(`/admin/pagediy/copy/${id}`);
}

export function pageDiySaveBase(data) {
  return http.post('/admin/pagediy/save/base', data);
}

export function pageDiySave(data) {
  return http.post('/admin/pagediy/save', data);
}

export function pageDiyUpdate(data) {
  return http.post('/admin/pagediy/update', data);
}

export function productHeaders(params) {
  return http.get('/admin/store/product/tabs/headers', { params });
}

export function productList(params) {
  return http.get('/admin/store/product/list', { params });
}

export function productExport(params) {
  return http.get('/admin/export/excel/product', { params });
}

export function orderExport(params) {
  return http.get('/admin/export/excel/order', { params });
}

export function productDetail(id) {
  return http.get(`/admin/store/product/info/${id}`);
}

export function combinationProductDetail(id) {
  return http.get('/admin/store/combination/product/detail', { params: { id } });
}

export function productListByIds(ids) {
  return http.get(`/admin/store/product/listids/${ids}`);
}

export function productUpdate(data) {
  return http.post('/admin/store/product/update', data);
}

export function productCreate(data) {
  return http.post('/admin/store/product/save', data);
}

export function productImport(params) {
  return http.post('/admin/store/product/importProduct', null, { params });
}

export function productCopyConfig() {
  return http.post('/admin/store/product/copy/config');
}

export function productCopy(data) {
  return http.post('/admin/store/product/copy/product', data);
}

export function productRuleList(params) {
  return http.get('/admin/store/product/rule/list', { params });
}

export function productRuleSave(data) {
  return http.post('/admin/store/product/rule/save', data);
}

export function productRuleUpdate(data) {
  return http.post('/admin/store/product/rule/update', data);
}

export function productRuleDelete(ids) {
  return http.get(`/admin/store/product/rule/delete/${ids}`);
}

export function productRuleInfo(id) {
  return http.get(`/admin/store/product/rule/info/${id}`);
}

export function productReplyList(params) {
  return http.get('/admin/store/product/reply/list', { params });
}

export function productReplySave(data) {
  return http.post('/admin/store/product/reply/save', data);
}

export function productReplyInfo(id) {
  return http.get(`/admin/store/product/reply/info/${id}`);
}

export function productReplyDelete(id) {
  return http.get(`/admin/store/product/reply/delete/${id}`);
}

export function productReplyComment(data) {
  return http.post('/admin/store/product/reply/comment', data);
}

export function productDelete(id, type = 'recycle') {
  return http.get(`/admin/store/product/delete/${id}`, { params: { type } });
}

export function productRestore(id) {
  return http.get(`/admin/store/product/restore/${id}`);
}

export function productPutOnShell(id) {
  return http.get(`/admin/store/product/putOnShell/${id}`);
}

export function productOffShell(id) {
  return http.get(`/admin/store/product/offShell/${id}`);
}

export function stockAdd(data) {
  return http.post('/admin/store/product/quick/stock/add', data);
}

export function categoryTree(params = { status: -1, type: 1 }) {
  return http.get('/admin/category/list/tree', { params });
}

export function categoryList(params) {
  return http.get('/admin/category/list', { params });
}

export function categoryInfo(params) {
  return http.get('/admin/category/info', { params });
}

export function categorySave(data) {
  return http.post('/admin/category/save', null, { params: data });
}

export function categoryUpdate(data) {
  return http.post('/admin/category/update', null, { params: data });
}

export function categoryDelete(params) {
  return http.get('/admin/category/delete', { params });
}

export function categoryUpdateStatus(id) {
  return http.get(`/admin/category/updateStatus/${id}`);
}

export function categoryByIds(params) {
  return http.get('/admin/category/list/ids', { params });
}

export function articleList(params) {
  return http.get('/admin/article/list', { params });
}

export function articleInfo(params) {
  return http.get('/admin/article/info', { params });
}

export function articleSave(data) {
  return http.post('/admin/article/save', data);
}

export function articleUpdate(params, data) {
  return http.post('/admin/article/update', data, { params });
}

export function articleDelete(params) {
  return http.get('/admin/article/delete', { params });
}

export function systemFormTempInfo(params) {
  return http.get('/admin/system/form/temp/info', { params });
}

export function systemFormTempList(params) {
  return http.get('/admin/system/form/temp/list', { params });
}

export function systemFormTempSave(data) {
  return http.post('/admin/system/form/temp/save', data);
}

export function systemFormTempUpdate(params, data) {
  return http.post('/admin/system/form/temp/update', data, { params });
}

export function systemFormTempDelete(params) {
  return http.get('/admin/system/form/temp/delete', { params });
}

export function systemGroupList(params) {
  return http.get('/admin/system/group/list', { params });
}

export function systemGroupInfo(params) {
  return http.get('/admin/system/group/info', { params });
}

export function systemGroupSave(data) {
  return http.post('/admin/system/group/save', null, { params: data });
}

export function systemGroupUpdate(data) {
  return http.post('/admin/system/group/update', null, { params: data });
}

export function systemGroupDelete(params) {
  return http.get('/admin/system/group/delete', { params });
}

export function systemGroupDataList(params) {
  return http.get('/admin/system/group/data/list', { params });
}

export function systemGroupDataInfo(params) {
  return http.get('/admin/system/group/data/info', { params });
}

export function systemGroupDataSave(data) {
  return http.post('/admin/system/group/data/save', data);
}

export function systemGroupDataUpdate(params, data) {
  return http.post('/admin/system/group/data/update', data, { params });
}

export function systemGroupDataDelete(params) {
  return http.get('/admin/system/group/data/delete', { params });
}

export function scheduleJobList() {
  return http.get('/admin/schedule/job/list');
}

export function scheduleJobLogList(params) {
  return http.get('/admin/schedule/job/log/list', { params });
}

export function scheduleJobAdd(data) {
  return http.post('/admin/schedule/job/add', data);
}

export function scheduleJobUpdate(data) {
  return http.post('/admin/schedule/job/update', data);
}

export function scheduleJobStart(id) {
  return http.post(`/admin/schedule/job/start/${id}`);
}

export function scheduleJobSuspend(id) {
  return http.post(`/admin/schedule/job/suspend/${id}`);
}

export function scheduleJobTrigger(id) {
  return http.post(`/admin/schedule/job/trig/${id}`);
}

export function scheduleJobDelete(id) {
  return http.post(`/admin/schedule/job/delete/${id}`);
}

export function systemConfigInfo(params) {
  return http.get('/admin/system/config/info', { params });
}

export function systemConfigSaveForm(data) {
  return http.post('/admin/system/config/save/form', data);
}

export function integralRecordList(params, data) {
  return http.post('/admin/user/integral/list', data, { params });
}

export function topUpLogList(params) {
  return http.get('/admin/user/topUpLog/list', { params });
}

export function topUpLogBalance() {
  return http.post('/admin/user/topUpLog/balance');
}

export function financeMonitorList(params) {
  return http.get('/admin/finance/founds/monitor/list', { params });
}

export function brokerageRecordList(params) {
  return http.get('/admin/finance/founds/monitor/brokerage/record', { params });
}

export function extractApplyList(params) {
  return http.get('/admin/finance/apply/list', { params });
}

export function extractApplyBalance(params) {
  return http.post('/admin/finance/apply/balance', null, { params });
}

export function extractApplyUpdate(params) {
  return http.post('/admin/finance/apply/update', null, { params });
}

export function extractApplyStatus(params) {
  return http.post('/admin/finance/apply/apply', null, { params });
}

export function distributionConfigInfo() {
  return http.get('/admin/store/retail/spread/manage/get');
}

export function distributionConfigSave(data) {
  return http.post('/admin/store/retail/spread/manage/set', data);
}

export function promoterList(params) {
  return http.get('/admin/store/retail/list', { params });
}

export function spreadUserList(params, data) {
  return http.post('/admin/store/retail/spread/userlist', data, { params });
}

export function spreadOrderList(params, data) {
  return http.post('/admin/store/retail/spread/orderlist', data, { params });
}

export function spreadClear(id) {
  return http.get(`/admin/store/retail/spread/clean/${id}`);
}

export function systemConfigUploadType() {
  return http.get('/admin/system/config/get/upload/type');
}

export function notificationList(params) {
  return http.get('/admin/system/notification/list', { params });
}

export function notificationRoutineSwitch(id) {
  return http.post(`/admin/system/notification/routine/switch/${id}`);
}

export function notificationWechatSwitch(id) {
  return http.post(`/admin/system/notification/wechat/switch/${id}`);
}

export function notificationSmsSwitch(id) {
  return http.post(`/admin/system/notification/sms/switch/${id}`);
}

export function notificationDetail(params) {
  return http.get('/admin/system/notification/detail', { params });
}

export function notificationUpdate(data) {
  return http.post('/admin/system/notification/update', data);
}

export function systemRoleList(params) {
  return http.get('/admin/system/role/list', { params });
}

export function systemRoleSave(data) {
  return http.post('/admin/system/role/save', data);
}

export function systemRoleUpdate(data) {
  return http.post('/admin/system/role/update', data, { params: { id: data.id } });
}

export function systemRoleDelete(params) {
  return http.get('/admin/system/role/delete', { params });
}

export function systemRoleInfo(id) {
  return http.get(`/admin/system/role/info/${id}`);
}

export function systemRoleUpdateStatus(params) {
  return http.get('/admin/system/role/updateStatus', { params });
}

export function systemMenuCacheTree() {
  return http.get('/admin/system/menu/cache/tree');
}

export function systemAdminInfo(params) {
  return http.get('/admin/system/admin/info', { params });
}

export function systemAdminSave(data) {
  return http.post('/admin/system/admin/save', data);
}

export function systemAdminUpdate(data) {
  return http.post('/admin/system/admin/update', data);
}

export function systemAdminDelete(params) {
  return http.get('/admin/system/admin/delete', { params });
}

export function systemAdminUpdateStatus(params) {
  return http.get('/admin/system/admin/updateStatus', { params });
}

export function systemAdminUpdateIsSms(params) {
  return http.get('/admin/system/admin/update/isSms', { params });
}

export function systemMenuList(params) {
  return http.get('/admin/system/menu/list', { params });
}

export function systemMenuAdd(data) {
  return http.post('/admin/system/menu/add', data);
}

export function systemMenuDelete(id) {
  return http.post(`/admin/system/menu/delete/${id}`);
}

export function systemMenuInfo(id) {
  return http.get(`/admin/system/menu/info/${id}`);
}

export function systemMenuUpdate(data) {
  return http.post('/admin/system/menu/update', data);
}

export function systemMenuUpdateShowStatus(id) {
  return http.post(`/admin/system/menu/updateShowStatus/${id}`);
}

export function uploadImage(data, params = {}) {
  return http.post('/admin/upload/image', data, { params: { model: 'product', pid: 0, ...params } });
}

export function uploadFile(data, params = {}) {
  return http.post('/admin/upload/file', data, { params: { model: 'product', pid: 0, ...params } });
}

export function attachmentList(params) {
  return http.get('/admin/system/attachment/list', { params });
}

export function attachmentDelete(ids) {
  return http.get(`/admin/system/attachment/delete/${ids}`);
}

export function attachmentMove(data) {
  return http.post('/admin/system/attachment/move', data);
}

export function shippingTemplatesList(params) {
  return http.get('/admin/express/shipping/templates/list', { params });
}

export function shippingTemplateInfo(params) {
  return http.get('/admin/express/shipping/templates/info', { params });
}

export function shippingTemplateSave(data) {
  return http.post('/admin/express/shipping/templates/save', data);
}

export function shippingTemplateUpdate(data, id) {
  return http.post('/admin/express/shipping/templates/update', data, { params: { id } });
}

export function shippingTemplateDelete(params) {
  return http.get('/admin/express/shipping/templates/delete', { params });
}

export function cityListTree() {
  return http.get('/admin/system/city/list/tree');
}

export function cityList(params) {
  return http.get('/admin/system/city/list', { params });
}

export function cityInfo(params) {
  return http.get('/admin/system/city/info', { params });
}

export function cityUpdate(params) {
  return http.post('/admin/system/city/update', null, { params });
}

export function expressList(params) {
  return http.get('/admin/express/list', { params });
}

export function expressInfo(params) {
  return http.get('/admin/express/info', { params });
}

export function expressUpdate(data) {
  return http.post('/admin/express/update', data);
}

export function expressUpdateShow(data) {
  return http.post('/admin/express/update/show', data);
}

export function expressSync() {
  return http.post('/admin/express/sync/express');
}

export function couponSendList(params) {
  return http.get('/admin/marketing/coupon/send/list', { params });
}

export function couponReceive(params) {
  return http.post('/admin/marketing/coupon/user/receive', null, { params });
}

export function couponList(params) {
  return http.get('/admin/marketing/coupon/list', { params });
}

export function couponInfo(params) {
  return http.post('/admin/marketing/coupon/info', null, { params });
}

export function couponSave(data) {
  return http.post('/admin/marketing/coupon/save', data);
}

export function couponUserList(params) {
  return http.get('/admin/marketing/coupon/user/list', { params });
}

export function couponUpdateStatus(params) {
  return http.post('/admin/marketing/coupon/update/status', null, { params });
}

export function couponDelete(params) {
  return http.post('/admin/marketing/coupon/delete', null, { params });
}

export function activityStyleList(params) {
  return http.get('/admin/activitystyle/list', { params });
}

export function activityStyleInfo(params) {
  return http.get('/admin/activitystyle/info', { params });
}

export function activityStyleSave(data) {
  return http.post('/admin/activitystyle/save', data);
}

export function activityStyleUpdate(data) {
  return http.post('/admin/activitystyle/update', data);
}

export function activityStyleDelete(params) {
  return http.get('/admin/activitystyle/delete', { params });
}

export function activityStyleStatus(data) {
  return http.post('/admin/activitystyle/status', data);
}

export function videoChannelProductList(params) {
  return http.get('/admin/marketing/videoChannel/list', { params });
}

export function videoChannelDraftList(params) {
  return http.get('/admin/marketing/videoChannel/draft/list', { params });
}

export function wechatMediaUpload(data, params) {
  return http.post('/admin/wechat/media/upload', data, { params });
}

export function orderList(params) {
  return http.get('/admin/store/order/list', { params });
}

export function orderInfo(params) {
  return http.get('/admin/store/order/info', { params });
}

export function orderMark(params) {
  return http.post('/admin/store/order/mark', null, { params });
}

export function orderUpdatePrice(data) {
  return http.post('/admin/store/order/update/price', data);
}

export function orderSend(data) {
  return http.post('/admin/store/order/send', data);
}

export function orderStatusList(params) {
  return http.get('/admin/store/order/status/list', { params });
}

export function orderDelete(params) {
  return http.get('/admin/store/order/delete', { params });
}

export function orderPrint(id) {
  return http.get(`/admin/yly/print/${id}`);
}

export function orderStatusNum(params) {
  return http.get('/admin/store/order/status/num', { params });
}

export function orderRefund(params) {
  return http.get('/admin/store/order/refund', {
    params: {
      orderNo: params.orderNo,
      amount: params.amount
    }
  });
}

export function orderRefundRefuse(params) {
  return http.get('/admin/store/order/refund/refuse', {
    params: {
      orderNo: params.orderNo,
      reason: params.reason
    }
  });
}

export function orderWriteUpdate(verifyCode) {
  return http.get(`/admin/store/order/writeUpdate/${verifyCode}`);
}

export function orderWriteConfirm(verifyCode) {
  return http.get(`/admin/store/order/writeConfirm/${verifyCode}`);
}

export function orderStatistics() {
  return http.get('/admin/store/order/statistics');
}

export function orderStatisticsData(params) {
  return http.get('/admin/store/order/statisticsData', { params });
}

export function orderTime(params) {
  return http.get('/admin/store/order/time', { params });
}

export function writeOffOrderList(params) {
  return http.post('/admin/system/store/order/list', null, { params });
}

export function storePointList(params) {
  return http.get('/admin/system/store/list', { params });
}

export function storePointCount(params) {
  return http.get('/admin/system/store/getCount', { params });
}

export function storePointUpdateStatus(params) {
  return http.get('/admin/system/store/update/status', { params });
}

export function storePointDelete(params) {
  return http.get('/admin/system/store/delete', { params });
}

export function storePointCompletelyDelete(params) {
  return http.get('/admin/system/store/completely/delete', { params });
}

export function storePointRecovery(params) {
  return http.get('/admin/system/store/recovery', { params });
}

export function storePointInfo(params) {
  return http.get('/admin/system/store/info', { params });
}

export function storePointSave(data) {
  return http.post('/admin/system/store/save', data);
}

export function storePointUpdate(data, id) {
  return http.post('/admin/system/store/update', data, { params: { id } });
}

export function storeStaffList(params) {
  return http.get('/admin/system/store/staff/list', { params });
}

export function storeStaffSave(data) {
  return http.post('/admin/system/store/staff/save', data);
}

export function storeStaffUpdate(data) {
  return http.post('/admin/system/store/staff/update', data, { params: { id: data.id } });
}

export function storeStaffDelete(params) {
  return http.get('/admin/system/store/staff/delete', { params });
}

export function storeStaffInfo(params) {
  return http.get('/admin/system/store/staff/info', { params });
}

export function storeStaffUpdateStatus(params) {
  return http.get('/admin/system/store/staff/update/status', { params });
}

export function systemAdminList(params) {
  return http.get('/admin/system/admin/list', { params });
}

export function userList(params) {
  return http.get('/admin/user/list', { params });
}

export function userInfo(params) {
  return http.get('/admin/user/info', { params });
}

export function userUpdate(params, data) {
  return http.post('/admin/user/update', data, { params });
}

export function userGroups(params = { page: 1, limit: 9999 }) {
  return http.get('/admin/user/group/list', { params });
}

export function userTags(params = { page: 1, limit: 9999 }) {
  return http.get('/admin/user/tag/list', { params });
}

export function userGroupSave(data) {
  return http.post('/admin/user/group/save', data);
}

export function userTagSave(data) {
  return http.post('/admin/user/tag/save', data);
}

export function userGroupUpdate(params, data) {
  return http.post('/admin/user/group/update', data, { params });
}

export function userTagUpdate(params, data) {
  return http.post('/admin/user/tag/update', data, { params });
}

export function userGroupDelete(params) {
  return http.get('/admin/user/group/delete', { params });
}

export function userTagDelete(params) {
  return http.get('/admin/user/tag/delete', { params });
}

export function userGroupInfo(params) {
  return http.get('/admin/user/group/info', { params });
}

export function userTagInfo(params) {
  return http.get('/admin/user/tag/info', { params });
}

export function userLevels() {
  return http.get('/admin/system/user/level/list');
}

export function userLevelSave(data) {
  return http.post('/admin/system/user/level/save', data);
}

export function userLevelUpdate(id, data) {
  return http.post(`/admin/system/user/level/update/${id}`, data);
}

export function userLevelInfo(params) {
  return http.get('/admin/system/user/level/info', { params });
}

export function userLevelDelete(id) {
  return http.post(`/admin/system/user/level/delete/${id}`);
}

export function userLevelUse(data) {
  return http.post('/admin/system/user/level/use', data);
}

export function userUpdatePhone(params) {
  return http.get('/admin/user/update/phone', { params });
}

export function userSetGroup(params) {
  return http.post('/admin/user/group', null, { params });
}

export function userSetTag(params) {
  return http.post('/admin/user/tag', null, { params });
}

export function userOperateFunds(params) {
  return http.get('/admin/user/operate/founds', { params });
}

export function userUpdateLevel(data) {
  return http.post('/admin/user/update/level', data);
}

export function userUpdateSpread(data) {
  return http.post('/admin/user/update/spread', data);
}

export function userClearSpread(id) {
  return http.get(`/admin/store/retail/spread/clean/${id}`);
}

export function wechatReplyList(params) {
  return http.get('/admin/wechat/keywords/reply/list', { params });
}

export function wechatReplySave(data) {
  return http.post('/admin/wechat/keywords/reply/save', data);
}

export function wechatReplyUpdate(params, data) {
  return http.post('/admin/wechat/keywords/reply/update', data, { params });
}

export function wechatReplyStatus(params) {
  return http.post('/admin/wechat/keywords/reply/status', null, { params });
}

export function wechatReplyInfo(params) {
  return http.get('/admin/wechat/keywords/reply/info', { params });
}

export function wechatReplyInfoByKeywords(params) {
  return http.get('/admin/wechat/keywords/reply/info/keywords', { params });
}

export function wechatReplyDelete(params) {
  return http.get('/admin/wechat/keywords/reply/delete', { params });
}

export function wechatMenuInfo(params) {
  return http.get('/admin/wechat/menu/public/get', { params });
}

export function wechatMenuSave(data) {
  return http.post('/admin/wechat/menu/public/create', data);
}

export function wechatMenuDelete() {
  return http.get('/admin/wechat/menu/public/delete');
}

export function wechatTemplateList(params) {
  return http.get('/admin/wechat/template/list', { params });
}

export function wechatTemplateSave(data) {
  return http.post('/admin/wechat/template/save', data);
}

export function wechatTemplateUpdate(id, data) {
  return http.post(`/admin/wechat/template/update/${id}`, data);
}

export function wechatTemplateStatus(id, params) {
  return http.post(`/admin/wechat/template/update/status/${id}`, null, { params });
}

export function wechatTemplateDelete(id) {
  return http.get(`/admin/wechat/template/delete/${id}`);
}

export function wechatTemplateSync() {
  return http.post('/admin/wechat/template/whcbqhn/sync');
}

export function routineTemplateSync() {
  return http.post('/admin/wechat/template/routine/sync');
}

export function publicTempList(params) {
  return http.get('/admin/wechat/program/public/temp/list', { params });
}

export function publicTempCategories() {
  return http.get('/admin/wechat/program/category');
}

export function publicTempKeywords(params) {
  return http.get('/admin/wechat/program/getWeChatKeywordsByTid', { params });
}

export function myTempList(params) {
  return http.get('/admin/wechat/program/my/temp/list', { params });
}

export function myTempSave(data) {
  return http.post('/admin/wechat/program/my/temp/save', data);
}

export function myTempUpdate(id, data) {
  return http.post('/admin/wechat/program/my/temp/update', data, { params: { id } });
}

export function myTempStatus(params) {
  return http.get('/admin/wechat/program/my/temp/update/status', { params });
}

export function myTempType(params) {
  return http.get('/admin/wechat/program/my/temp/update/type', { params });
}

export function myTempDelete(params) {
  return http.get('/admin/wechat/program/my/temp/delete', { params });
}

export function myTempAsync() {
  return http.get('/admin/wechat/program/my/temp/async');
}

export function smsTemplates(params) {
  return http.get('/admin/sms/temps', { params });
}

export function smsTemplateApply(data) {
  return http.post('/admin/sms/temp/apply', data);
}

export function smsRecords(params) {
  return http.get('/admin/sms/record/list', { params });
}

export function smsPassInfo() {
  return http.get('/admin/pass/info');
}

export function smsPassIsLogin() {
  return http.get('/admin/pass/isLogin');
}

export function smsMealList(params) {
  return http.get('/admin/pass/meal/list', { params });
}

export function smsSafePayCode(data) {
  return http.post('/admin/pass/meal/code', data);
}

export function seckillStoreList(params) {
  return http.get('/admin/store/seckill/list', { params });
}

export function seckillStoreInfo(params) {
  return http.get('/admin/store/seckill/info', { params });
}

export function seckillStoreSave(data) {
  return http.post('/admin/store/seckill/save', data);
}

export function seckillStoreUpdate(params, data) {
  return http.post('/admin/store/seckill/update', data, { params });
}

export function seckillStoreStatus(params) {
  return http.post('/admin/store/seckill/update/status', null, { params });
}

export function seckillStoreDelete(params) {
  return http.get('/admin/store/seckill/delete', { params });
}

export function seckillManagerList(params) {
  return http.get('/admin/store/seckill/manger/list', { params });
}

export function seckillManagerInfo(params) {
  return http.get('/admin/store/seckill/manger/info', { params });
}

export function seckillManagerSave(data) {
  return http.post('/admin/store/seckill/manger/save', data);
}

export function seckillManagerUpdate(params, data) {
  return http.post('/admin/store/seckill/manger/update', data, { params });
}

export function seckillManagerDelete(params) {
  return http.get('/admin/store/seckill/manger/delete', { params });
}

export function seckillManagerStatus(id, params) {
  return http.post(`/admin/store/seckill/manger/update/status/${id}`, null, { params });
}

export function seckillExport(params) {
  return http.get('/admin/export/excel/seckill/product', { params });
}

export function bargainList(params) {
  return http.get('/admin/store/bargain/list', { params });
}

export function bargainInfo(params) {
  return http.get('/admin/store/bargain/info', { params });
}

export function bargainSave(data) {
  return http.post('/admin/store/bargain/save', data);
}

export function bargainUpdate(params, data) {
  return http.post('/admin/store/bargain/update', data, { params });
}

export function bargainStatus(params) {
  return http.post('/admin/store/bargain/update/status', null, { params });
}

export function bargainDelete(params) {
  return http.get('/admin/store/bargain/delete', { params });
}

export function bargainUserList(params) {
  return http.get('/admin/store/bargain/bargain_list', { params });
}

export function bargainUserHelp(id) {
  return http.get(`/admin/store/bargain/bargain_list/${id}`);
}

export function bargainExport(params) {
  return http.get('/admin/export/excel/bargain/product', { params });
}

export function combinationList(params) {
  return http.get('/admin/store/combination/list', { params });
}

export function combinationInfo(params) {
  return http.get('/admin/store/combination/info', { params });
}

export function combinationSave(data) {
  return http.post('/admin/store/combination/save', data);
}

export function combinationUpdate(params, data) {
  return http.post('/admin/store/combination/update', data, { params });
}

export function combinationStatus(params) {
  return http.post('/admin/store/combination/update/status', null, { params });
}

export function combinationDelete(params) {
  return http.get('/admin/store/combination/delete', { params });
}

export function combinationPinkList(params) {
  return http.get('/admin/store/combination/combine/list', { params });
}

export function combinationStatistics() {
  return http.get('/admin/store/combination/statistics');
}

export function combinationPinkOrders(id) {
  return http.get(`/admin/store/combination/order_pink/${id}`);
}

export function combinationExport(params) {
  return http.get('/admin/export/excel/combiantion/product', { params });
}
