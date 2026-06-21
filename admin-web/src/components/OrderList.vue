<template>
  <div class="divBox relative">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline size="small" label-width="75px">
          <el-form-item label="订单类型：">
            <el-select v-model="query.type" placeholder="状态" class="selWidth" @change="searchList">
              <el-option v-for="item in orderTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间选择：">
            <el-date-picker
              v-model="timeValue"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              type="daterange"
              placement="bottom-end"
              placeholder="自定义时间"
              class="selWidth"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              @change="handleTimeChange"
            />
          </el-form-item>
          <el-form-item label="订单号码：">
            <el-input
              v-model="query.orderNo"
              placeholder="请输入订单号"
              class="selWidth"
              clearable
              @keyup.enter="searchList"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchList">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-tabs v-model="query.status" @tab-change="searchList">
            <el-tab-pane
              v-for="item in statusTabs"
              :key="item.name"
              :name="item.name"
              :label="`${item.label}(${statusCounts[item.name] || 0})`"
            />
          </el-tabs>
          <el-button :loading="exportLoading" @click="handleExport">导出</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        size="small"
        class="table"
        highlight-current-row
        border
        row-key="orderId"
      >
        <el-table-column v-if="visibleColumns.includes('订单号')" label="订单号" min-width="210">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderId }}</span>
            <span v-if="row.isDel" class="deleted-tip">用户已删除</span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('订单类型')" prop="orderType" label="订单类型" min-width="110" />
        <el-table-column v-if="visibleColumns.includes('收货人')" prop="realName" label="收货人" min-width="100" />
        <el-table-column v-if="visibleColumns.includes('商品信息')" label="商品信息" min-width="400" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.productList?.length">
              <div v-for="item in row.productList" :key="item.id" class="tabBox acea-row row-middle">
                <div class="demo-image__preview mr10">
                  <el-image :src="item.info.image" :preview-src-list="[item.info.image]" fit="cover" />
                </div>
                <div class="text_overflow">
                  <span class="tabBox_tit mr10">{{ item.info.productName }} | {{ item.info.sku || '-' }}</span>
                  <span class="tabBox_pice">{{ money(item.info.price) }} x {{ item.info.payNum || 0 }}</span>
                </div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('实际支付')" label="实际支付" min-width="90">
          <template #default="{ row }">{{ money(row.payPrice) }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('支付方式')" prop="payTypeStr" label="支付方式" min-width="90" />
        <el-table-column v-if="visibleColumns.includes('订单状态')" label="订单状态" min-width="110">
          <template #default="{ row }">
            <el-popover
              v-if="hasRefundInfo(row)"
              trigger="hover"
              placement="left"
              :open-delay="500"
            >
              <template #reference>
                <b class="refunding">{{ row.statusStr?.value || '-' }}</b>
              </template>
              <div class="pup_card flex-column">
                <span>退款状态：{{ refundStatusText(row) }}</span>
                <span>退款原因：{{ row.refundReasonWap || '-' }}</span>
                <span>备注说明：{{ row.refundReasonWapExplain || '-' }}</span>
                <span>退款时间：{{ formatDateTime(row.refundReasonTime) }}</span>
                <span v-if="row.refundReason">拒绝原因：{{ row.refundReason }}</span>
                <span class="acea-row">
                  退款凭证：
                  <template v-if="refundImages(row).length">
                    <div v-for="item in refundImages(row)" :key="item" class="refund-image">
                      <el-image :src="assetUrl(item)" :preview-src-list="[assetUrl(item)]" fit="cover" />
                    </div>
                  </template>
                  <span v-else>无</span>
                </span>
              </div>
            </el-popover>
            <span v-else>{{ row.statusStr?.value || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('下单时间')" label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #header>
            <span class="operation-header">
              <span>操作</span>
              <el-button class="column-setting" link @click="cardSelectVisible = !cardSelectVisible">
                <el-icon><Setting /></el-icon>
              </el-button>
            </span>
          </template>
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button
              v-if="row.paid === false && !row.isAlterPrice"
              link
              type="primary"
              @click="handleEditPrice(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.statusStr?.key === 'notShipped' && row.refundStatus === 0"
              link
              type="primary"
              @click="handleSend(row)"
            >
              发送货
            </el-button>
            <el-button
              v-if="row.statusStr?.key === 'toBeWrittenOff' && row.paid && row.refundStatus === 0"
              link
              type="primary"
              @click="handleWriteOff(row)"
            >
              立即核销
            </el-button>
            <el-dropdown trigger="click">
              <span class="el-dropdown-link">更多<el-icon><ArrowDown /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleOrderLog(row)">订单记录</el-dropdown-item>
                  <el-dropdown-item @click="handleOrderMark(row)">订单备注</el-dropdown-item>
                  <el-dropdown-item v-if="row.refundStatus === 1" @click="handleRefundRefuse(row)">拒绝退款</el-dropdown-item>
                  <el-dropdown-item v-if="row.refundStatus === 1" @click="handleRefund(row)">立即退款</el-dropdown-item>
                  <el-dropdown-item v-if="row.statusStr?.key === 'deleted'" @click="handleDelete(row)">删除订单</el-dropdown-item>
                  <el-dropdown-item v-if="row.statusStr?.key !== 'unPaid'" @click="handlePrint(row)">打印小票</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 80]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <div v-show="cardSelectVisible" class="card_abs">
      <div class="cell_ht">
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAllChange">全选</el-checkbox>
        <el-button link type="primary" @click="saveColumns">保存</el-button>
      </div>
      <el-checkbox-group v-model="visibleColumns" @change="handleColumnChange">
        <el-checkbox v-for="item in columnData" :key="item" :label="item" class="check_cell">{{ item }}</el-checkbox>
      </el-checkbox-group>
    </div>

    <el-drawer
      v-model="detailDrawerVisible"
      title="订单详情"
      direction="rtl"
      size="70%"
      class="showHeader"
      destroy-on-close
    >
      <div v-if="currentOrder" class="order-detail">
        <div class="detailHead">
          <div class="full">
            <div class="order_icon">订</div>
            <div class="text">
              <div class="title">{{ currentOrder.orderType || '-' }}</div>
              <div><span class="mr20">订单号：{{ currentOrder.orderId }}</span></div>
            </div>
          </div>
          <ul class="list">
            <li class="item"><div class="title">订单状态</div><div class="color-warning">{{ currentOrder.statusStr?.value || '-' }}</div></li>
            <li class="item"><div class="title">实际支付</div><div>{{ money(currentOrder.payPrice) }}</div></li>
            <li class="item"><div class="title">支付方式</div><div>{{ currentOrder.payTypeStr || '-' }}</div></li>
            <li class="item"><div class="title">创建时间</div><div>{{ formatDateTime(currentOrder.createTime) }}</div></li>
          </ul>
        </div>

        <el-tabs v-model="detailTab" type="border-card" class="order-detail-tabs">
          <el-tab-pane label="订单信息" name="detail">
            <section class="detailSection">
              <div class="title">收货信息</div>
              <ul class="list">
                <li class="item"><div class="lang">收货人：</div><div class="value">{{ currentOrder.realName || '-' }}</div></li>
                <li class="item"><div class="lang">收货电话：</div><div class="value">{{ currentOrder.userPhone || '-' }}</div></li>
                <li class="item"><div class="lang">收货地址：</div><div class="value">{{ currentOrder.userAddress || '-' }}</div></li>
              </ul>
            </section>
            <section v-if="currentOrder.shippingType === 2" class="detailSection">
              <div class="title">核销信息</div>
              <ul class="list">
                <li class="item"><div class="lang">核销姓名：</div><div class="value">{{ currentOrder.realName || '-' }}</div></li>
                <li class="item"><div class="lang">核销电话：</div><div class="value">{{ currentOrder.userPhone || '-' }}</div></li>
                <li class="item"><div class="lang">核销码：</div><div class="value">{{ currentOrder.verifyCode || '-' }}</div></li>
              </ul>
            </section>
            <section class="detailSection">
              <div class="title">订单信息</div>
              <ul class="list">
                <li class="item"><div class="lang">商品总价：</div><div class="value">{{ money(currentOrder.proTotalPrice || currentOrder.totalPrice) }}</div></li>
                <li class="item"><div class="lang">商品总数：</div><div class="value">{{ currentOrder.totalNum || 0 }}</div></li>
                <li class="item"><div class="lang">优惠券：</div><div class="value">{{ money(currentOrder.couponPrice) }}</div></li>
                <li class="item"><div class="lang">实际支付：</div><div class="value">{{ money(currentOrder.payPrice) }}</div></li>
                <li class="item"><div class="lang">支付邮费：</div><div class="value">{{ money(currentOrder.payPostage) }}</div></li>
                <li class="item"><div class="lang">创建时间：</div><div class="value">{{ formatDateTime(currentOrder.createTime) }}</div></li>
              </ul>
            </section>
            <section v-if="hasRefundInfo(currentOrder)" class="detailSection">
              <div class="title">退款信息</div>
              <ul class="list">
                <li class="item"><div class="lang">退款状态：</div><div class="value">{{ refundStatusText(currentOrder) }}</div></li>
                <li class="item"><div class="lang">申请金额：</div><div class="value">{{ money(currentOrder.payPrice) }}</div></li>
                <li class="item"><div class="lang">已退金额：</div><div class="value">{{ money(currentOrder.refundPrice || 0) }}</div></li>
                <li class="item"><div class="lang">退款原因：</div><div class="value">{{ currentOrder.refundReasonWap || '-' }}</div></li>
                <li class="item"><div class="lang">备注说明：</div><div class="value">{{ currentOrder.refundReasonWapExplain || '-' }}</div></li>
                <li class="item"><div class="lang">申请时间：</div><div class="value">{{ formatDateTime(currentOrder.refundReasonTime) }}</div></li>
                <li v-if="currentOrder.refundReason" class="item"><div class="lang">拒绝原因：</div><div class="value">{{ currentOrder.refundReason }}</div></li>
                <li class="item refund-voucher-item">
                  <div class="lang">退款凭证：</div>
                  <div class="value refund-voucher-list">
                    <template v-if="refundImages(currentOrder).length">
                      <div v-for="item in refundImages(currentOrder)" :key="item" class="refund-image detail-refund-image">
                        <el-image :src="assetUrl(item)" :preview-src-list="[assetUrl(item)]" fit="cover" />
                      </div>
                    </template>
                    <span v-else>-</span>
                  </div>
                </li>
              </ul>
              <div v-if="Number(currentOrder.refundStatus || 0) === 1" class="refund-action-row">
                <el-button type="primary" @click="handleRefund(currentOrder)">立即退款</el-button>
                <el-button @click="handleRefundRefuse(currentOrder)">拒绝退款</el-button>
              </div>
            </section>
            <section class="detailSection"><div class="title">买家留言</div><div>{{ currentOrder.mark || '-' }}</div></section>
            <section class="detailSection"><div class="title">商家备注</div><div>{{ currentOrder.remark || '-' }}</div></section>
          </el-tab-pane>
          <el-tab-pane label="商品信息" name="goods">
            <el-table :data="currentOrder.productList || []" class="detail-product-table" size="small" border>
              <el-table-column label="商品信息" min-width="420">
                <template #default="{ row }">
                  <div class="tabBox acea-row row-middle">
                    <div class="demo-image__preview mr10"><el-image :src="row.info.image" :preview-src-list="[row.info.image]" fit="cover" /></div>
                    <div class="text_overflow"><span class="tabBox_tit mr10">{{ row.info.productName }}</span><span class="tabBox_pice">规格：{{ row.info.sku || '-' }}</span></div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="商品售价" width="120"><template #default="{ row }">{{ money(row.info.price) }}</template></el-table-column>
              <el-table-column label="购买数量" width="100"><template #default="{ row }">{{ row.info.payNum }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="currentOrder.status > 0" label="发货记录" name="delivery">
            <el-table :data="currentOrder.productList || []" size="small" border>
              <el-table-column min-width="420">
                <template #header>
                  <span class="font-color">【{{ deliveryTypeText(currentOrder) }}】</span>
                  <span v-if="currentOrder.deliveryType !== 'noNeed'">{{ currentOrder.deliveryName || '-' }}：{{ currentOrder.deliveryId || '-' }}</span>
                  <span class="ml30">{{ formatDateTime(currentOrder.deliveryTime || currentOrder.createTime) }}</span>
                </template>
                <template #default="{ row }">
                  <div class="tabBox acea-row row-middle">
                    <div class="demo-image__preview mr10"><el-image :src="row.info.image" :preview-src-list="[row.info.image]" fit="cover" /></div>
                    <div class="text_overflow"><span class="tabBox_tit mr10">{{ row.info.productName }}</span><span class="tabBox_pice">规格：{{ row.info.sku || '-' }}</span></div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column width="120"><template #default="{ row }">x {{ row.info.payNum }}</template></el-table-column>
              <el-table-column width="180" align="right">
                <template #header>
                  <el-button
                    v-if="currentOrder.deliveryType === 'express'"
                    link
                    type="primary"
                    @click="openLogistics(currentOrder)"
                  >
                    查看物流
                  </el-button>
                </template>
                <template #default>
                  <span v-if="currentOrder.deliveryType === 'noNeed'" class="muted-tip">无需物流</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-dialog v-model="logisticsDialogVisible" title="物流信息" width="520px" destroy-on-close>
      <div v-if="currentOrder" class="logistics-card">
        <div class="logistics-row">
          <span class="logistics-label">配送方式：</span>
          <span>{{ deliveryTypeText(currentOrder) }}</span>
        </div>
        <div class="logistics-row">
          <span class="logistics-label">物流公司：</span>
          <span>{{ currentOrder.deliveryName || '-' }}</span>
        </div>
        <div class="logistics-row">
          <span class="logistics-label">物流单号：</span>
          <span>{{ currentOrder.deliveryId || '-' }}</span>
        </div>
        <div class="logistics-row">
          <span class="logistics-label">物流编码：</span>
          <span>{{ currentOrder.deliveryCode || '-' }}</span>
        </div>
        <div class="logistics-row">
          <span class="logistics-label">发货类型：</span>
          <span>{{ expressRecordTypeText(currentOrder.expressRecordType) }}</span>
        </div>
        <div class="logistics-row">
          <span class="logistics-label">发货时间：</span>
          <span>{{ formatDateTime(currentOrder.deliveryTime || currentOrder.createTime) }}</span>
        </div>
        <el-alert
          class="logistics-safe-tip"
          type="info"
          show-icon
          :closable="false"
          title="当前展示订单发货字段；第三方快递轨迹需配置物流服务后启用。"
        />
      </div>
      <template #footer>
        <el-button type="primary" @click="logisticsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sendDialogVisible" title="发送货" width="620px" destroy-on-close>
      <el-form label-width="110px" size="small">
        <el-form-item label="选择类型：">
          <el-radio-group v-model="sendForm.deliveryType" @change="resetSendFields">
            <el-radio label="express">发货</el-radio>
            <el-radio label="send">送货</el-radio>
            <el-radio label="fictitious">虚拟</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="sendForm.deliveryType === 'express'">
          <el-form-item label="发货类型：">
            <el-radio-group v-model="sendForm.expressRecordType">
              <el-radio label="1">手动填写</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="快递公司：">
            <el-select v-model="sendForm.expressCode" filterable placeholder="请选择快递公司" style="width: 100%" @change="handleExpressChange">
              <el-option v-for="item in expressOptions" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="快递单号：">
            <el-input v-model="sendForm.expressNumber" placeholder="请输入快递单号" />
          </el-form-item>
        </template>
        <template v-if="sendForm.deliveryType === 'send'">
          <el-form-item label="送货人姓名："><el-input v-model="sendForm.deliveryName" placeholder="请输入送货人姓名" /></el-form-item>
          <el-form-item label="送货人电话："><el-input v-model="sendForm.deliveryTel" placeholder="请输入送货人电话" /></el-form-item>
        </template>
        <el-form-item v-if="sendForm.deliveryType === 'fictitious'" label="发货说明：">
          <span class="muted-tip">该订单将按老系统“无需发货/虚拟发货”流程处理。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="submitSend">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" title="操作记录" width="720px" destroy-on-close>
      <el-table v-loading="logLoading" :data="logRows" size="small" border>
        <el-table-column prop="oid" label="订单ID" width="100" />
        <el-table-column prop="changeMessage" label="操作记录" min-width="360" show-overflow-tooltip />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="logQuery.page"
          v-model:page-size="logQuery.limit"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="logTotal"
          background
          @size-change="loadOrderLog"
          @current-change="loadOrderLog"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Setting } from '@element-plus/icons-vue';
import {
  expressList,
  orderDelete,
  orderExport,
  orderInfo,
  orderList,
  orderMark,
  orderPrint,
  orderRefund,
  orderRefundRefuse,
  orderSend,
  orderStatusList,
  orderStatusNum,
  orderUpdatePrice,
  orderWriteUpdate
} from '../api';

const orderTypeOptions = [
  { value: 2, label: '全部' },
  { value: 0, label: '普通订单' }
];

const statusTabs = [
  { name: 'all', label: '全部' },
  { name: 'unPaid', label: '未支付' },
  { name: 'notShipped', label: '未发货' },
  { name: 'spike', label: '待收货' },
  { name: 'bargain', label: '待评价' },
  { name: 'complete', label: '交易完成' },
  { name: 'toBeWrittenOff', label: '待核销' },
  { name: 'refunding', label: '退款中' },
  { name: 'refunded', label: '已退款' },
  { name: 'refundRefused', label: '已拒绝' },
  { name: 'deleted', label: '已删除' }
];

const columnData = ['订单号', '订单类型', '收货人', '商品信息', '实际支付', '支付方式', '订单状态', '下单时间'];
const query = reactive({
  status: 'all',
  dateLimit: '',
  orderNo: '',
  page: 1,
  limit: 20,
  type: 2
});

const timeValue = ref([]);
const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const statusCounts = ref({});
const cardSelectVisible = ref(false);
const detailDrawerVisible = ref(false);
const detailTab = ref('detail');
const currentOrder = ref(null);
const logisticsDialogVisible = ref(false);
const sendDialogVisible = ref(false);
const sendLoading = ref(false);
const sendOrderRow = ref(null);
const expressOptions = ref([]);
const logDialogVisible = ref(false);
const logLoading = ref(false);
const logRows = ref([]);
const logTotal = ref(0);
const exportLoading = ref(false);
const logQuery = reactive({
  page: 1,
  limit: 10,
  orderNo: ''
});
const visibleColumns = ref(loadColumns());
const checkAll = ref(visibleColumns.value.length === columnData.length);
const isIndeterminate = ref(visibleColumns.value.length > 0 && visibleColumns.value.length < columnData.length);
const sendForm = reactive({
  deliveryType: 'express',
  expressRecordType: '1',
  expressCode: '',
  expressName: '',
  expressNumber: '',
  deliveryName: '',
  deliveryTel: ''
});

function loadColumns() {
  const saved = localStorage.getItem('order_stroge');
  if (!saved) return [...columnData];
  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) && parsed.length ? parsed : [...columnData];
  } catch {
    return [...columnData];
  }
}

function handleTimeChange(value) {
  query.dateLimit = Array.isArray(value) && value.length === 2 ? value.join(',') : '';
  searchList();
}

function searchList() {
  query.page = 1;
  loadStatusNum();
  loadList();
}

function handleReset() {
  query.type = 2;
  query.dateLimit = '';
  query.orderNo = '';
  query.status = 'all';
  timeValue.value = [];
  searchList();
}

async function loadStatusNum() {
  statusCounts.value = await orderStatusNum({
    dateLimit: query.dateLimit,
    type: query.type,
    orderNo: query.orderNo
  });
}

async function loadList() {
  loading.value = true;
  try {
    const data = await orderList({ ...query });
    tableData.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function handleExport() {
  exportLoading.value = true;
  try {
    const data = await orderExport({
      status: query.status,
      dateLimit: query.dateLimit,
      orderNo: query.orderNo,
      type: query.type
    });
    const fileName = data?.fileName;
    if (!fileName) {
      ElMessage.error('导出文件生成失败');
      return;
    }
    window.open(fileName, '_blank');
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
}

async function handleDetail(row) {
  detailTab.value = 'detail';
  currentOrder.value = await orderInfo({ orderNo: row.orderId });
  detailDrawerVisible.value = true;
}

async function loadOrderLog() {
  if (!logQuery.orderNo) return;
  logLoading.value = true;
  try {
    const data = await orderStatusList({ ...logQuery });
    logRows.value = data.list || [];
    logTotal.value = data.total || 0;
  } finally {
    logLoading.value = false;
  }
}

async function handleOrderLog(row) {
  logQuery.page = 1;
  logQuery.orderNo = row.orderId;
  logDialogVisible.value = true;
  await loadOrderLog();
}

async function handleOrderMark(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入订单备注', '订单备注', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row.mark || '',
      inputValidator: (value) => value !== undefined && value !== null ? true : '请填写备注'
    });
    await orderMark({ orderNo: row.orderId, mark: value.trim() });
    ElMessage.success('备注成功');
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function handleEditPrice(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入实际支付金额', '编辑订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: String(row.payPrice ?? 0),
      inputPattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      inputErrorMessage: '请输入正确的金额'
    });
    await orderUpdatePrice({ orderNo: row.orderId, payPrice: value });
    ElMessage.success('修改成功');
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function handleSend(row) {
  sendOrderRow.value = row;
  resetSendForm();
  await loadExpressOptions();
  sendDialogVisible.value = true;
}

async function loadExpressOptions() {
  if (expressOptions.value.length) return;
  const data = await expressList({ page: 1, limit: 999, isShow: 1 });
  expressOptions.value = Array.isArray(data?.list) ? data.list : [];
}

function resetSendForm() {
  sendForm.deliveryType = 'express';
  sendForm.expressRecordType = '1';
  sendForm.expressCode = '';
  sendForm.expressName = '';
  sendForm.expressNumber = '';
  sendForm.deliveryName = '';
  sendForm.deliveryTel = '';
}

function resetSendFields() {
  sendForm.expressCode = '';
  sendForm.expressName = '';
  sendForm.expressNumber = '';
  sendForm.deliveryName = '';
  sendForm.deliveryTel = '';
}

function handleExpressChange(code) {
  const company = expressOptions.value.find((item) => item.code === code);
  sendForm.expressName = company?.name || '';
}

async function submitSend() {
  if (!sendOrderRow.value?.orderId) return;
  const payload = { orderNo: sendOrderRow.value.orderId, deliveryType: sendForm.deliveryType };
  if (sendForm.deliveryType === 'express') {
    if (!sendForm.expressCode) {
      ElMessage.error('请选择快递公司');
      return;
    }
    if (!sendForm.expressNumber.trim()) {
      ElMessage.error('请输入快递单号');
      return;
    }
    Object.assign(payload, {
      expressRecordType: sendForm.expressRecordType,
      expressCode: sendForm.expressCode,
      expressName: sendForm.expressName,
      expressNumber: sendForm.expressNumber.trim()
    });
  } else if (sendForm.deliveryType === 'send') {
    if (!sendForm.deliveryName.trim()) {
      ElMessage.error('请输入送货人姓名');
      return;
    }
    if (!sendForm.deliveryTel.trim()) {
      ElMessage.error('请输入送货人电话');
      return;
    }
    Object.assign(payload, {
      deliveryName: sendForm.deliveryName.trim(),
      deliveryTel: sendForm.deliveryTel.trim()
    });
  }
  sendLoading.value = true;
  try {
    await orderSend(payload);
    ElMessage.success('发送货成功');
    sendDialogVisible.value = false;
    query.page = 1;
    await Promise.all([loadStatusNum(), loadList()]);
  } finally {
    sendLoading.value = false;
  }
}

function money(value) {
  if (value === null || value === undefined || value === '') return '-';
  return `￥${value}`;
}

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
  const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function assetUrl(value) {
  if (!value) return '';
  const url = String(value).trim();
  if (!url) return '';
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url;
  return url.startsWith('/') ? url : `/${url}`;
}

function deliveryTypeText(row) {
  if (row.deliveryType === 'express') return '快递配送';
  if (row.deliveryType === 'send') return '商家送货';
  if (row.deliveryType === 'noNeed' || row.deliveryType === 'fictitious') return '虚拟发货';
  return '虚拟发货';
}

function expressRecordTypeText(value) {
  const map = {
    0: '无需面单',
    1: '手动填写',
    2: '电子面单',
    3: '一号通商家寄件'
  };
  return map[Number(value || 0)] || `未知(${value})`;
}

function openLogistics(row) {
  if (!row || row.deliveryType !== 'express') {
    ElMessage.warning('当前订单不是快递配送订单');
    return;
  }
  logisticsDialogVisible.value = true;
}

function refundStatusText(rowOrStatus) {
  if (typeof rowOrStatus === 'object' && rowOrStatus !== null) {
    if (Number(rowOrStatus.refundStatus || 0) === 0 && rowOrStatus.refundReason) {
      return '已拒绝';
    }
    return refundStatusText(rowOrStatus.refundStatus);
  }
  const map = {
    0: '无售后',
    1: '申请退款',
    2: '已退款',
    3: '退款中'
  };
  return map[Number(rowOrStatus || 0)] || `未知(${rowOrStatus})`;
}

function hasRefundInfo(row) {
  if (!row) return false;
  return Number(row.refundStatus || 0) > 0
    || Boolean(row.refundReasonWap)
    || Boolean(row.refundReasonWapExplain)
    || Boolean(row.refundReasonTime)
    || Boolean(row.refundReasonWapImg)
    || Boolean(row.refundReason);
}

function refundImages(row) {
  const source = row?.refundReasonWapImg;
  if (!source) return [];
  if (Array.isArray(source)) return source.map((item) => String(item).trim()).filter(Boolean);
  const text = String(source).trim();
  if (!text) return [];
  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) return parsed.map((item) => String(item).trim()).filter(Boolean);
    } catch {
      return [];
    }
  }
  return text.split(',').map((item) => item.trim()).filter(Boolean);
}

async function handleRefund(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入退款金额。余额支付会退回余额；微信/支付宝退款需配置对应支付服务后启用，未配置时仅更新售后状态和日志。', '立即退款', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: refundableAmount(row),
      inputPattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      inputErrorMessage: '请输入正确的金额'
    });
    await orderRefund({ orderNo: row.orderId, amount: value.trim() });
    ElMessage.success('退款处理已提交');
    await refreshCurrentOrder(row.orderId);
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

function refundableAmount(row) {
  const payPrice = Number(row?.payPrice || 0);
  const refunded = Number(row?.refundPrice || 0);
  const rest = Math.max(0, payPrice - refunded);
  return rest.toFixed(2).replace(/\.00$/, '');
}

async function handleRefundRefuse(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝退款原因', '拒绝退款', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (value) => Boolean(value && value.trim()) || '请填写拒绝退款原因'
    });
    await orderRefundRefuse({ orderNo: row.orderId, reason: value.trim() });
    ElMessage.success('已拒绝退款');
    await refreshCurrentOrder(row.orderId);
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function refreshCurrentOrder(orderId) {
  if (detailDrawerVisible.value && currentOrder.value?.orderId === orderId) {
    currentOrder.value = await orderInfo({ orderNo: orderId });
  }
}

async function handleWriteOff(row) {
  if (!row.verifyCode) {
    ElMessage.error('核销码不存在');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定核销订单 ${row.orderId} 吗？`, '核销订单', {
      confirmButtonText: '确定核销',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await orderWriteUpdate(row.verifyCode);
    ElMessage.success('核销成功');
    query.page = 1;
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function handleDelete(row) {
  if (!row.isDel) {
    ElMessage.warning('用户未删除的订单无法删除');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除订单 ${row.orderId} 吗？`, '删除订单', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await orderDelete({ orderNo: row.orderId });
    ElMessage.success('删除成功');
    query.page = 1;
    await Promise.all([loadStatusNum(), loadList()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function handlePrint(row) {
  await orderPrint(row.orderId);
  ElMessage.success('打印成功');
}

function handleCheckAllChange(value) {
  visibleColumns.value = value ? [...columnData] : [];
  isIndeterminate.value = false;
}

function handleColumnChange(value) {
  checkAll.value = value.length === columnData.length;
  isIndeterminate.value = value.length > 0 && value.length < columnData.length;
}

function saveColumns() {
  localStorage.setItem('order_stroge', JSON.stringify(visibleColumns.value));
  cardSelectVisible.value = false;
  ElMessage.success('保存成功');
}

onMounted(async () => {
  await Promise.all([loadStatusNum(), loadList()]);
});
</script>

<style scoped>
.order-no {
  display: block;
}

.deleted-tip {
  color: #ed4014;
  display: block;
}

.tabBox {
  flex-wrap: inherit;
  margin: 4px 0;
}

.demo-image__preview {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
}

.demo-image__preview :deep(.el-image) {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.text_overflow {
  min-width: 0;
  line-height: 18px;
}

.tabBox_tit {
  display: block;
  max-width: 330px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tabBox_pice {
  color: #909399;
}

.refunding {
  color: #f124c7;
  cursor: pointer;
}

.refund-image {
  width: 35px;
  height: 35px;
  display: inline-block;
  margin-left: 5px;
}

.refund-image :deep(.el-image) {
  width: 35px;
  height: 35px;
}

.el-dropdown-link {
  display: inline-flex;
  align-items: center;
  color: #409eff;
  cursor: pointer;
}

.card_abs {
  position: absolute;
  padding-bottom: 15px;
  top: 260px;
  right: 40px;
  width: 200px;
  background: #fff;
  z-index: 10;
  box-shadow: 0 0 14px rgba(0, 0, 0, 0.1);
}

.cell_ht {
  height: 50px;
  padding: 15px 20px;
  box-sizing: border-box;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.check_cell {
  width: 100%;
  padding: 15px 20px 0;
}

.operation-header {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.column-setting {
  font-size: 14px;
  color: #606266;
}

.order-detail {
  padding: 0 20px 20px;
}

.detail-product-table {
  margin-top: 18px;
}

.detailHead {
  display: flex;
  align-items: stretch;
  min-height: 96px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  background: #fff;
}

.detailHead .full {
  display: flex;
  align-items: center;
  width: 38%;
  padding: 18px 20px;
  border-right: 1px solid #ebeef5;
}

.detailHead .order_icon {
  width: 42px;
  height: 42px;
  margin-right: 14px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  line-height: 42px;
  text-align: center;
}

.detailHead .text .title {
  margin-bottom: 8px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.detailHead .list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  flex: 1;
  margin: 0;
  padding: 0;
  list-style: none;
}

.detailHead .list .item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 18px;
  border-right: 1px solid #ebeef5;
}

.detailHead .list .item:last-child {
  border-right: 0;
}

.detailHead .list .title {
  margin-bottom: 8px;
  color: #909399;
  font-size: 13px;
}

.color-warning {
  color: #e6a23c;
}

.order-detail-tabs {
  min-height: 420px;
}

.detailSection {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.detailSection:first-child {
  padding-top: 4px;
}

.detailSection > .title {
  margin-bottom: 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.detailSection .list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.detailSection .item {
  display: flex;
  min-width: 0;
  color: #606266;
  font-size: 14px;
}

.detailSection .lang {
  flex: 0 0 90px;
  color: #909399;
}

.detailSection .value {
  min-width: 0;
  color: #303133;
  word-break: break-all;
}

.refund-action-row {
  margin-top: 14px;
  padding-left: 90px;
}

.font-color {
  color: #409eff;
  font-weight: 600;
}

.ml30 {
  margin-left: 30px;
}

.mr20 {
  margin-right: 20px;
}

.muted-tip {
  color: #909399;
}

.logistics-card {
  padding: 4px 0;
}

.logistics-row {
  display: flex;
  min-height: 32px;
  align-items: center;
  color: #303133;
}

.logistics-label {
  flex: 0 0 86px;
  color: #909399;
}

.logistics-safe-tip {
  margin-top: 14px;
}
</style>
