# CRMEB Modern 交付审计

审计时间：2026-06-20 12:36 CST

本审计以 `modern/DELIVERY_GOAL.md` 为目标口径：新版后台使用 `19527`，新版 H5 使用 `19528`，不再以 `9527/9528` 老入口作为 Modern 验收入口。目标是老 CRMEB 的功能级替代：接口路径、老库结构、核心流程和关键入口不丢；UI 可优化；真实支付、真实退款、短信、微信发布、物流同步和视频号同步默认安全承接，未配置前不触发外部写操作。

## 当前结论

CRMEB Modern 已进入可交付收口状态。当前证据证明核心构建、接口运行、后台全菜单、H5 老路径、浏览器交互、订单/商品/营销/装修/公众号等写回闭环均已通过自动化验收。

仍需按交付边界说明：真实微信/支付宝支付、真实第三方退款、真实短信发送、真实公众号发布、真实物流轨迹同步和视频号同步未在本地环境启用，当前为安全承接或本地保存口径。

## 运行入口

- 新版后台：`http://127.0.0.1:19527`
- 新版 H5：`http://127.0.0.1:19528`
- 管理 API：`http://127.0.0.1:18080`
- 用户 API：`http://127.0.0.1:18081`

当前本机端口检查已确认以上四个服务在运行。

## 验收证据

可用统一脚本复跑交付审计：

```bash
sh modern/scripts/run-delivery-audit.sh full
```

模式说明：

- `fast`：构建门禁、静态检查、只读运行冒烟。
- `writeback`：订单、商品、营销、装修、公众号等写回闭环。
- `browser`：后台全菜单、H5 核心路径和低风险交互浏览器验收。
- `full`：依次执行以上全部内容。

### 构建与静态门禁

- `modern/backend && sh scripts/check-delivery.sh`：通过。
- `modern/admin-web && npm run check:delivery && npm run build`：通过。
- `modern/app-web && npm run check:delivery && npm run build`：通过。

后台构建仍有 Vite/Rollup 对 `@vueuse/core` PURE 注释和大 chunk 的警告，属于构建警告，不是失败。

### 运行冒烟

- `node modern/scripts/runtime-guard-smoke.mjs`：通过。
- `node modern/scripts/runtime-smoke.mjs`：通过。
- `node modern/scripts/runtime-deep-smoke.mjs`：通过。

覆盖内容包括无 token 返回 `401`、短信/物流/支付安全承接、后台商品/订单/用户/营销/优惠券/装修/素材列表、H5 首页/分类/商品/搜索/活动/优惠券，以及订单详情、商品详情、营销活动编辑详情、页面装修详情和 H5 商品评价接口。

### 写回闭环

以下写回脚本已顺序复跑通过，均使用临时数据或备份恢复，不保留验收数据：

- `runtime-order-safe-smoke.mjs`
- `runtime-writeback-smoke.mjs`
- `runtime-coupon-writeback-smoke.mjs`
- `runtime-seckill-manager-writeback-smoke.mjs`
- `runtime-seckill-product-writeback-smoke.mjs`
- `runtime-bargain-product-writeback-smoke.mjs`
- `runtime-combination-product-writeback-smoke.mjs`
- `runtime-product-writeback-smoke.mjs`
- `runtime-shipping-template-writeback-smoke.mjs`
- `runtime-article-writeback-smoke.mjs`
- `runtime-product-reply-writeback-smoke.mjs`
- `runtime-user-options-writeback-smoke.mjs`
- `runtime-store-pickup-writeback-smoke.mjs`
- `runtime-form-temp-writeback-smoke.mjs`
- `runtime-system-group-writeback-smoke.mjs`
- `runtime-page-layout-writeback-smoke.mjs`
- `runtime-wechat-reply-writeback-smoke.mjs`
- `runtime-wechat-menu-writeback-smoke.mjs`

覆盖保存/回显/删除或恢复的模块包括订单备注、改价、发货字段、拒绝退款、核销只读校验、页面装修、页面布局、优惠券、商品、秒杀、砍价、拼团、运费模板、商品评价、文章、用户分组/标签/等级、提货点、核销员、表单模板、组合数据、公众号回复和公众号菜单。

### 浏览器验收

- `node modern/scripts/browser-smoke.mjs`：通过。
- `node modern/scripts/browser-interaction-smoke.mjs`：通过。

浏览器验收覆盖新版后台 `58` 个真实菜单叶子页面、商品新增/编辑/详情、秒杀/拼团/砍价编辑深层路由，以及 H5 首页、分类、商品详情、搜索、商品列表、活动、领券、资讯、客服、购物车、订单、售后、收藏、优惠券、余额、积分、会员、推广和个人中心等老路径承接。

交互验收覆盖订单详情、日志、拒绝退款提示、发货弹窗、物流弹窗、商品复制/库存/分类/规格/评论弹窗、营销活动表单/详情/记录、页面装修/页面设计、财务、分销、公众号、短信、配置页，以及 H5 分类选择、商品评价、个人资料、地址、领券、余额账单、积分、推广、购物车结算入口、订单详情、支付状态和售后列表。

### 文案与假数据门禁

三端 release-copy 门禁已固化，自动拦截以下交付敏感词：

- `本地迁移`
- `迁移环境`
- `迁移模式`
- `迁移阶段`
- `迁移版`
- `迁移中`
- `正在迁移`
- `未迁移`
- `本地安全模式`
- `暂不可用`
- `假数据`
- `mock`

当前已通过：

- `modern/admin-web && npm run check:release-copy && npm run check:delivery`
- `modern/app-web && npm run check:release-copy && npm run check:delivery`
- `modern/backend && sh scripts/check-release-copy.sh && sh scripts/check-delivery.sh`

## 目标逐项映射

| 目标要求 | 当前证据 |
| --- | --- |
| 新版后台 `19527`，新版 H5 `19528` | README、门禁脚本和端口检查均确认 |
| 后台 58 个菜单叶子无白屏/占位/无限加载 | `check:runtime-menu`、`browser-smoke.mjs` |
| H5 老路径承接 | `check:legacy-routes` 显示 `61/61`，浏览器 smoke 已覆盖核心路径 |
| 老库真实数据 | runtime smoke 读取真实商品、订单、用户、营销、优惠券、素材数据 |
| 保存后可回显 | 18 个 runtime writeback 脚本 |
| 订单/售后/发货/退款凭证/物流安全承接 | `runtime-order-safe-smoke.mjs`、`check-order-refund-detail`、浏览器交互验收 |
| 商品新增/编辑/SKU/库存/上下架/运费模板 | `runtime-product-writeback-smoke.mjs`、`runtime-shipping-template-writeback-smoke.mjs`、浏览器交互验收 |
| 营销拼团/秒杀/砍价/优惠券 | 对应 runtime writeback 脚本、浏览器深层路由和交互验收 |
| 页面装修、素材、链接选择、H5 回显 | `runtime-writeback-smoke.mjs`、`runtime-page-layout-writeback-smoke.mjs`、浏览器交互验收 |
| 第三方能力不误触发 | `runtime-guard-smoke.mjs`、`runtime-order-safe-smoke.mjs` 和脚本文档声明 |

## 交付边界

- 本地验收不启用真实微信/支付宝扣款。
- 本地验收不执行真实第三方退款。
- 本地验收不发送真实短信。
- 本地验收不发布真实公众号菜单或模板。
- 本地验收不调用真实物流轨迹同步。
- 视频号、OnePass 等外部能力默认只展示配置和安全承接状态。

以上不是未完成页面，而是交付安全边界；生产启用需要单独配置真实账号、回调域名、证书/密钥，并进行专项验收。
