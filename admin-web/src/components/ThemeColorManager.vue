<template>
  <section class="theme-page">
    <el-card shadow="never" class="theme-card">
      <div class="theme-head">
        <button
          v-for="(item, index) in themes"
          :key="item.value"
          type="button"
          class="tab-color"
          :class="{ active: active === index }"
          @click="selected(index)"
        >
          <span class="color-dot" :style="{ backgroundColor: item.color }">
            <el-icon v-if="active === index"><Check /></el-icon>
          </span>
          <span>{{ item.title }}</span>
        </button>
      </div>

      <div class="theme-content">
        <img :src="themes[active]?.image" :alt="themes[active]?.title" />
      </div>

      <div class="save">
        <el-button type="primary" :loading="saving" @click="saveTheme">保存</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { Check } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getThemeColor, saveThemeColor } from '../api';
import theme1 from '../assets/theme/theme1.jpg';
import theme2 from '../assets/theme/theme2.jpg';
import theme3 from '../assets/theme/theme3.jpg';
import theme4 from '../assets/theme/theme4.png';
import theme5 from '../assets/theme/theme5.jpg';

const themes = [
  { title: '热情红', value: 1, color: '#e93323', image: theme1 },
  { title: '家居橙', value: 2, color: '#fe5c2d', image: theme2 },
  { title: '生鲜绿', value: 3, color: '#42ca4d', image: theme3 },
  { title: '海鲜蓝', value: 4, color: '#1ca5e9', image: theme4 },
  { title: '女神粉', value: 5, color: '#ff448f', image: theme5 }
];
const active = ref(0);
const saving = ref(false);

onMounted(loadTheme);

async function loadTheme() {
  const data = await getThemeColor();
  const value = Number(data?.value || 1);
  active.value = Math.min(Math.max(value, 1), themes.length) - 1;
  document.documentElement.style.setProperty('--prev-color-primary', themes[active.value].color);
}

function selected(index) {
  active.value = index;
}

async function saveTheme() {
  saving.value = true;
  try {
    await saveThemeColor({ value: themes[active.value].value });
    document.documentElement.style.setProperty('--prev-color-primary', themes[active.value].color);
    ElMessage.success('编辑成功');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.theme-card {
  border-radius: 4px;
}

.theme-head {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin: 20px 0;
}

.tab-color {
  display: inline-flex;
  width: 114px;
  height: 45px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
  background: #fff;
  color: #303133;
  cursor: pointer;
  font: inherit;
}

.tab-color.active {
  border-color: var(--prev-color-primary, #e93323);
}

.color-dot {
  display: inline-flex;
  width: 25px;
  height: 25px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: #fff;
  font-size: 14px;
}

.theme-content {
  display: flex;
  width: 100%;
  margin-bottom: 40px;
  overflow-x: auto;
}

.theme-content img {
  display: block;
  width: min(800px, 100%);
  min-width: 360px;
  border: 1px solid #ebeef5;
}

.save {
  display: flex;
  justify-content: flex-start;
}
</style>
