<template>
  <!--input数字输入框-->
  <div class="numbox borderPadding" v-if="configData && configData.isShow === 1">
    <div class="c_row-item">
      <div class="label labelwidth">
        <span>{{ configData.title || '商品数量' }}</span>
      </div>
      <div class="slider-box ml22">
        <el-input-number
          controls-position="right"
          size="small"
          v-model="configData.val"
          placeholder="请输入数量"
          :step="1"
          :max="configData.max ? configData.max : 100"
          :min="configData.min ? configData.min : 1"
          @change="bindChange"
          style="text-align: right"
          :step-strictly="true"
        ></el-input-number>
      </div>
    </div>
  </div>
</template>

<script>
import { config } from '@vue/test-utils';


export default {
  name: 'c_input_number',
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
  created() {
    this.defaults = this.configObj;
    this.configData = this.configObj[this.configNme];
  },
  watch: {
    configObj: {
      handler(nVal, oVal) {
        this.defaults = nVal;
        this.configData = nVal[this.configNme];
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    bindChange() {
      this.$emit('getConfig', { name: 'number', numVal: this.configData.val });
    },
  },
};
</script>

<style scoped lang="scss">
.slider-box {
  width: 81%;
}
::v-deep.el-input-number {
  width: 100%;
}
::v-deep.ivu-input {
  font-size: 12px !important;
}
.numbox {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  font-size: 12px;
  span {
    width: 80px;
    color: #999;
    font-size: 12px;
  }
}
.c_row-item {
  width: 100%;
}
</style>
