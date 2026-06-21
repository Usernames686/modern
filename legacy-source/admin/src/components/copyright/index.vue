<template>
  <div class="ivu-global-footer i-copyright">
    <div class="footers acea-row">
      <div class="title" v-text="copyrightNew"></div>
    </div>
  </div>
</template>
<script>
import { getCopyrightInfo } from '@/libs/copyPublic';

export default {
  name: 'i-copyright',
  data() {
    return {
      copyright: 'Copyright © 2025 鼎利元研究院',
      copyrightNew: '',
    };
  },
  mounted() {
    this.getVersion();
  },
  methods: {
    getVersion() {
      getCopyrightInfo().then((res) => {
        const data = res || {};
        this.copyrightNew = data.companyName ? data.companyName : this.copyright;
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.ivu-global-footer {
  text-align: center;
  box-sizing: border-box;
}
.i-copyright {
  flex: 0 0 auto;
}
.ivu-global-footer-links {
  margin-bottom: 8px;
}
.ivu-global-footer-links a:not(:last-child) {
  margin-right: 40px;
}
.ivu-global-footer-links a {
  font-size: 14px;
  color: #808695;
  transition: all 0.2s ease-in-out;
}
.ivu-global-footer-copyright {
  color: #808695;
  font-size: 14px;
}
</style>
