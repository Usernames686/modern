<template>
  <!--单选框组-->
  <div class="acea-row borderPadding mb20">
    <div class="title-tips labelwidth" v-if="configData.title">
      <span>{{ configData.title }}</span>
    </div>
    <div class="radio-boxs labelml">
      <el-radio-group v-model="configData.tabVal" size="large" @change="radioChange($event)">
        <el-radio :label="index" v-for="(item, index) in configData.list" :key="index">
          <span>{{ item.name }}</span>
        </el-radio>
      </el-radio-group>
    </div>
  </div>
</template>

<script>

export default {
  name: 'c_tab_radio',
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
      formData: {
        type: 0,
      },
      defaults: {},
      configData: {},
    };
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
  mounted() {
    this.$nextTick(() => {
      this.defaults = this.configObj;
      this.configData = this.configObj[this.configNme];
    });
  },
  methods: {
    radioChange(e) {
      if (this.defaults.picStyle) {
        this.defaults.picStyle.tabVal = '0';
      }

      this.$emit('getConfig', { name: 'tab_radio', values: e });
      // this.$emit('getConfig', e);
    },
  },
};
</script>

<style scoped lang="scss">
.title-tips {
  font-size: 12px;
  color: #333;
  span {
    color: #999;
  }
}
.iconfont-diy,
.iconfont {
  font-size: 20px;
  line-height: 18px;
}
</style>
