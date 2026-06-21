<template>
  <aside class="payment-mask" @click.self="$emit('close')">
    <section class="payment-panel">
      <div class="payment-title">
        <strong>选择付款方式</strong>
        <button type="button" @click="$emit('close')">×</button>
      </div>
      <button
        v-for="method in methods"
        :key="method.value"
        class="payment-item"
        :class="{ disabled: !methodEnabled(method) }"
        type="button"
        :disabled="loading || !methodEnabled(method)"
        @click="$emit('execute', method.value)"
      >
        <span class="payment-icon" :class="iconClass(method.value)">{{ iconText(method.value) }}</span>
        <span>
          <strong>{{ method.name }}</strong>
          <em>{{ methodSubText(method) }}</em>
        </span>
        <b>{{ methodEnabled(method) ? "›" : "不可用" }}</b>
      </button>
    </section>
  </aside>
</template>

<script setup>
defineProps({
  loading: Boolean,
  methods: { type: Array, default: () => [] },
  methodEnabled: { type: Function, required: true },
  methodSubText: { type: Function, required: true },
  iconClass: { type: Function, required: true },
  iconText: { type: Function, required: true }
});

defineEmits(["close", "execute"]);
</script>
