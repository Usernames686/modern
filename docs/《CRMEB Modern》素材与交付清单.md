# 《CRMEB Modern》素材与交付清单

## 1. 交付目录

新版项目交付目录：

```text
modern/
```

## 2. 源码清单

- `admin-web/`：后台前端源码。
- `app-web/`：H5 前端源码。
- `backend/`：后端源码。
- `deploy/`：Nginx 配置。
- `docker-compose.yml`：Docker Compose 编排。
- `.env.example`：环境变量示例。

## 3. 数据清单

- `legacy-deps/db/single_open_current.sql`：当前数据库快照。
- `legacy-deps/sql/single_open.sql`：原始 SQL。
- 数据库名：`single_open`。
- 表数量：84。
- 表前缀：`eb_`。

## 4. 素材清单

新版运行素材位于：

```text
legacy-deps/upload
```

当前约 161M，包含约 2041 个文件，包括：

- 商品图片。
- 用户头像。
- 富文本图片。
- 首页和活动素材。
- 公共上传资源。

说明：外层原包的 `电商系统素材/` 是原始素材资料；新版项目运行所需素材已经收口到 `legacy-deps/upload`，不再重复放第二份。

## 5. 老源码参考副本

- `legacy-source/admin`
- `legacy-source/app`
- `legacy-source/crmeb`

说明：该目录用于对照老功能，不作为新版运行入口。

## 6. 文档清单

- `README.md`
- `docs/交付说明.md`
- `docs/开发运维手册.md`
- `docs/《CRMEB Modern》需求规格说明书.md`
- `docs/《CRMEB Modern》设计说明书.md`
- `docs/《CRMEB Modern》数据库设计说明书.md`
- `docs/《CRMEB Modern》接口文档.md`
- `docs/《CRMEB Modern》系统部署安装说明书.md`
- `docs/《CRMEB Modern》系统测试说明书.md`
- `docs/《CRMEB Modern》项目实训指导书.md`
- `docs/《CRMEB Modern》素材与交付清单.md`
- `DELIVERY_AUDIT.md`
- `MIGRATION.md`

## 7. 不应交付到仓库的内容

- `node_modules/`
- `target/`
- `dist/`
- `.env`
- `.local-logs/`
- `*.pem`
- `*.key`
- `*.p12`
- `*.pfx`
- 真实生产证书和私钥。

## 8. 交付检查

- `modern/` 可单独打开并查看文档。
- Docker Compose 可启动。
- 数据库快照存在。
- 上传素材存在。
- 后台和 H5 可访问。
- 没有依赖外层老目录才能运行的路径。
