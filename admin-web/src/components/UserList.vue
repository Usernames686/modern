<template>
  <div class="divBox relative">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline size="small" label-width="75px">
          <div class="acea-row search-form row-between">
            <div class="search-form-box">
              <el-form-item label="用户搜索：">
                <el-input v-model="query.keywords" placeholder="请输入姓名或手机号" clearable class="selWidth" @keyup.enter="searchList" />
              </el-form-item>
              <el-form-item label="用户等级：">
                <el-select v-model="levelData" placeholder="请选择" class="selWidth" clearable filterable multiple @change="searchList">
                  <el-option v-for="item in levelList" :key="item.id" :value="item.id" :label="item.name" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item v-if="!collapse" class="search-form-sub">
              <el-button type="primary" @click="searchList">搜索</el-button>
              <el-button class="ResetSearch mr14" @click="handleReset">重置</el-button>
              <a class="ivu-ml-8" @click="collapse = !collapse">
                展开 <el-icon><ArrowDown /></el-icon>
              </a>
            </el-form-item>
          </div>
          <div v-if="collapse" class="acea-row search-form">
            <div class="search-form-box">
              <el-form-item label="用户分组：">
                <el-select v-model="groupData" placeholder="请选择" class="selWidth" clearable filterable multiple @change="searchList">
                  <el-option v-for="item in groupList" :key="item.id" :value="item.id" :label="item.groupName" />
                </el-select>
              </el-form-item>
              <el-form-item label="用户标签：">
                <el-select v-model="labelData" placeholder="请选择" class="selWidth" clearable filterable multiple @change="searchList">
                  <el-option v-for="item in tagList" :key="item.id" :value="item.id" :label="item.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="国家：">
                <el-select v-model="query.country" placeholder="请选择" class="selWidth" clearable @change="searchList">
                  <el-option value="" label="全部" />
                  <el-option value="CN" label="中国" />
                  <el-option value="OTHER" label="国外" />
                </el-select>
              </el-form-item>
              <el-form-item label="消费情况：">
                <el-select v-model="query.payCount" placeholder="请选择" class="selWidth" clearable @change="searchList">
                  <el-option value="" label="全部" />
                  <el-option value="0" label="0" />
                  <el-option value="1" label="1+" />
                  <el-option value="2" label="2+" />
                  <el-option value="3" label="3+" />
                  <el-option value="4" label="4+" />
                  <el-option value="5" label="5+" />
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
              <el-form-item label="访问情况：">
                <el-select v-model="query.accessType" placeholder="请选择" class="selWidth" clearable @change="searchList">
                  <el-option :value="0" label="全部" />
                  <el-option :value="1" label="首次访问" />
                  <el-option :value="2" label="时间段访问过" />
                  <el-option :value="3" label="时间段未访问" />
                </el-select>
              </el-form-item>
              <el-form-item label="性别：">
                <el-select v-model="query.sex" placeholder="请选择" class="selWidth" @change="searchList">
                  <el-option value="" label="全部" />
                  <el-option value="0" label="未知" />
                  <el-option value="1" label="男" />
                  <el-option value="2" label="女" />
                  <el-option value="3" label="保密" />
                </el-select>
              </el-form-item>
              <el-form-item label="身份：">
                <el-select v-model="query.isPromoter" placeholder="请选择" class="selWidth" @change="searchList">
                  <el-option value="" label="全部" />
                  <el-option :value="1" label="推广员" />
                  <el-option :value="0" label="普通用户" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item class="search-form-sub-bottom">
              <el-button type="primary" @click="searchList">搜索</el-button>
              <el-button class="ResetSearch mr14" @click="handleReset">重置</el-button>
              <a class="ivu-ml-8" @click="collapse = !collapse">
                收起 <el-icon><ArrowUp /></el-icon>
              </a>
            </el-form-item>
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-tabs v-model="loginType" @tab-change="handleTabChange">
            <el-tab-pane v-for="item in headerTabs" :key="item.type" :label="item.name" :name="item.type" />
          </el-tabs>
          <div>
            <el-button type="primary" @click="openCouponDialog">发送优惠券</el-button>
            <el-button :disabled="!selectionList.length" @click="openBatchDialog('group')">批量设置分组</el-button>
            <el-button :disabled="!selectionList.length" @click="openBatchDialog('tag')">批量设置标签</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="listError"
        class="list-error"
        type="error"
        :title="listError"
        show-icon
        :closable="false"
      />

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        :empty-text="listError ? '用户列表加载失败' : '暂无用户数据'"
        style="width: 100%"
        size="small"
        highlight-current-row
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-form label-position="left" inline class="demo-table-expand">
              <el-form-item label="身份："><span>{{ promoterText(row.isPromoter) }}</span></el-form-item>
              <el-form-item label="首次访问："><span>{{ formatDateTime(row.createTime) }}</span></el-form-item>
              <el-form-item label="近次访问："><span>{{ formatDateTime(row.lastLoginTime) }}</span></el-form-item>
              <el-form-item label="手机号："><span>{{ row.phone || '-' }}</span></el-form-item>
              <el-form-item label="标签："><span>{{ row.tagName || '-' }}</span></el-form-item>
              <el-form-item label="地址："><span>{{ row.addres || '-' }}</span></el-form-item>
              <el-form-item label="备注：" class="mark-item"><span>{{ row.mark || '-' }}</span></el-form-item>
            </el-form>
          </template>
        </el-table-column>
        <el-table-column type="selection" width="55" />
        <el-table-column v-if="visibleColumns.includes('ID')" prop="uid" label="ID" min-width="80" />
        <el-table-column v-if="visibleColumns.includes('头像')" label="头像" min-width="80">
          <template #default="{ row }">
            <div class="demo-image__preview">
              <el-image :src="row.avatar" :preview-src-list="[row.avatar]" fit="cover">
                <template #error>
                  <div class="image-slot"><el-icon><UserFilled /></el-icon></div>
                </template>
              </el-image>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('姓名')" label="姓名" min-width="160">
          <template #default="{ row }">{{ row.nickname || '-' }} | {{ sexText(row.sex) }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('用户等级')" label="用户等级" min-width="100">
          <template #default="{ row }">{{ levelName(row.level) }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('分组')" label="分组" min-width="100">
          <template #default="{ row }">{{ row.groupName || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('推荐人')" prop="spreadNickname" label="推荐人" min-width="130" />
        <el-table-column v-if="visibleColumns.includes('手机号')" label="手机号" min-width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('余额')" prop="nowMoney" label="余额" min-width="100" />
        <el-table-column v-if="visibleColumns.includes('积分')" prop="integral" label="积分" min-width="100" />
        <el-table-column label="操作" width="170" fixed="right">
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
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-dropdown trigger="click">
              <span class="el-dropdown-link">更多<el-icon><ArrowDown /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openFundsDialog(row)">积分余额</el-dropdown-item>
                  <el-dropdown-item @click="openBatchDialog('group', row)">设置分组</el-dropdown-item>
                  <el-dropdown-item @click="openBatchDialog('tag', row)">设置标签</el-dropdown-item>
                  <el-dropdown-item @click="openPhonePrompt(row)">修改手机号</el-dropdown-item>
                  <el-dropdown-item @click="openLevelDialog(row)">修改用户等级</el-dropdown-item>
                  <el-dropdown-item @click="openSpreadDialog(row)">修改上级推广人</el-dropdown-item>
                  <el-dropdown-item v-if="row.spreadUid && row.spreadUid > 0" @click="handleClearSpread(row)">清除上级推广人</el-dropdown-item>
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

    <div v-show="cardSelectVisible" class="card_abs" :style="{ top: collapse ? '570px' : '270px' }">
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
      title="用户详情"
      direction="rtl"
      size="70%"
      class="showHeader"
      destroy-on-close
    >
      <div v-if="currentUser" class="user-detail">
        <div class="detail-head">
          <el-image class="detail-avatar" :src="currentUser.avatar" :preview-src-list="[currentUser.avatar]" fit="cover">
            <template #error>
              <div class="image-slot"><el-icon><UserFilled /></el-icon></div>
            </template>
          </el-image>
          <div>
            <h3>{{ currentUser.nickname || '-' }}</h3>
            <p>ID：{{ currentUser.uid }}　{{ currentUser.phone || '-' }}</p>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户等级">{{ levelName(currentUser.level) }}</el-descriptions-item>
          <el-descriptions-item label="身份">{{ promoterText(currentUser.isPromoter) }}</el-descriptions-item>
          <el-descriptions-item label="余额">￥{{ currentUser.nowMoney || 0 }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ currentUser.integral || 0 }}</el-descriptions-item>
          <el-descriptions-item label="分组">{{ currentUser.groupName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ currentUser.tagName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="推荐人">{{ currentUser.spreadNickname || '无' }}</el-descriptions-item>
          <el-descriptions-item label="消费次数">{{ currentUser.payCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="首次访问">{{ formatDateTime(currentUser.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="近次访问">{{ formatDateTime(currentUser.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentUser.addres || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentUser.mark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog v-model="batchDialogVisible" title="设置" width="540px">
      <el-form label-width="90px">
        <el-form-item v-if="batchType === 'group'" label="用户分组：">
          <el-select v-model="batchForm.groupId" placeholder="请选择分组" style="width: 100%" filterable>
            <el-option v-for="item in groupList" :key="item.id" :value="String(item.id)" :label="item.groupName" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="用户标签：">
          <el-select v-model="batchForm.tagIds" placeholder="请选择标签" style="width: 100%" multiple filterable>
            <el-option v-for="item in tagList" :key="item.id" :value="String(item.id)" :label="item.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitBatch">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fundsDialogVisible" title="积分余额" width="540px" :close-on-click-modal="false">
      <el-form :model="fundsForm" label-width="90px">
        <el-form-item label="修改余额：">
          <el-radio-group v-model="fundsForm.moneyType">
            <el-radio :label="1">增加</el-radio>
            <el-radio :label="2">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="余额：">
          <el-input-number v-model="fundsForm.moneyValue" controls-position="right" :precision="2" :step="0.1" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="修改积分：">
          <el-radio-group v-model="fundsForm.integralType">
            <el-radio :label="1">增加</el-radio>
            <el-radio :label="2">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分：">
          <el-input-number v-model="fundsForm.integralValue" controls-position="right" step-strictly :min="0" :max="999999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fundsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitFunds">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="levelDialogVisible" title="设置" width="540px">
      <el-form label-width="90px">
        <el-form-item label="用户等级：">
          <el-select v-model="levelForm.levelId" placeholder="请选择用户等级" style="width: 100%" filterable>
            <el-option v-for="item in levelList" :key="item.id" :value="item.id" :label="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="扣除经验：">
          <el-radio-group v-model="levelForm.isSub">
            <el-radio :label="false">否</el-radio>
            <el-radio :label="true">是</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitLevel">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑" width="640px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="用户编号：">
          <el-input v-model="editForm.uid" disabled />
        </el-form-item>
        <el-form-item label="用户地址：">
          <el-input v-model="editForm.addres" placeholder="请输入用户地址" />
        </el-form-item>
        <el-form-item label="用户备注：">
          <el-input v-model="editForm.mark" type="textarea" placeholder="请输入用户备注" />
        </el-form-item>
        <el-form-item label="用户分组：">
          <el-select v-model="editForm.groupId" placeholder="请选择" style="width: 100%" clearable filterable>
            <el-option v-for="item in groupList" :key="item.id" :value="String(item.id)" :label="item.groupName" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户标签：">
          <el-select v-model="editForm.tagIds" placeholder="请选择" style="width: 100%" clearable filterable multiple>
            <el-option v-for="item in tagList" :key="item.id" :value="String(item.id)" :label="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="推广员：">
          <el-radio-group v-model="editForm.isPromoter">
            <el-radio :label="true">开启</el-radio>
            <el-radio :label="false">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态：">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="true">开启</el-radio>
            <el-radio :label="false">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitEdit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="spreadDialogVisible" title="修改推广人" width="540px" destroy-on-close>
      <el-form :model="spreadForm" label-width="100px">
        <el-form-item label="当前用户：">
          <span>{{ spreadForm.userName }}（ID：{{ spreadForm.userId }}）</span>
        </el-form-item>
        <el-form-item label="用户头像：">
          <div class="spread-picker-row">
            <div class="upLoadPicBox" @click="openSpreadUserDialog">
              <div v-if="spreadForm.image" class="pictrue"><img :src="spreadForm.image" alt="" /></div>
              <div v-else class="upLoad">
                <el-icon><UserFilled /></el-icon>
              </div>
            </div>
            <div class="spread-picker-info">
              <div>{{ spreadForm.spreadName || '请选择推广人' }}</div>
              <div class="muted">点击头像区域选择用户，或手动输入推广人编号。</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="推广人编号：">
          <el-input-number v-model="spreadForm.spreadUid" controls-position="right" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spreadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitSpread">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="spreadUserDialogVisible" class="user-dialog" title="用户列表" width="900px" append-to-body destroy-on-close>
      <el-card shadow="never" class="spread-user-card">
        <template #header>
          <el-form inline @submit.prevent>
            <el-form-item label="用户名称：">
              <el-input v-model="spreadUserQuery.keywords" placeholder="请输入用户名称" class="selWidth" clearable @keyup.enter="searchSpreadUsers" />
              <el-button class="ml30" type="primary" @click="searchSpreadUsers">搜索</el-button>
            </el-form-item>
          </el-form>
        </template>
        <el-table v-loading="spreadUserLoading" :data="spreadUserRows" size="small" row-key="uid">
          <el-table-column label="" width="50">
            <template #default="{ row }">
              <el-radio
                :model-value="spreadForm.spreadUid"
                :label="row.uid"
                @change="selectSpreadUser(row)"
              >
                &nbsp;
              </el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="uid" label="ID" min-width="60" />
          <el-table-column prop="nickname" label="微信用户名称" min-width="130" show-overflow-tooltip />
          <el-table-column label="用户头像" min-width="80">
            <template #default="{ row }">
              <div class="demo-image__preview">
                <el-image :src="row.avatar" :preview-src-list="[row.avatar]" fit="cover">
                  <template #error>
                    <div class="image-slot"><el-icon><UserFilled /></el-icon></div>
                  </template>
                </el-image>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="性别" min-width="80">
            <template #default="{ row }">{{ sexText(row.sex) }}</template>
          </el-table-column>
          <el-table-column label="地区" prop="addres" min-width="130" show-overflow-tooltip />
        </el-table>
        <div class="spread-user-footer">
          <el-pagination
            v-model:current-page="spreadUserQuery.page"
            v-model:page-size="spreadUserQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="sizes, prev, pager, next, jumper"
            :total="spreadUserTotal"
            background
            @size-change="loadSpreadUsers"
            @current-change="loadSpreadUsers"
          />
          <div>
            <el-button @click="spreadUserDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="spreadUserDialogVisible = false">确定</el-button>
          </div>
        </div>
      </el-card>
    </el-dialog>

    <CouponSendDialog v-model="couponDialogVisible" :user-ids="couponTargetIds" @sent="handleCouponSent" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, ArrowUp, Setting, UserFilled } from '@element-plus/icons-vue';
import {
  userGroups,
  userInfo,
  userLevels,
  userList,
  userOperateFunds,
  userClearSpread,
  userSetGroup,
  userSetTag,
  userTags,
  userUpdate,
  userUpdateSpread,
  userUpdateLevel,
  userUpdatePhone
} from '../api';
import CouponSendDialog from './CouponSendDialog.vue';

const headerTabs = [
  { type: '', name: '全部用户' },
  { type: 'wechat', name: '微信公众号用户' },
  { type: 'routine', name: '微信小程序用户' },
  { type: 'h5', name: 'H5用户' }
];
const columnData = ['ID', '头像', '姓名', '用户等级', '分组', '推荐人', '手机号', '余额', '积分'];

const query = reactive({
  labelId: '',
  userType: '',
  sex: '',
  isPromoter: '',
  country: '',
  payCount: '',
  accessType: 0,
  dateLimit: '',
  keywords: '',
  province: '',
  city: '',
  page: 1,
  limit: 20,
  level: '',
  groupId: ''
});

const loginType = ref('');
const collapse = ref(false);
const levelData = ref([]);
const groupData = ref([]);
const labelData = ref([]);
const timeValue = ref([]);
const levelList = ref([]);
const groupList = ref([]);
const tagList = ref([]);
const tableData = ref([]);
const total = ref(0);
const loading = ref(false);
const listError = ref('');
const selectionList = ref([]);
const cardSelectVisible = ref(false);
const detailDrawerVisible = ref(false);
const currentUser = ref(null);
const batchDialogVisible = ref(false);
const fundsDialogVisible = ref(false);
const levelDialogVisible = ref(false);
const editDialogVisible = ref(false);
const spreadDialogVisible = ref(false);
const spreadUserDialogVisible = ref(false);
const couponDialogVisible = ref(false);
const actionLoading = ref(false);
const spreadUserLoading = ref(false);
const batchType = ref('group');
const batchTargetIds = ref('');
const spreadUserRows = ref([]);
const spreadUserTotal = ref(0);
const batchForm = reactive({
  groupId: '',
  tagIds: []
});
const fundsForm = reactive({
  uid: 0,
  moneyType: 2,
  moneyValue: 0,
  integralType: 2,
  integralValue: 0
});
const levelForm = reactive({
  uid: 0,
  levelId: '',
  isSub: false
});
const editForm = reactive({
  uid: 0,
  addres: '',
  mark: '',
  groupId: '',
  tagIds: [],
  isPromoter: false,
  status: true
});
const spreadForm = reactive({
  userId: 0,
  userName: '',
  spreadUid: 0,
  spreadName: '',
  image: ''
});
const spreadUserQuery = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});
const couponTargetIds = ref('');
const visibleColumns = ref(loadColumns());
const checkAll = ref(visibleColumns.value.length === columnData.length);
const isIndeterminate = ref(visibleColumns.value.length > 0 && visibleColumns.value.length < columnData.length);

function loadColumns() {
  const saved = localStorage.getItem('user_stroge');
  if (!saved) return [...columnData];
  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) && parsed.length ? parsed : [...columnData];
  } catch {
    return [...columnData];
  }
}

function handleTabChange() {
  query.userType = loginType.value;
  searchList();
}

function handleTimeChange(value) {
  query.dateLimit = Array.isArray(value) && value.length === 2 ? value.join(',') : '';
  searchList();
}

function searchList() {
  query.page = 1;
  loadList();
}

function handleReset() {
  query.labelId = '';
  query.userType = '';
  query.sex = '';
  query.isPromoter = '';
  query.country = '';
  query.payCount = '';
  query.accessType = 0;
  query.dateLimit = '';
  query.keywords = '';
  query.province = '';
  query.city = '';
  query.level = '';
  query.groupId = '';
  loginType.value = '';
  levelData.value = [];
  groupData.value = [];
  labelData.value = [];
  timeValue.value = [];
  searchList();
}

async function loadList() {
  loading.value = true;
  listError.value = '';
  try {
    query.userType = loginType.value;
    query.level = levelData.value.join(',');
    query.groupId = groupData.value.join(',');
    query.labelId = labelData.value.join(',');
    const data = await userList({ ...query });
    tableData.value = data.list || [];
    total.value = data.total || 0;
  } catch (error) {
    tableData.value = [];
    total.value = 0;
    listError.value = error?.message || '用户列表加载失败';
  } finally {
    loading.value = false;
  }
  cardSelectVisible.value = false;
}

function handleSelectionChange(rows) {
  selectionList.value = rows;
}

function handleDetail(row) {
  currentUser.value = row;
  detailDrawerVisible.value = true;
}

async function openEditDialog(row) {
  const detail = await userInfo({ id: row.uid });
  editForm.uid = detail.uid;
  editForm.addres = detail.addres || '';
  editForm.mark = detail.mark || '';
  editForm.groupId = detail.groupId || '';
  editForm.tagIds = detail.tagId ? detail.tagId.split(',').filter(Boolean) : [];
  editForm.isPromoter = Boolean(detail.isPromoter);
  editForm.status = Boolean(detail.status);
  editDialogVisible.value = true;
}

function selectedIds(row) {
  if (row) return String(row.uid);
  return selectionList.value.map((item) => item.uid).join(',');
}

function openBatchDialog(type, row = null) {
  const ids = selectedIds(row);
  if (!ids) {
    ElMessage.warning('请先选择用户');
    return;
  }
  batchType.value = type;
  batchTargetIds.value = ids;
  batchForm.groupId = row?.groupId || '';
  batchForm.tagIds = row?.tagId ? row.tagId.split(',').filter(Boolean) : [];
  batchDialogVisible.value = true;
}

function openCouponDialog() {
  if (selectionList.value.length === 0) {
    ElMessage.warning('请选择要设置的用户');
    return;
  }
  couponTargetIds.value = selectedIds();
  couponDialogVisible.value = true;
}

function handleCouponSent() {
  selectionList.value = [];
}

async function submitBatch() {
  actionLoading.value = true;
  try {
    if (batchType.value === 'group') {
      if (!batchForm.groupId) {
        ElMessage.warning('请选择用户分组');
        return;
      }
      await userSetGroup({ id: batchTargetIds.value, groupId: batchForm.groupId });
    } else {
      if (!batchForm.tagIds.length) {
        ElMessage.warning('请选择用户标签');
        return;
      }
      await userSetTag({ id: batchTargetIds.value, tagId: batchForm.tagIds.join(',') });
    }
    ElMessage.success('设置成功');
    batchDialogVisible.value = false;
    await loadList();
  } finally {
    actionLoading.value = false;
  }
}

function openFundsDialog(row) {
  fundsForm.uid = row.uid;
  fundsForm.moneyType = 2;
  fundsForm.moneyValue = 0;
  fundsForm.integralType = 2;
  fundsForm.integralValue = 0;
  fundsDialogVisible.value = true;
}

async function submitFunds() {
  actionLoading.value = true;
  try {
    await userOperateFunds({ ...fundsForm });
    ElMessage.success('操作成功');
    fundsDialogVisible.value = false;
    await loadList();
  } finally {
    actionLoading.value = false;
  }
}

function openLevelDialog(row) {
  levelForm.uid = row.uid;
  levelForm.levelId = row.level || '';
  levelForm.isSub = false;
  levelDialogVisible.value = true;
}

function openSpreadDialog(row) {
  spreadForm.userId = row.uid;
  spreadForm.userName = row.nickname || '-';
  spreadForm.spreadUid = row.spreadUid || 0;
  spreadForm.spreadName = row.spreadNickname && row.spreadNickname !== '无' ? row.spreadNickname : '';
  spreadForm.image = '';
  spreadDialogVisible.value = true;
}

async function openSpreadUserDialog() {
  spreadUserDialogVisible.value = true;
  spreadUserQuery.page = 1;
  await loadSpreadUsers();
}

function searchSpreadUsers() {
  spreadUserQuery.page = 1;
  loadSpreadUsers();
}

async function loadSpreadUsers() {
  spreadUserLoading.value = true;
  try {
    const data = await userList({
      page: spreadUserQuery.page,
      limit: spreadUserQuery.limit,
      keywords: spreadUserQuery.keywords || undefined
    });
    spreadUserRows.value = data?.list || [];
    spreadUserTotal.value = Number(data?.total || 0);
  } finally {
    spreadUserLoading.value = false;
  }
}

function selectSpreadUser(row) {
  if (Number(row.uid) === Number(spreadForm.userId)) {
    ElMessage.warning('推广人不能为当前用户');
    return;
  }
  spreadForm.spreadUid = row.uid;
  spreadForm.spreadName = row.nickname || '';
  spreadForm.image = row.avatar || '';
}

async function handleClearSpread(row) {
  try {
    await ElMessageBox.confirm(`确定清除【${row.nickname || row.uid}】的上级推广人吗`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await userClearSpread(row.uid);
    ElMessage.success('清除成功');
    await loadList();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

async function submitLevel() {
  if (!levelForm.levelId) {
    ElMessage.warning('请选择用户等级');
    return;
  }
  actionLoading.value = true;
  try {
    await userUpdateLevel({
      uid: levelForm.uid,
      levelId: levelForm.levelId,
      isSub: levelForm.isSub
    });
    ElMessage.success('设置成功');
    levelDialogVisible.value = false;
    await loadList();
  } finally {
    actionLoading.value = false;
  }
}

async function submitEdit() {
  actionLoading.value = true;
  try {
    await userUpdate({ id: editForm.uid }, {
      uid: editForm.uid,
      addres: editForm.addres,
      mark: editForm.mark,
      groupId: editForm.groupId,
      tagId: editForm.tagIds.join(','),
      isPromoter: editForm.isPromoter,
      status: editForm.status
    });
    ElMessage.success('编辑成功');
    editDialogVisible.value = false;
    await loadList();
  } finally {
    actionLoading.value = false;
  }
}

async function submitSpread() {
  if (!spreadForm.spreadUid) {
    ElMessage.warning('请输入推广人编号');
    return;
  }
  actionLoading.value = true;
  try {
    await userUpdateSpread({
      userId: spreadForm.userId,
      image: spreadForm.image || '',
      spreadUid: spreadForm.spreadUid
    });
    ElMessage.success('设置成功');
    spreadDialogVisible.value = false;
    await loadList();
  } finally {
    actionLoading.value = false;
  }
}

async function openPhonePrompt(row) {
  let result;
  try {
    result = await ElMessageBox.prompt('', '修改手机号', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'text',
      inputPlaceholder: '请输入手机号',
      closeOnClickModal: false,
      inputValidator: (value) => Boolean(value) || '请填写手机号'
    });
  } catch {
    return;
  }
  await userUpdatePhone({ id: row.uid, phone: result.value });
  ElMessage.success('编辑成功');
  await loadList();
}

function sexText(value) {
  return ({ 0: '未知', 1: '男', 2: '女', 3: '保密' })[value] || '未知';
}

function promoterText(value) {
  return value ? '推广员' : '普通用户';
}

function levelName(value) {
  return levelList.value.find((item) => Number(item.id) === Number(value))?.name || '-';
}

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
  const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
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
  localStorage.setItem('user_stroge', JSON.stringify(visibleColumns.value));
  cardSelectVisible.value = false;
  ElMessage.success('保存成功');
}

onMounted(async () => {
  try {
    const [groups, tags, levels] = await Promise.all([
      userGroups({ page: 1, limit: 9999 }),
      userTags({ page: 1, limit: 9999 }),
      userLevels()
    ]);
    groupList.value = groups.list || [];
    tagList.value = tags.list || [];
    levelList.value = levels || [];
    localStorage.setItem('single-admin-levelKey', JSON.stringify(levelList.value));
  } catch (error) {
    listError.value = error?.message || '用户筛选数据加载失败';
  }
  await loadList();
});
</script>

<style scoped>
.search-form {
  width: 100%;
}

.search-form-box {
  flex: 1;
}

.search-form-sub,
.search-form-sub-bottom {
  margin-left: 10px;
}

.mark-item {
  width: 100%;
  display: flex;
  margin-right: 10px;
}

.demo-image__preview {
  width: 36px;
  height: 36px;
}

.demo-image__preview :deep(.el-image) {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.image-slot {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
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

.user-detail {
  padding: 0 20px 20px;
}

.list-error {
  margin-bottom: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.detail-head h3 {
  margin: 0 0 5px;
  font-size: 18px;
}

.detail-head p {
  margin: 0;
  color: #606266;
}

.detail-avatar {
  width: 58px;
  height: 58px;
  border-radius: 4px;
}

.spread-picker-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.spread-picker-row .upLoadPicBox {
  width: 60px;
  height: 60px;
  border: 1px dashed #c0ccda;
  cursor: pointer;
}

.spread-picker-row .pictrue,
.spread-picker-row .pictrue img,
.spread-picker-row .upLoad {
  width: 100%;
  height: 100%;
}

.spread-picker-row .pictrue img {
  object-fit: cover;
}

.spread-picker-row .upLoad {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 22px;
}

.spread-picker-info {
  line-height: 1.6;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.spread-user-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.spread-user-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
}
</style>
