# 《CRMEB Modern》项目实训指导书

## 1. 实训目标

通过 CRMEB Modern 学习前后端分离、电商业务实现、数据库兼容、Docker 部署和交付验收。

## 2. 环境准备

- JDK 17
- Maven
- Node.js
- Docker / Docker Compose
- MySQL 8
- Redis 7

## 3. 项目启动

### 3.1 Docker 启动

```bash
cd modern
cp .env.example .env
docker compose up -d --build
```

### 3.2 本地开发

分别启动：

- `backend/crmeb-modern-admin-api`
- `backend/crmeb-modern-front-api`
- `admin-web`
- `app-web`

## 4. 学习顺序

1. 阅读 `README.md`。
2. 阅读 `docs/交付说明.md`。
3. 查看 `legacy-deps` 中数据库和素材。
4. 熟悉 `backend` 多模块结构。
5. 熟悉 `admin-web` 后台组件。
6. 熟悉 `app-web` H5 页面。
7. 跑通商品、购物车、下单、订单、售后。
8. 跑通后台新增商品后 H5 可见。

## 5. 后端实训

- 新增一个只读接口。
- 在 service 层查询真实业务表。
- 返回统一 `{ code, message, data }`。
- 增加鉴权和异常兜底。

## 6. 后台实训

- 新增一个表格页面。
- 接入后台接口。
- 支持分页、搜索、状态筛选。
- 支持保存或详情查看。

## 7. H5 实训

- 新增一个用户中心入口。
- 页面读取 `/api/front/**` 真实数据。
- 适配移动端布局。
- 不使用假数据代替业务数据。

## 8. 数据闭环实训

完成以下流程：

1. 后台新增商品。
2. H5 分类或搜索看到商品。
3. H5 加入购物车。
4. H5 创建订单。
5. 后台查看订单。
6. H5 查看订单详情。

## 9. 交付验收实训

执行：

```bash
sh scripts/run-delivery-audit.sh full
```

人工检查后台和 H5 的核心页面，确认图片、价格、分类、订单、售后、装修都真实可用。
