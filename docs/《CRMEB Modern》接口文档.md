# 《CRMEB Modern》接口文档

## 1. 接口总览

CRMEB Modern 使用以下接口路径：

- 后台接口：`/api/admin/**`
- H5 接口：`/api/front/**`
- 公共接口：`/api/public/**`

统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 2. 后台接口

### 2.1 鉴权与基础

- `POST /api/admin/login`：后台登录。
- `GET /api/admin/getAdminInfoByToken`：获取管理员信息。
- `GET /api/admin/getMenus`：获取菜单。
- `GET /api/admin/getLoginPic`：获取登录图。
- `GET /api/admin/logout`：退出登录。
- `GET /api/admin/health`：健康检查。
- `GET /api/public/ping`：公共健康检查。

### 2.2 上传与附件

- `POST /api/admin/upload/image`
- `POST /api/admin/upload/file`
- `GET /api/admin/system/attachment/list`
- `POST /api/admin/system/attachment/save`
- `GET /api/admin/system/attachment/delete/{ids}`
- `POST /api/admin/system/attachment/move`
- `GET /api/admin/system/attachment/info/{id}`

### 2.3 商品与分类

- `GET /api/admin/store/product/list`
- `GET /api/admin/store/product/tabs/headers`
- `GET /api/admin/store/product/info/{id}`
- `POST /api/admin/store/product/save`
- `POST /api/admin/store/product/update`
- `GET /api/admin/store/product/delete/{id}`
- `GET /api/admin/store/product/restore/{id}`
- `GET /api/admin/store/product/putOnShell/{id}`
- `GET /api/admin/store/product/offShell/{id}`
- `POST /api/admin/store/product/quick/stock/add`
- `POST /api/admin/store/product/importProduct`
- `POST /api/admin/store/product/copy/product`
- `GET /api/admin/category/list/tree`
- `GET /api/admin/category/list`
- `POST /api/admin/category/save`
- `POST /api/admin/category/update`
- `GET /api/admin/category/delete`
- `GET /api/admin/store/product/rule/list`
- `POST /api/admin/store/product/rule/save`
- `POST /api/admin/store/product/rule/update`
- `GET /api/admin/store/product/reply/list`
- `POST /api/admin/store/product/reply/save`
- `POST /api/admin/store/product/reply/comment`

### 2.4 订单

- `GET /api/admin/store/order/list`
- `GET /api/admin/store/order/info`
- `GET /api/admin/store/order/status/list`
- `POST /api/admin/store/order/mark`
- `POST /api/admin/store/order/update/price`
- `POST /api/admin/store/order/send`
- `GET /api/admin/store/order/delete`
- `GET /api/admin/store/order/refund`
- `GET /api/admin/store/order/refund/refuse`
- `GET /api/admin/store/order/status/num`
- `GET /api/admin/export/excel/order`
- `GET /api/admin/yly/print/{id}`
- `GET /api/admin/store/order/statistics`

### 2.5 用户

- `GET /api/admin/user/list`
- `GET /api/admin/user/info`
- `POST /api/admin/user/update`
- `POST /api/admin/user/integral/list`
- `GET /api/admin/user/group/list`
- `POST /api/admin/user/group/save`
- `POST /api/admin/user/group/update`
- `GET /api/admin/user/group/delete`
- `GET /api/admin/user/tag/list`
- `POST /api/admin/user/tag/save`
- `POST /api/admin/user/tag/update`
- `GET /api/admin/user/tag/delete`
- `GET /api/admin/system/user/level/list`
- `POST /api/admin/system/user/level/save`
- `POST /api/admin/system/user/level/update/{id}`
- `POST /api/admin/system/user/level/delete/{id}`

### 2.6 营销

- 优惠券：`/api/admin/marketing/coupon/**`
- 秒杀：`/api/admin/store/seckill/**`
- 拼团：`/api/admin/store/combination/**`
- 砍价：`/api/admin/store/bargain/**`
- 视频号：`/api/admin/marketing/videoChannel/**`

### 2.7 装修与内容

- `GET /api/admin/pagediy/list`
- `GET /api/admin/pagediy/info/{id}`
- `POST /api/admin/pagediy/save`
- `POST /api/admin/pagediy/update`
- `GET /api/admin/pagediy/delete`
- `GET /api/admin/page/layout/index`
- `POST /api/admin/page/layout/index/banner/save`
- `POST /api/admin/page/layout/index/table/save`
- `POST /api/admin/page/layout/category/config/save`
- `GET /api/admin/article/list`
- `POST /api/admin/article/save`
- `POST /api/admin/article/update`
- `GET /api/admin/article/delete`

### 2.8 系统、财务与应用

- 系统配置：`/api/admin/system/config/**`
- 管理员：`/api/admin/system/admin/**`
- 角色权限：`/api/admin/system/role/**`、`/api/admin/system/menu/**`
- 门店自提：`/api/admin/system/store/**`
- 运费物流：`/api/admin/express/**`
- 财务：`/api/admin/finance/**`、`/api/admin/user/topUpLog/**`
- 短信：`/api/admin/sms/**`、`/api/admin/pass/**`
- 公众号菜单：`/api/admin/wechat/menu/**`
- 公众号回复：`/api/admin/wechat/keywords/reply/**`
- 微信模板：`/api/admin/wechat/template/**`
- 定时任务：`/api/admin/schedule/job/**`

## 3. H5 接口

### 3.1 登录与用户

- `POST /api/front/login`
- `POST /api/front/login/mobile`
- `GET /api/front/logout`
- `GET /api/front/user`
- `POST /api/front/user/edit`
- `POST /api/front/user/password`
- `POST /api/front/sendCode`
- `GET /api/front/menu/user`

### 3.2 首页、分类、商品

- `GET /api/front/index`
- `GET /api/front/page/diy/default`
- `GET /api/front/category`
- `GET /api/front/products`
- `GET /api/front/product/hot`
- `GET /api/front/product/good`
- `GET /api/front/product/detail/{id}`
- `GET /api/front/reply/list/{id}`
- `GET /api/front/reply/config/{id}`
- `GET /api/front/search/keyword`

### 3.3 购物车、订单、支付

- `GET /api/front/cart/list`
- `POST /api/front/cart/save`
- `POST /api/front/cart/num`
- `POST /api/front/cart/delete`
- `POST /api/front/order/pre/order`
- `GET /api/front/order/load/pre/{preOrderNo}`
- `POST /api/front/order/create`
- `GET /api/front/order/list`
- `GET /api/front/order/detail/{orderId}`
- `POST /api/front/order/cancel`
- `POST /api/front/order/take`
- `GET /api/front/order/refund/reason`
- `GET /api/front/order/express/{orderId}`
- `POST /api/front/recharge/wechat`
- `POST /api/front/recharge/routine`

### 3.4 地址、收藏、优惠券

- `GET /api/front/address/list`
- `POST /api/front/address/edit`
- `POST /api/front/address/del`
- `GET /api/front/address/default`
- `POST /api/front/address/default/set`
- `GET /api/front/collect/user`
- `POST /api/front/collect/add`
- `POST /api/front/collect/delete`
- `GET /api/front/coupons`
- `GET /api/front/coupon/list`
- `POST /api/front/coupon/receive`

### 3.5 营销活动

- 秒杀：`/api/front/seckill/**`
- 拼团：`/api/front/combination/**`
- 砍价：`/api/front/bargain/**`

### 3.6 个人中心

- `GET /api/front/user/balance`
- `GET /api/front/integral/list`
- `GET /api/front/user/sign/config`
- `POST /api/front/user/sign/user`
- `GET /api/front/user/level/grade`
- `GET /api/front/commission`
- `GET /api/front/spread/count/{type}`
- `GET /api/front/spread/commission/detail`

### 3.7 上传和公共

- `POST /api/front/upload/image`
- `POST /api/front/upload/file`
- `POST /api/front/user/upload/image`
- `GET /api/front/health`
- `GET /api/public/ping`

## 4. 安全说明

后台保护接口需要 token。无 token 请求应返回 401。涉及支付、退款、短信、微信发布、物流同步的真实外部调用默认保持安全模式。
