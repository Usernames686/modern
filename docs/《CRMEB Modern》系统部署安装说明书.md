# 《CRMEB Modern》系统部署安装说明书

## 1. 部署方式

CRMEB Modern 支持两种方式：

- Docker Compose 独立部署。
- 本地开发方式分服务启动。

推荐交付验收使用 Docker Compose。

## 2. Docker 部署

### 2.1 环境要求

- Docker
- Docker Compose
- 可用端口：`3306`、`6379`、`8080`、`8081`、`9527`、`9528`

### 2.2 启动命令

```bash
cd modern
cp .env.example .env
docker compose up -d --build
docker compose ps
```

### 2.3 访问地址

- 后台：`http://127.0.0.1:9527`
- H5：`http://127.0.0.1:9528`
- 后台 API：`http://127.0.0.1:8080/api/admin/health`
- H5 API：`http://127.0.0.1:8081/api/front/health`

### 2.4 默认账号

- 后台账号：`admin`
- 后台密码：`123456`

### 2.5 数据初始化

首次启动时 MySQL 自动导入：

```text
legacy-deps/db/single_open_current.sql
```

上传资源挂载：

```text
legacy-deps/upload -> /data/upload
```

## 3. 本地开发部署

### 3.1 后端构建

```bash
cd modern/backend
./mvnw -pl crmeb-modern-admin-api -am -DskipTests package
./mvnw -pl crmeb-modern-front-api -am -DskipTests package
```

本机直连时建议设置：

```bash
export CRMEB_IMAGE_PATH="./legacy-deps/upload"
```

### 3.2 后台启动

```bash
cd modern/admin-web
npm install
npm run dev
```

默认地址：`http://127.0.0.1:19527`

### 3.3 H5 启动

```bash
cd modern/app-web
npm install
npm run dev
```

默认地址：`http://127.0.0.1:19528`

## 4. 环境变量

- `CRMEB_DB_URL`：数据库 JDBC 地址。
- `CRMEB_DB_USERNAME`：数据库账号。
- `CRMEB_DB_PASSWORD`：数据库密码。
- `CRMEB_REDIS_HOST`：Redis 地址。
- `CRMEB_REDIS_PORT`：Redis 端口。
- `CRMEB_REDIS_DATABASE`：Redis 库编号。
- `CRMEB_IMAGE_PATH`：上传资源目录。
- `CRMEB_ADMIN_SITE_URL`：后台站点地址。
- `CRMEB_APP_SITE_URL`：H5 站点地址。

## 5. 部署后检查

```bash
curl http://127.0.0.1:8080/api/admin/health
curl http://127.0.0.1:8081/api/front/health
curl http://127.0.0.1:9527/api/admin/health
curl http://127.0.0.1:9528/api/front/health
```

## 6. 常见问题

- 如果端口被其他服务占用，修改 `.env` 中端口。
- 如果图片不显示，检查 `legacy-deps/upload` 是否存在并挂载成功。
- 如果数据库没有数据，检查 MySQL volume 是否已经初始化；必要时删除 volume 后重新启动。
- 如果真实支付不可用，先确认当前是否仍处于安全模式。
