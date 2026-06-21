# CRMEB Modern Legacy Dependencies

这个目录保存新版 `modern` 工程运行所需的老项目依赖副本，目的是让交付目录更完整，减少对根目录 `crmeb/`、`admin/`、`app/` 的运行依赖。

## 内容

- `upload/`：老项目上传资源副本，包含 `/crmebimage/**` 和 `/public/**` 访问所需图片、富文本资源、素材文件。
- `sql/`：老项目原始 SQL 副本，来自 `crmeb/sql/`。
- `db/single_open_current.sql`：当前本地 `single_open` 数据库快照，包含本轮迁移和 H5 商品补货后的数据。

## 使用口径

- Docker Compose 默认使用 `db/single_open_current.sql` 初始化 MySQL。
- Docker Compose 默认挂载 `upload/` 到 API 容器的 `/data/upload`。
- 本机直接启动 API 时，建议设置：

```bash
export CRMEB_IMAGE_PATH="modern/legacy-deps/upload"
```

## 注意

- 这个目录是运行依赖副本，不是老项目源码继续开发入口。
- 老项目源码仍保留在根目录 `admin/`、`app/`、`crmeb/`，用于对照和兜底。
- 如果后台继续上传图片或导入商品，生产交付前需要重新同步 `upload/` 和数据库快照。
