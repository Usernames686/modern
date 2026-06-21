<template>
  <section class="plain-view extract-cash-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>佣金提现</h1>
      <button type="button" @click="$emit('records')">记录</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <nav class="extract-tabs">
        <button type="button" :class="{ active: type === 'bank' }" @click="$emit('update:type', 'bank')">
          <b>卡</b><span>银行卡</span>
        </button>
        <button type="button" :class="{ active: type === 'weixin' }" @click="$emit('update:type', 'weixin')">
          <b>微</b><span>微信</span>
        </button>
      </nav>

      <form class="extract-form" @submit.prevent="$emit('submit')">
        <template v-if="type === 'bank'">
          <label>
            <span>持卡人</span>
            <input :value="form.name" maxlength="20" placeholder="请输入持卡人姓名" @input="updateField('name', $event.target.value.trim())" />
          </label>
          <label>
            <span>卡号</span>
            <input :value="form.cardum" inputmode="numeric" maxlength="19" placeholder="请填写卡号" @input="updateField('cardum', $event.target.value.trim())" />
          </label>
          <label>
            <span>银行</span>
            <select :value="form.bankName" @change="updateField('bankName', $event.target.value)">
              <option value="">请选择银行</option>
              <option v-for="bank in banks" :key="bank" :value="bank">{{ bank }}</option>
            </select>
          </label>
        </template>
        <template v-else>
          <label>
            <span>姓名</span>
            <input :value="form.name" maxlength="20" placeholder="请输入真实姓名" @input="updateField('name', $event.target.value.trim())" />
          </label>
          <label>
            <span>账号</span>
            <input :value="form.wechat" maxlength="20" placeholder="请填写您的微信账号" @input="updateField('wechat', $event.target.value.trim())" />
          </label>
          <label>
            <span>收款码</span>
            <input :value="form.qrcodeUrl" placeholder="可填写已上传的收款码图片路径" @input="updateField('qrcodeUrl', $event.target.value.trim())" />
          </label>
        </template>

        <label>
          <span>提现</span>
          <input :value="form.money" inputmode="decimal" :placeholder="`最低提现金额${moneyText(info.minPrice)}`" @input="updateField('money', $event.target.value.trim())" />
        </label>
        <p class="extract-tip">
          当前可提现金额: <strong>￥{{ moneyText(info.commissionCount) }}</strong>,
          冻结佣金：￥{{ moneyText(info.brokenCommission) }}
        </p>
        <p class="extract-tip">说明: 每笔佣金的冻结期为{{ info.brokenDay || 0 }}天，到期后可提现</p>
        <button class="extract-submit" type="submit" :disabled="submitting">
          {{ submitting ? "提交中..." : "提现" }}
        </button>
      </form>
    </template>
  </section>
</template>

<script setup>
const emit = defineEmits(["back", "records", "submit", "update:type", "update-field"]);

defineProps({
  loading: Boolean,
  submitting: Boolean,
  type: { type: String, default: "bank" },
  form: { type: Object, default: () => ({}) },
  info: { type: Object, default: () => ({}) },
  banks: { type: Array, default: () => [] },
  moneyText: { type: Function, required: true }
});

function updateField(key, value) {
  emit("update-field", key, value);
}
</script>
