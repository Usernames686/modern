<template>
  <div class="product-detail">
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else>
      <el-tabs v-model="currentTab" class="list-tabs">
        <el-tab-pane label="商品信息" name="0" />
        <el-tab-pane label="规格库存" name="1" />
        <el-tab-pane label="商品详情" name="2" />
        <el-tab-pane label="其他设置" name="3" />
      </el-tabs>

      <el-form class="formValidate mt20" label-width="90px">
        <el-row v-show="currentTab === '0'" :gutter="24">
          <el-col :span="12">
            <el-form-item label="商品名称：">
              <el-input :model-value="detail.storeName" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类：">
              <el-input :model-value="detail.cateValues || detail.cateId" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品关键字：">
              <el-input :model-value="detail.keyword" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位：">
              <el-input :model-value="detail.unitName" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品封面图：">
              <div v-if="detail.image" class="pictrue">
                <el-image class="image" :src="detail.image" :preview-src-list="[detail.image]" fit="cover" />
              </div>
              <div v-else class="upLoad">
                <el-icon><Camera /></el-icon>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品轮播图：">
              <div class="image-list">
                <div v-for="item in sliderImages" :key="item" class="pictrue">
                  <el-image class="image" :src="item" :preview-src-list="sliderImages" fit="cover" />
                </div>
                <span v-if="sliderImages.length === 0">无</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运费模板：">
              <el-input :model-value="detail.tempId" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-show="currentTab === '1'">
          <el-col :span="24">
            <el-form-item label="商品规格：">
              <el-radio-group :model-value="detail.specType" disabled>
                <el-radio :label="false">单规格</el-radio>
                <el-radio :label="true">多规格</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="佣金设置：">
              <el-radio-group :model-value="detail.isSub" disabled>
                <el-radio :label="true">单独设置</el-radio>
                <el-radio :label="false">默认设置</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品属性：" class="labeltop">
              <el-table :data="skuRows" border class="tabNumWidth" size="small">
                <template v-if="detail.specType">
                  <el-table-column v-for="name in attrColumnNames" :key="name" :label="name" min-width="80">
                    <template #default="{ row }">
                      <span>{{ row.attrMap[name] || '-' }}</span>
                    </template>
                  </el-table-column>
                </template>
                <el-table-column label="图片" min-width="80">
                  <template #default="{ row }">
                    <div v-if="row.image" class="tabPic">
                      <el-image class="image" :src="row.image" :preview-src-list="[row.image]" fit="cover" />
                    </div>
                    <div v-else class="upLoad tabPic">
                      <el-icon><Camera /></el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="price" label="售价" min-width="100" />
                <el-table-column prop="cost" label="成本价" min-width="100" />
                <el-table-column prop="otPrice" label="原价" min-width="100" />
                <el-table-column prop="stock" label="库存" min-width="100" />
                <el-table-column prop="barCode" label="商品编号" min-width="120" />
                <el-table-column prop="weight" label="重量（KG）" min-width="110" />
                <el-table-column prop="volume" label="体积(m³)" min-width="110" />
              </el-table>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-show="currentTab === '2'">
          <el-col :span="24">
            <el-form-item label="商品详情：">
              <div class="content-preview" v-html="detail.content || '无'"></div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-show="currentTab === '3'">
          <el-col :span="8">
            <el-form-item label="排序：">
              <el-input-number :model-value="detail.sort || 0" disabled controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="积分：">
              <el-input-number :model-value="detail.giveIntegral || 0" disabled controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="虚拟销量：">
              <el-input-number :model-value="detail.ficti || 0" disabled controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品推荐：">
              <el-checkbox-group :model-value="recommendValues" disabled>
                <el-checkbox label="isHot">热卖单品</el-checkbox>
                <el-checkbox label="isBenefit">促销单品</el-checkbox>
                <el-checkbox label="isBest">精品推荐</el-checkbox>
                <el-checkbox label="isNew">首发新品</el-checkbox>
                <el-checkbox label="isGood">优品推荐</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动优先级：">
              <div class="color-list">
                <span v-for="item in detail.activity || []" :key="item" class="color-item">{{ item }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券：">
              <span v-if="!detail.couponIds || detail.couponIds.length === 0">暂无优惠券</span>
              <el-tag v-for="id in detail.couponIds || []" :key="id" class="mr10">{{ id }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Camera } from '@element-plus/icons-vue';
import { productDetail } from '../api';

const props = defineProps({
  productId: {
    type: Number,
    required: true
  }
});

const currentTab = ref('0');
const loading = ref(false);
const detail = reactive({
  activity: [],
  attr: [],
  attrValue: [],
  couponIds: []
});
const skuRows = ref([]);

const sliderImages = computed(() => {
  if (!detail.sliderImage) return [];
  try {
    const parsed = JSON.parse(detail.sliderImage);
    const values = Array.isArray(parsed) ? parsed : Object.values(parsed);
    return values.map(normalizeAsset).filter(Boolean);
  } catch {
    return [];
  }
});

const attrColumnNames = computed(() => {
  const names = (detail.attr || []).map((item) => item.attrName).filter(Boolean);
  if (names.length) return names;
  const first = skuRows.value.find((row) => row.attrMap && Object.keys(row.attrMap).length);
  return first ? Object.keys(first.attrMap) : [];
});

const recommendValues = computed(() => {
  const values = [];
  if (detail.isHot) values.push('isHot');
  if (detail.isBenefit) values.push('isBenefit');
  if (detail.isBest) values.push('isBest');
  if (detail.isNew) values.push('isNew');
  if (detail.isGood) values.push('isGood');
  return values;
});

function normalizeAsset(value) {
  if (!value) return '';
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/')) {
    return value;
  }
  return `/${value}`;
}

function parseAttrValue(value) {
  if (!value) return {};
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

async function loadDetail() {
  loading.value = true;
  try {
    const data = await productDetail(props.productId);
    Object.assign(detail, data);
    skuRows.value = (data.attrValue || []).map((item) => ({
      ...item,
      attrMap: parseAttrValue(item.attrValue)
    }));
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.product-detail {
  padding: 0 28px;
}

.formValidate {
  max-width: 100%;
}

.from-ipt-width {
  width: 100%;
}

.pictrue,
.upLoad,
.tabPic {
  width: 58px;
  height: 58px;
}

.tabPic {
  width: 36px;
  height: 36px;
}

.image {
  width: 100%;
  height: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upLoad {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
}

.content-preview {
  width: 100%;
  min-height: 220px;
  padding: 12px;
  border: 1px solid #ebeef5;
  background: #fff;
  overflow: auto;
}

.content-preview :deep(img) {
  max-width: 100%;
}

.color-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.color-item {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 3px;
  background: #f2f6fc;
  color: #606266;
  border: 1px solid #dcdfe6;
}
</style>
