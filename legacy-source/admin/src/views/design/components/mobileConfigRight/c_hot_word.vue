<template>
  <!--搜索框中搜索热词组件-->
  <div class="line-box borderPadding pd-bt-20" v-if="configData">
    <div v-if="configData.info" class="title">
      <span>{{ configData.info }}</span>
    </div>
    <div class="input-box">
      <draggable class="dragArea list-group" :list="configData.list" group="peoples" handle=".icon">
        <div class="input-item" v-for="(item, index) in configData.list" :key="index">
          <div class="icon">
            <i class="iconfont icontuozhuaitubiao" style="font-size: 20px; color: #dddddd" />
          </div>
          <el-input size="small" class="ml20" v-model="item.val" maxlength="10" placeholder="选填，不超过十个字" />
          <div class="delete" @click.stop="bindDelete(index)">
            <i class="el-icon-error" style="font-size: 20px" />
          </div>
        </div>
      </draggable>
      <div class="add-btn" @click="addHotTxt" v-if="configData.list.length < 20">
        <el-button icon="el-icon-plus" plain style="width: 100%; height: 40px; font-size: 12px">添加热词</el-button>
      </div>
    </div>
  </div>
</template>
<script>

import vuedraggable from 'vuedraggable';
export default {
  name: 'c_hot_word',
  props: {
    configObj: {
      type: Object,
    },
    configNme: {
      type: String,
    },
  },
  components: {
    draggable: vuedraggable,
  },
  data() {
    return {
      hotWordList: [],
      hotIndex: 1,
      defaults: {},
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
        this.configData = nVal[this.configNme];
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    addHotTxt() {
      let obj = {
        val: '',
      };
      this.configData.list.push(obj);
    },
    // 删除数组
    bindDelete(index) {
      this.configData.list.splice(index, 1);
    },
  },
};
</script>

<style scoped lang="scss">
.line-box {
  border-bottom: 1px dashed #eee;
  .title {
    p {
      font-size: 12px;
      color: #333333;
    }
    span {
      font-size: 12px;
      color: #999999;
    }
  }
  .input-box {
    margin-top: 10px;
    .add-btn {
      margin-top: 18px;
    }
    .input-item {
      display: flex;
      align-items: center;
      margin-bottom: 15px;
      position: relative;
      .delete {
        position: absolute;
        right: -7px;
        top: -8px;
        color: #999;
      }
      .icon {
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: move;
      }

      ::v-deep.ivu-input {
        flex: 1;
        height: 36px;
        font-size: 13px !important;
      }
    }
  }
}
.pd-bt-20 {
  padding-bottom: 20px;
}
</style>
