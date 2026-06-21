import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const adminApiTarget = process.env.CRMEB_ADMIN_API_TARGET || 'http://127.0.0.1:18080';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 19527,
    proxy: {
      '/api/admin': adminApiTarget,
      '/api/public': adminApiTarget,
      '/crmebimage': adminApiTarget,
      '/public': adminApiTarget
    }
  }
});
