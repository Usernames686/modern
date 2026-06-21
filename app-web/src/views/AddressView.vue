<template>
  <section class="plain-view address-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>收货地址</h1>
      <button type="button" @click="$emit('create')">新增</button>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="addresses.length === 0" class="state">暂无收货地址</div>
    <div v-else class="address-list">
      <article v-for="item in addresses" :key="item.id" class="address-card" @click="$emit('choose', item)">
        <div>
          <strong>{{ item.realName }}</strong>
          <span>{{ item.phone }}</span>
          <em v-if="item.isDefault">默认</em>
        </div>
        <p>{{ item.fullAddress }}</p>
        <footer>
          <button type="button" @click.stop="$emit('edit', item)">编辑</button>
          <button type="button" @click.stop="$emit('set-default', item.id)" :disabled="item.isDefault">设为默认</button>
          <button type="button" @click.stop="$emit('delete', item.id)">删除</button>
        </footer>
      </article>
    </div>

    <form v-if="editing" class="address-form" @submit.prevent="$emit('save')">
      <div class="view-head compact">
        <h1>{{ form.id ? "编辑地址" : "新增地址" }}</h1>
        <button type="button" @click="$emit('close-edit')">收起</button>
      </div>
      <input :value="form.realName" type="text" placeholder="收货人" @input="updateField('realName', $event.target.value.trim())" />
      <input :value="form.phone" type="tel" placeholder="手机号" @input="updateField('phone', $event.target.value.trim())" />
      <div class="address-grid">
        <input :value="form.province" type="text" placeholder="省" @input="updateField('province', $event.target.value.trim())" />
        <input :value="form.city" type="text" placeholder="市" @input="updateField('city', $event.target.value.trim())" />
        <input :value="form.district" type="text" placeholder="区/县" @input="updateField('district', $event.target.value.trim())" />
      </div>
      <input :value="form.detail" type="text" placeholder="详细地址" @input="updateField('detail', $event.target.value.trim())" />
      <label class="check-line">
        <input :checked="form.isDefault" type="checkbox" @change="updateField('isDefault', $event.target.checked)" />
        设为默认地址
      </label>
      <button class="create-order-button" type="submit" :disabled="saving">
        {{ saving ? "保存中..." : "保存地址" }}
      </button>
    </form>
  </section>
</template>

<script setup>
const emit = defineEmits([
  "back",
  "create",
  "choose",
  "edit",
  "set-default",
  "delete",
  "close-edit",
  "save",
  "update-field"
]);

defineProps({
  loading: Boolean,
  addresses: { type: Array, default: () => [] },
  editing: Boolean,
  form: { type: Object, default: () => ({}) },
  saving: Boolean
});

function updateField(key, value) {
  emit("update-field", key, value);
}
</script>
