<template>
  <!--标题-->
  <div class="acea-row title-box mb20">
    <div class="title-bar mr20">{{ configData.tabTitle }}</div>
    <div v-if="configData.info" class="title-tips">{{ configData.info }}</div>
  </div>
</template>

<script>

export default {
  name: 'c_title',
  props: {
    configObj: {
      type: Object,
    },
    configNme: {
      type: String,
    },
  },
  data() {
    return {
      defaults: {},
      sliderWidth: 0,
      configData: {},
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.defaults = this.configObj;
      this.configData = this.configObj[this.configNme];
    });
  },
  watch: {
    configObj: {
      handler(nVal, oVal) {
        this.defaults = nVal;
        this.configData = nVal[this.configNme];
      },
      deep: true,
    },
  },
  methods: {
    sliderChange(e) {},
  },
};
</script>
<style scoped lang="scss">
.title-box {
  border-top: 6px solid #f6f7f9;
}
.title-tips {
  height: 38px;
  line-height: 38px;
  color: #999;
  font-size: 12px;
}
.title-bar {
  padding: 20px 0 0 15px;
  color: #333333;
  font-size: 14px;
  font-family: PingFang SC-Regular, PingFang SC;
  font-weight: 400;
  position: relative;
}
</style>
