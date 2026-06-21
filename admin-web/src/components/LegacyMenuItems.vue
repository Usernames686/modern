<template>
  <template v-for="item in items" :key="item.id">
    <el-sub-menu v-if="hasChildren(item)" :index="String(item.id)">
      <template #title>
        <i class="legacy-menu-icon" :class="legacyIconClass(item.icon)" />
        <span>{{ item.name }}</span>
      </template>
      <LegacyMenuItems :items="item.childList" @select="$emit('select', $event)" />
    </el-sub-menu>

    <el-menu-item v-else :index="String(item.id)" @click="$emit('select', item)">
      <i class="legacy-menu-icon" :class="legacyIconClass(item.icon)" />
      <span>{{ item.name }}</span>
    </el-menu-item>
  </template>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
});

defineEmits(['select']);

function hasChildren(item) {
  return Array.isArray(item.childList) && item.childList.length > 0;
}

function legacyIconClass(icon) {
  return icon ? `el-icon-${icon}` : '';
}
</script>
