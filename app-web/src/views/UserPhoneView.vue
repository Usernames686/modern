<template>
  <section class="plain-view account-form-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>更换手机号</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后更换手机号</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <form v-else class="account-change-form" @submit.prevent="$emit('save')">
      <label v-if="step === 1">
        <input :value="user?.phone || ''" disabled placeholder="当前手机号码" />
      </label>
      <label v-else>
        <input :value="form.phone" type="tel" maxlength="11" placeholder="填写新手机号码" @input="updateField('phone', $event.target.value.trim())" />
      </label>
      <div class="account-code-row">
        <input :value="form.captcha" type="text" maxlength="6" placeholder="填写验证码" @input="updateField('captcha', $event.target.value.trim())" />
        <button type="button" @click="$emit('send-code', step === 1 ? user?.phone : form.phone)">获取验证码</button>
      </div>
      <button class="confirm-bnt" type="submit" :disabled="saving">
        {{ step === 1 ? "下一步" : saving ? "保存中..." : "保存" }}
      </button>
      <p>当前未配置短信服务商，验证码填写任意非空内容即可完成安全校验。</p>
    </form>
  </section>
</template>

<script setup>
const emit = defineEmits(["back", "login", "save", "send-code", "update-field"]);

defineProps({
  authToken: { type: String, default: "" },
  user: { type: Object, default: null },
  form: { type: Object, default: () => ({}) },
  step: { type: Number, default: 1 },
  saving: Boolean
});

function updateField(key, value) {
  emit("update-field", key, value);
}
</script>
