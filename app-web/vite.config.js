import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const frontApiTarget = process.env.CRMEB_FRONT_API_TARGET || 'http://127.0.0.1:18081';

const frontProxy = {
  target: frontApiTarget,
  changeOrigin: true,
  secure: false
};

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 19528,
    proxy: {
      '/api/front': frontProxy,
      '/api/public': frontProxy,
      '/crmebimage': frontProxy,
      '/public': frontProxy
    }
  }
});
