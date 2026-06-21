# CRMEB Modern 一体化电商系统

这个目录是 CRMEB Modern 的独立交付工程。新版后台、H5、后端、Docker、数据库快照、上传素材和项目文档都已经收口在 `modern/` 内，交付、部署和二次开发优先使用本目录。

## 先看这里

- 交付说明：`docs/交付说明.md`
- 开发运维手册：`docs/开发运维手册.md`
- 新版需求规格说明书：`docs/《CRMEB Modern》需求规格说明书.md`
- 新版设计说明书：`docs/《CRMEB Modern》设计说明书.md`
- 新版数据库设计说明书：`docs/《CRMEB Modern》数据库设计说明书.md`
- 新版接口文档：`docs/《CRMEB Modern》接口文档.md`
- 新版部署安装说明书：`docs/《CRMEB Modern》系统部署安装说明书.md`
- 新版系统测试说明书：`docs/《CRMEB Modern》系统测试说明书.md`
- 新版项目实训指导书：`docs/《CRMEB Modern》项目实训指导书.md`
- 新版素材与交付清单：`docs/《CRMEB Modern》素材与交付清单.md`
- 交付前文件审计报告：`docs/交付前文件审计报告.md`
- 运行依赖说明：`legacy-deps/README.md`
- 参考源码副本说明：`legacy-source/README.md`
- 交付审计记录：`DELIVERY_AUDIT.md`
- 开发过程记录：`MIGRATION.md`

默认入口：

- 本地开发后台：`http://127.0.0.1:19527`
- 本地开发 H5：`http://127.0.0.1:19528`
- Docker 后台：`http://127.0.0.1:9527`
- Docker H5：`http://127.0.0.1:9528`

默认后台账号：`admin / 123456`

快速 Docker 启动：

```bash
cd modern
cp .env.example .env
docker compose up -d --build
```

当前交付目标是“新技术栈实现，兼容原系统核心业务”：保留原业务能力、接口路径、数据结构和核心流程；UI 已在 Vue3 / Element Plus / 新 H5 中整理优化，页面必须真实可用、数据真实、入口不空、操作不丢。

## 当前技术栈

- 后端：Java 17 + Spring Boot 3 + MyBatis Plus Boot3 + springdoc-openapi
- 后台：Vue 3 + Vite + Element Plus
- 用户端：Vue 3 + Vite H5，兼容原 H5 主要业务路径
- 部署：Docker Compose + MySQL 8 + Redis 7 + Nginx

## 目录

- `backend`：新 Spring Boot 3 多模块工程
  - `crmeb-modern-common`
  - `crmeb-modern-service`
  - `crmeb-modern-admin-api`
  - `crmeb-modern-front-api`
- `admin-web`：新后台管理端入口
- `app-web`：用户端 H5 新技术栈入口，兼容原 H5 主要路径和业务流程
- `deploy/nginx`：后台和用户端 Nginx 代理配置
- `docker-compose.yml`：现代技术栈编排
- `DELIVERY_AUDIT.md`：交付审计结论、验收脚本证据和第三方安全边界

## 本地构建

交付前可以先跑统一审计脚本：

```bash
cd modern
sh scripts/run-delivery-audit.sh full
```

如果只需要快速确认构建、静态门禁和只读运行冒烟：

```bash
sh scripts/run-delivery-audit.sh fast
```

也可以单独跑写回闭环或浏览器验收：

```bash
sh scripts/run-delivery-audit.sh writeback
sh scripts/run-delivery-audit.sh browser
```

```bash
cd modern/backend
sh scripts/check-delivery.sh

cd ../admin-web
npm install
npm run check:delivery
npm run build

cd ../app-web
npm install
npm run check:delivery
npm run build

cd ..
node scripts/runtime-smoke.mjs
node scripts/runtime-deep-smoke.mjs
node scripts/runtime-guard-smoke.mjs
node scripts/runtime-writeback-smoke.mjs
node scripts/runtime-coupon-writeback-smoke.mjs
node scripts/runtime-seckill-manager-writeback-smoke.mjs
node scripts/runtime-seckill-product-writeback-smoke.mjs
node scripts/runtime-bargain-product-writeback-smoke.mjs
node scripts/runtime-combination-product-writeback-smoke.mjs
node scripts/runtime-product-writeback-smoke.mjs
node scripts/runtime-shipping-template-writeback-smoke.mjs
node scripts/runtime-article-writeback-smoke.mjs
node scripts/runtime-product-reply-writeback-smoke.mjs
node scripts/runtime-user-options-writeback-smoke.mjs
node scripts/runtime-store-pickup-writeback-smoke.mjs
node scripts/runtime-form-temp-writeback-smoke.mjs
node scripts/runtime-system-group-writeback-smoke.mjs
node scripts/runtime-page-layout-writeback-smoke.mjs
node scripts/runtime-wechat-reply-writeback-smoke.mjs
node scripts/runtime-wechat-menu-writeback-smoke.mjs
node scripts/runtime-order-safe-smoke.mjs
node scripts/browser-smoke.mjs
node scripts/browser-interaction-smoke.mjs
```

本机直接启动 API 时，上传目录默认建议指向 `modern/legacy-deps/upload`。这个目录是新版项目随包交付的运行资源目录，商品图、登录图、富文本图片和新上传文件都会继续走 `/crmebimage/**` 兼容路径：

```bash
cd modern
export CRMEB_IMAGE_PATH="./legacy-deps/upload"

java -jar backend/crmeb-modern-admin-api/target/crmeb-modern-admin-api-0.1.0-SNAPSHOT.jar --server.port=18080
java -jar backend/crmeb-modern-front-api/target/crmeb-modern-front-api-0.1.0-SNAPSHOT.jar --server.port=18081
```

`admin-web` 本地 Vite 开发服务默认运行在 `http://127.0.0.1:19527`，并把 `/api/admin/**` 代理到 `http://127.0.0.1:18080`。如需改成 Docker 或远程 API，可设置 `CRMEB_ADMIN_API_TARGET`。

本机调试访问地址：

- 新版后台：`http://127.0.0.1:19527`
- 新版用户端：`http://127.0.0.1:19528`
- 管理 API：`http://127.0.0.1:18080/api/admin/health`
- 用户 API：`http://127.0.0.1:18081/api/front/health`

API 和前端服务都启动后，运行 `node modern/scripts/runtime-smoke.mjs` 做核心列表冒烟，运行 `node modern/scripts/runtime-deep-smoke.mjs` 做订单、商品、营销活动、装修和 H5 商品详情的只读深层冒烟，运行 `node modern/scripts/runtime-guard-smoke.mjs` 验证受保护接口无 token 返回 `401`，以及短信、物流同步、微信/支付宝支付配置继续处于安全模式；运行 `node modern/scripts/runtime-writeback-smoke.mjs` 创建临时 Page DIY，更新、复制、短暂设为默认首页，通过 H5 `/api/front/page/diy/default` 回读后恢复原默认首页并删除临时页，验证微页面装修保存和 H5 回显闭环；运行 `node modern/scripts/runtime-coupon-writeback-smoke.mjs` 创建一个临时后台赠送券，更新后回读并删除，验证营销优惠券保存闭环；运行 `node modern/scripts/runtime-seckill-manager-writeback-smoke.mjs` 创建一个关闭状态的临时秒杀时段，更新后回读并删除，验证秒杀配置保存闭环；运行 `node modern/scripts/runtime-seckill-product-writeback-smoke.mjs` 基于真实普通商品创建一个关闭且未来日期的临时秒杀商品，更新活动 SKU、限量和详情后回读并删除，验证秒杀商品配置保存闭环；运行 `node modern/scripts/runtime-bargain-product-writeback-smoke.mjs` 基于真实普通商品创建一个关闭且未来日期的临时砍价商品，更新活动 SKU、限量、最低价和详情后回读并删除，验证砍价商品配置保存闭环；运行 `node modern/scripts/runtime-combination-product-writeback-smoke.mjs` 基于真实普通商品创建一个关闭且未来日期的临时拼团商品，更新活动 SKU、限量、拼团人数和详情后回读并删除，验证拼团商品配置保存闭环；运行 `node modern/scripts/runtime-product-writeback-smoke.mjs` 创建一个下架临时商品，更新基础信息、详情和关联后回读并硬删除，验证商品编辑闭环；运行 `node modern/scripts/runtime-shipping-template-writeback-smoke.mjs` 创建一个临时运费模板，更新区域运费和指定包邮规则后回读并删除，验证商品运费模板配置闭环；运行 `node modern/scripts/runtime-article-writeback-smoke.mjs` 创建临时文章分类和文章，更新后回读并删除，验证内容管理保存闭环；运行 `node modern/scripts/runtime-product-reply-writeback-smoke.mjs` 创建临时商品评价，商家回复后验证 H5 商品评价列表可读，再软删并确认前台不显示，验证评价闭环；运行 `node modern/scripts/runtime-user-options-writeback-smoke.mjs` 创建临时用户分组、标签和等级，更新后回读并删除，验证用户配置保存闭环；运行 `node modern/scripts/runtime-store-pickup-writeback-smoke.mjs` 创建临时提货点和核销员，更新、启停、回收、恢复、硬删除后回读验证，证明门店自提配置闭环；运行 `node modern/scripts/runtime-form-temp-writeback-smoke.mjs` 创建临时表单模板，更新字段 JSON 后回读并删除，验证表单配置闭环；运行 `node modern/scripts/runtime-system-group-writeback-smoke.mjs` 创建临时表单模板、组合数据组和组合数据，更新后回读并删除，验证组合数据配置闭环；运行 `node modern/scripts/runtime-page-layout-writeback-smoke.mjs` 备份并临时改写首页轮播、首页商品 Tab、首页菜单、首页快讯、个人中心菜单、个人中心轮播、底部导航和分类页配置，回读确认后恢复原配置，验证页面设计保存闭环；运行 `node modern/scripts/runtime-wechat-reply-writeback-smoke.mjs` 创建临时公众号关键字回复，更新为图文、切换状态、回读后删除，验证公众号回复保存闭环；运行 `node modern/scripts/runtime-wechat-menu-writeback-smoke.mjs` 备份公众号自定义菜单配置，临时保存 click/view/miniprogram 菜单后回读，再恢复原配置并确认无 marker 残留，验证公众号菜单本地保存闭环；运行 `node modern/scripts/runtime-order-safe-smoke.mjs` 验证订单详情、售后状态、发货/物流字段、导出/打印本地文件、物流同步安全模式和订单危险写接口鉴权；运行 `node modern/scripts/browser-smoke.mjs` 与 `node modern/scripts/browser-interaction-smoke.mjs` 做真实浏览器页面和低风险交互验收。这些脚本都不会触发发货、真实退款、真实支付、短信、微信发布或物流同步。

## Docker 启动

如果本机已有其他服务占用 `3306/6379/8080/8081/9527/9528`，先释放端口或修改 `.env` 端口配置：

```bash
../.local-runtime/stop-local.sh
```

再启动现代栈：

```bash
cd modern
cp .env.example .env
docker compose up -d --build
```

Compose 会在镜像内完成后端 Maven 打包和前端 Vite 构建，不需要提前提交 `target` 或 `dist` 产物。
`.env` 中的端口、数据库账号、站点地址都可按部署环境调整；不改 `.env` 时会使用本地默认值。
Compose 会把 `modern/legacy-deps/upload` 挂载到 API 容器的 `/data/upload`，并用 `modern/legacy-deps/db/single_open_current.sql` 初始化 MySQL，保证交付演示数据和图片资源完整可用。

访问地址：

- 后台：`http://127.0.0.1:9527`（Docker 独立部署）
- 用户端：`http://127.0.0.1:9528`（Docker 独立部署）
- 管理 API：`http://127.0.0.1:8080/api/admin/health`
- 用户 API：`http://127.0.0.1:8081/api/front/health`

启动后检查六个服务健康状态：

```bash
docker compose ps
curl http://127.0.0.1:8080/api/admin/health
curl http://127.0.0.1:8081/api/front/health
curl http://127.0.0.1:9527/api/admin/health
curl http://127.0.0.1:9528/api/front/health
curl http://127.0.0.1:9528/api/public/ping
```

Compose 的 API 容器健康检查使用轻量业务健康口 `/api/admin/health` 和 `/api/front/health`。这些接口只验证应用自身可响应，避免 actuator 深度探针因为外部依赖探测延迟而影响容器健康状态。

代理约定：

- `admin-web` 代理 `/api/admin/**`、`/api/public/**`、`/crmebimage/**`、`/public/**` 到 `admin-api`。
- `app-web` 代理 `/api/front/**`、`/api/public/**`、`/crmebimage/**`、`/public/**` 到 `front-api`。
- 图片和上传资源继续通过兼容路径访问，不在前端写死旧 IP。
- 上传接口兼容常用路径和字段：`/api/admin/upload/image`、`/api/admin/upload/file`、`/api/front/upload/image`、`/api/front/upload/file`、`/api/front/user/upload/image`，文件字段同时兼容 `multipart` 和 `file`。
- 后台素材管理接口：`/api/admin/system/attachment/list`、`/api/admin/system/attachment/delete/{ids}`、`/api/admin/system/attachment/move`、`/api/admin/system/attachment/info/{id}`。

## 交付规则

- 数据库结构保持兼容，Docker 默认使用 `modern/legacy-deps/db/single_open_current.sql` 初始化；原始 SQL 副本保存在 `modern/legacy-deps/sql/single_open.sql`。
- 接口路径保持兼容：`/api/admin/**`、`/api/front/**`、`/api/public/**`。
- 页面以真实业务可用为准，UI 可在新技术栈下优化。
- `modern/legacy-source` 是参考源码副本，用于后续对照和二次开发；新版运行入口是 `admin-web`、`app-web`、`backend`。
- 新版运行所需上传资源和 SQL 快照在 `modern/legacy-deps`。
