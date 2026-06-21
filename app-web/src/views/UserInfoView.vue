<template>
  <section class="plain-view personal-data-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>个人资料</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后编辑个人资料</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <form v-else class="personal-data-form" @submit.prevent="$emit('save')">
      <section class="personal-list">
        <label class="personal-item avatar-item">
          <span>头像</span>
          <div class="personal-avatar">
            <img :src="assetUrl(form.avatar || user?.avatar || defaultAvatar)" alt="" />
            <div class="personal-avatar-control">
              <button type="button" :disabled="avatarUploading" @click="avatarInput?.click()">
                {{ avatarUploading ? "上传中..." : "更换头像" }}
              </button>
              <input ref="avatarInput" type="file" accept="image/*" @change="$emit('upload-avatar', $event)" />
            </div>
          </div>
        </label>
        <label class="personal-item avatar-path-item">
          <span>头像路径</span>
          <input :value="form.avatar" type="text" placeholder="可手动填写老图片路径" @input="updateField('avatar', $event.target.value.trim())" />
        </label>
        <label class="personal-item">
          <span>昵称</span>
          <input :value="form.nickname" type="text" maxlength="20" placeholder="请输入昵称" @input="updateField('nickname', $event.target.value.trim())" />
        </label>
        <button class="personal-item" type="button" @click="$emit('phone')">
          <span>手机号码</span>
          <strong>{{ user?.phone || "点击绑定手机号" }} ›</strong>
        </button>
        <div class="personal-item">
          <span>ID号</span>
          <strong>{{ user?.uid || "-" }} 锁定</strong>
        </div>
        <button class="personal-item" type="button" @click="$emit('password')">
          <span>密码</span>
          <strong>点击修改密码 ›</strong>
        </button>
        <button class="personal-item" type="button" @click="$emit('address')">
          <span>地址管理</span>
          <strong>立即设置 ›</strong>
        </button>
      </section>
      <button class="modify-bnt" type="submit" :disabled="saving">
        {{ saving ? "保存中..." : "保存修改" }}
      </button>
      <button class="logout-bnt" type="button" @click="$emit('logout')">退出登录</button>
    </form>
  </section>
</template>

<script setup>
import { ref } from "vue";

const emit = defineEmits(["back", "login", "save", "phone", "password", "address", "logout", "update-field", "upload-avatar"]);
const avatarInput = ref(null);

defineProps({
  authToken: { type: String, default: "" },
  user: { type: Object, default: null },
  form: { type: Object, default: () => ({}) },
  saving: Boolean,
  avatarUploading: Boolean,
  defaultAvatar: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

function updateField(key, value) {
  emit("update-field", key, value);
}
</script>
