<template>
  <section class="plain-view account-form-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>修改密码</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后修改密码</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <form v-else class="account-change-form" @submit.prevent="$emit('save')">
      <div class="account-phone">当前手机号：{{ maskPhone(user?.phone) }}</div>
      <label>
        <input :value="form.password" type="password" maxlength="18" placeholder="6-18位字母加数字" @input="updateField('password', $event.target.value)" />
      </label>
      <label>
        <input :value="form.confirmPassword" type="password" maxlength="18" placeholder="确认新密码" @input="updateField('confirmPassword', $event.target.value)" />
      </label>
      <div class="account-code-row">
        <input :value="form.captcha" type="text" maxlength="6" placeholder="填写验证码" @input="updateField('captcha', $event.target.value.trim())" />
        <button type="button" @click="$emit('send-code', user?.phone)">获取验证码</button>
      </div>
      <button class="confirm-bnt" type="submit" :disabled="saving">
        {{ saving ? "修改中..." : "确认修改" }}
      </button>
      <p>沿用老项目密码规则：必须以字母开头，长度6-18位，只能包含字母、数字和下划线。</p>
    </form>
  </section>
</template>

<script setup>
const emit = defineEmits(["back", "login", "save", "send-code", "update-field"]);

defineProps({
  authToken: { type: String, default: "" },
  user: { type: Object, default: null },
  form: { type: Object, default: () => ({}) },
  saving: Boolean,
  maskPhone: { type: Function, required: true }
});

function updateField(key, value) {
  emit("update-field", key, value);
}
</script>
