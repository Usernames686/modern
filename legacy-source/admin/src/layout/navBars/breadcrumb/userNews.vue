<template>
  <div class="layout-navbars-breadcrumb-user-news">
    <div class="head-box">
      <div class="head-box-title">系统通知</div>
    </div>
    <div class="content-box">
      <template v-if="newsList.length > 0">
        <div class="content-box-item" v-for="(v, k) in newsList" :key="k" @click="jumpUrl(v.url)">
          <div class="content-box-right">
            <div class="content-box-type">{{ v.title }}</div>
            <div class="content-box-msg">
              {{ v.message }}
            </div>
          </div>
        </div>
        <div v-if="list.length > 3 && list.length != newsList.length" class="moreBtn" @click="newsList = list">
          展开全部<span class="el-icon-arrow-down"></span>
        </div>
      </template>
      <div class="content-box-empty" v-else>
        <div class="content-box-empty-margin">
          <img class="no-msg" src="@/assets/images/no-message.png" alt="" />
          <div class="mt15">暂无系统通知</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { needDealtList } from '@/api/system';
export default {
  name: 'layoutBreadcrumbUserNews',
  data() {
    return {
      list: [],
      newsList: [],
      newOrderAudioLink: null,
      messageList: [],
    };
  },
  props: ['vm'],
  mounted() {
    this.getNotict();
  },
  filters: {
    // 1 待发货 2 库存报警  3评论回复  4提现申请
    msgType(type) {
      let typeName;
      switch (type) {
        case 1:
          typeName = '待发货订单提醒';
          break;
        case 2:
          typeName = '库存报警';
          break;
        case 3:
          typeName = '评论回复';
          break;
        case 4:
          typeName = '提现申请';
          break;
        default:
          typeName = '其它';
      }
      return typeName;
    },
  },
  methods: {
    // 全部已读点击
    onAllReadClick() {
      this.list = [];
      this.$emit('haveNews', !!this.list.length);
    },
    // 前往通知中心点击
    onGoToGiteeClick() {},
    getNotict() {
      needDealtList()
        .then((res) => {
          this.list = res.data || [];
          this.newsList = res.data.length > 3 ? res.data.slice(0, 3) : res.data;
          this.$emit('haveNews', !!this.list.length);
        })
        .catch(() => {});
    },
    jumpUrl(path) {
      this.vm.$router.push({
        path,
      });
    },
    icon(type) {
      return require(`@/assets/images/news-${type}.png`);
    },
  },
};
</script>

<style scoped lang="scss">
.layout-navbars-breadcrumb-user-news {
  width: 240px;
  padding: 8px 14px 14px;
  background: #ffffff;
  .head-box {
    display: flex;
    box-sizing: border-box;
    color: var(--prev-color-text-primary);
    justify-content: space-between;
    align-items: center;
    .head-box-title {
      font-size: 13px;
      font-weight: bold;
      color: #333333;
      line-height: 13px;
    }
    .head-box-btn {
      color: var(--prev-color-primary);
      font-size: 13px;
      cursor: pointer;
      opacity: 0.8;
      font-weight: 400;
      line-height: 13px;
      &:hover {
        opacity: 1;
      }
    }
  }
  .content-box {
    font-size: 13px;
    .content-box-item {
      padding-top: 24px;
      cursor: pointer;
      display: flex;
      align-items: center;
      &:last-of-type {
        padding-bottom: 12px;
      }
      .icon {
        width: 26px;
        height: 26px;
        margin-right: 10px;
      }
      .content-box-right {
      }
      .content-box-type {
        font-size: 13px;
        font-weight: bold;
        color: #333333;
      }
      .content-box-msg {
        margin-top: 6px;
        font-size: 13px;
        font-weight: 400;
        color: #666666;
        line-height: 13px;
      }
      .content-box-time {
        color: var(--prev-color-text-secondary);
      }
    }
    .content-box-empty {
      width: 292px;
      // height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;
      .content-box-empty-margin {
        text-align: center;
        font-size: 13px;
        color: #999999;
        i {
          color: var(--prev-color-primary);
          font-size: 60px;
        }
        .no-msg {
          width: 180px;
          height: 138px;
        }
      }
    }
  }
  .foot-box {
    height: 35px;
    color: var(--prev-color-primary);
    font-size: 13px;
    cursor: pointer;
    opacity: 0.8;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top: 1px solid var(--prev-border-color-lighter);
    &:hover {
      opacity: 1;
    }
  }
  ::v-deep(.el-empty__description p) {
    font-size: 13px;
  }
}
.moreBtn {
  color: #666666;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  cursor: pointer;
}
</style>
