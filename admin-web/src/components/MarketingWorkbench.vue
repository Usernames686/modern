<template>
  <div class="divBox marketing-workbench">
    <el-card shadow="never" class="ivu-mt">
      <div class="workbench-head">
        <div>
          <h2>{{ currentGroup.title }}</h2>
          <p>{{ currentGroup.desc }}</p>
        </div>
        <el-segmented v-model="activeKey" :options="segmentOptions" @change="switchGroup" />
      </div>

      <div class="shortcut-grid">
        <button
          v-for="item in currentGroup.actions"
          :key="item.path"
          type="button"
          class="shortcut-card"
          @click="navigate(item.path)"
        >
          <span class="shortcut-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <strong>{{ item.title }}</strong>
          <em>{{ item.desc }}</em>
        </button>
      </div>
    </el-card>

    <el-card shadow="never" class="mt14">
      <template #header>
        <div class="table-head">
          <span>模块状态</span>
          <el-button link type="primary" @click="navigate(currentGroup.primaryPath)">进入默认页</el-button>
        </div>
      </template>
      <el-table :data="currentGroup.checks" size="small" border>
        <el-table-column prop="name" label="模块" width="160" />
        <el-table-column prop="status" label="状态" width="160">
          <template #default="{ row }">
            <el-tag type="success" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="desc" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { Goods, Money, Plus, Tickets, Timer, TrendCharts } from '@element-plus/icons-vue';

const props = defineProps({
  path: {
    type: String,
    default: '/marketing/index'
  }
});

const groups = {
  overview: {
    title: '营销中心',
    desc: '优惠券、秒杀、砍价、拼团常用入口',
    primaryPath: '/marketing/coupon/list',
    actions: [
      { title: '优惠券列表', desc: '模板、状态、发券入口', path: '/marketing/coupon/list', icon: Tickets },
      { title: '秒杀配置', desc: '场次维护和商品入口', path: '/marketing/seckill/config', icon: Timer },
      { title: '砍价商品', desc: '砍价活动商品管理', path: '/marketing/bargain/bargainGoods', icon: TrendCharts },
      { title: '拼团商品', desc: '开团商品与参团记录', path: '/marketing/groupBuy/groupGoods', icon: Goods }
    ],
    checks: [
      { name: '优惠券', status: '已接入', desc: '列表、添加/复制、发放、领取记录' },
      { name: '秒杀', status: '已接入', desc: '配置、商品列表、新建/编辑、状态切换' },
      { name: '砍价', status: '已接入', desc: '活动商品、参与记录、帮砍详情' },
      { name: '拼团', status: '已接入', desc: '活动商品、开团列表、参团订单详情' }
    ]
  },
  coupon: {
    title: '优惠券',
    desc: '后台优惠券模板与领取记录',
    primaryPath: '/marketing/coupon/list',
    actions: [
      { title: '优惠券列表', desc: '查看、启停、删除优惠券', path: '/marketing/coupon/list', icon: Tickets },
      { title: '添加优惠券', desc: '创建满减/商品/分类券', path: '/marketing/coupon/list/save', icon: Plus },
      { title: '领取记录', desc: '查询用户领取与使用情况', path: '/marketing/coupon/record', icon: Money }
    ],
    checks: [
      { name: '列表', status: '已接入', desc: '支持搜索、状态筛选、分页' },
      { name: '表单', status: '已接入', desc: '支持新增和复制模板' },
      { name: '记录', status: '已接入', desc: '支持领取记录查询与发放' }
    ]
  },
  seckill: {
    title: '秒杀管理',
    desc: '秒杀场次、活动商品和活动数据管理',
    primaryPath: '/marketing/seckill/config',
    actions: [
      { title: '秒杀配置', desc: '维护秒杀场次', path: '/marketing/seckill/config', icon: Timer },
      { title: '秒杀商品', desc: '查询和上下架秒杀商品', path: '/marketing/seckill/list', icon: Goods },
      { title: '添加秒杀商品', desc: '选择商品并配置活动价', path: '/marketing/seckill/creatSeckill/creat', icon: Plus }
    ],
    checks: [
      { name: '场次', status: '已接入', desc: '配置列表、添加、编辑、启停' },
      { name: '商品', status: '已接入', desc: '商品列表、详情、导出、状态切换' },
      { name: '快捷入口', status: '可用', desc: '相关秒杀入口会进入当前模块' }
    ]
  },
  bargain: {
    title: '砍价管理',
    desc: '砍价商品和用户砍价记录',
    primaryPath: '/marketing/bargain/bargainGoods',
    actions: [
      { title: '砍价商品', desc: '商品活动列表', path: '/marketing/bargain/bargainGoods', icon: Goods },
      { title: '添加砍价商品', desc: '新增砍价活动', path: '/marketing/bargain/creatBargain', icon: Plus },
      { title: '砍价列表', desc: '用户参与记录', path: '/marketing/bargain/bargainList', icon: TrendCharts }
    ],
    checks: [
      { name: '商品', status: '已接入', desc: '列表、详情、编辑、删除、导出' },
      { name: '记录', status: '已接入', desc: '参与记录、帮砍明细' }
    ]
  },
  groupBuy: {
    title: '拼团管理',
    desc: '拼团商品和开团列表',
    primaryPath: '/marketing/groupBuy/groupGoods',
    actions: [
      { title: '拼团商品', desc: '商品活动列表', path: '/marketing/groupBuy/groupGoods', icon: Goods },
      { title: '添加拼团商品', desc: '新增拼团活动', path: '/marketing/groupBuy/creatGroup', icon: Plus },
      { title: '拼团列表', desc: '开团/参团记录', path: '/marketing/groupBuy/groupList', icon: TrendCharts }
    ],
    checks: [
      { name: '商品', status: '已接入', desc: '列表、详情、编辑、删除、导出' },
      { name: '开团', status: '已接入', desc: '开团列表、参团订单详情' }
    ]
  }
};

const segmentOptions = [
  { label: '总览', value: 'overview' },
  { label: '优惠券', value: 'coupon' },
  { label: '秒杀', value: 'seckill' },
  { label: '砍价', value: 'bargain' },
  { label: '拼团', value: 'groupBuy' }
];

const activeKey = ref(pathToKey(props.path));
const currentGroup = computed(() => groups[activeKey.value] || groups.overview);

watch(
  () => props.path,
  (value) => {
    activeKey.value = pathToKey(value);
  }
);

function pathToKey(path) {
  if (path.includes('/coupon')) return 'coupon';
  if (path.includes('/seckill') || path.includes('/spike')) return 'seckill';
  if (path.includes('/bargain')) return 'bargain';
  if (path.includes('/groupBuy')) return 'groupBuy';
  return 'overview';
}

function switchGroup(value) {
  const target = {
    overview: '/marketing/index',
    coupon: '/marketing/coupon/index',
    seckill: '/marketing/seckill/index',
    bargain: '/marketing/bargain/index',
    groupBuy: '/marketing/groupBuy/index'
  }[value];
  navigate(target);
}

function navigate(path) {
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}
</script>

<style scoped>
.workbench-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.workbench-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
}

.workbench-head p {
  margin: 0;
  color: #909399;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.shortcut-card {
  min-height: 104px;
  padding: 18px;
  border: 1px solid #ebeef5;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.shortcut-card:hover {
  border-color: #409eff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
}

.shortcut-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  border-radius: 6px;
  background: #ecf5ff;
  color: #409eff;
}

.shortcut-card strong {
  display: block;
  margin-bottom: 6px;
  font-size: 15px;
  color: #303133;
}

.shortcut-card em {
  display: block;
  font-style: normal;
  color: #909399;
  line-height: 1.5;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
