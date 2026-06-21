# CRMEB Legacy Source Snapshot

这个目录保存老项目源码副本，方便在 `modern` 交付目录内继续按模块对照、复制和适配老功能。

## 内容

- `admin/`：老后台前端源码副本。
- `app/`：老 H5/uni-app 源码副本。
- `crmeb/`：老 Java 后端源码副本。

## 排除内容

为避免交付目录重复膨胀，复制时排除了构建产物和已单独收口的运行资源：

- `node_modules/`
- `dist/`
- `unpackage/`
- `target/`
- `crmeb/upload/`
- `crmeb/sql/`

上传资源、原始 SQL 和当前数据库快照已放在 `modern/legacy-deps/`。

## 使用口径

- 这里是对照和移植来源，不是继续运行的老工程入口。
- 新开发仍以 `modern/admin-web`、`modern/app-web`、`modern/backend` 为主。
- 需要补老功能时，按模块从这里复制页面结构、字段、业务规则，再适配 Vue3、Element Plus、Spring Boot 3 和当前老库。
