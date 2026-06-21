# 《CRMEB Modern》数据库设计说明书

## 1. 数据库概况

- 数据库名称：`single_open`
- 表前缀：`eb_`
- 当前快照：`legacy-deps/db/single_open_current.sql`
- 原始 SQL：`legacy-deps/sql/single_open.sql`
- 当前表数量：84
- 字符集建议：`utf8mb4`
- 存储引擎建议：InnoDB

## 2. 设计原则

- 保持当前表结构稳定，降低交付风险。
- 不在交付收口阶段做大规模表结构重设计。
- 后台和 H5 共用同一套业务数据。
- 图片路径兼容 `/crmebimage/**`。
- Docker 首次启动自动导入当前快照。

## 3. 核心表结构分类

### 3.1 商品与分类

- `eb_store_product`：商品主表。
- `eb_store_product_attr`：商品规格。
- `eb_store_product_attr_value`：商品 SKU。
- `eb_store_product_attr_result`：商品规格结果。
- `eb_store_product_description`：商品详情。
- `eb_store_product_cate`：商品分类关联。
- `eb_store_product_rule`：规格模板。
- `eb_store_product_reply`：商品评价。
- `eb_category`：分类表。

### 3.2 购物车、订单与售后

- `eb_store_cart`：购物车。
- `eb_store_order`：订单主表。
- `eb_store_order_info`：订单商品明细。
- `eb_store_order_status`：订单操作记录。

### 3.3 营销

- `eb_store_coupon`：优惠券。
- `eb_store_coupon_user`：用户优惠券。
- `eb_store_seckill`：秒杀商品。
- `eb_store_seckill_manger`：秒杀时段。
- `eb_store_combination`：拼团商品。
- `eb_store_pink`：拼团记录。
- `eb_store_bargain`：砍价商品。
- `eb_store_bargain_user`：用户砍价记录。
- `eb_store_bargain_user_help`：砍价助力记录。

### 3.4 用户与资金

- `eb_user`：用户主表。
- `eb_user_address`：用户地址。
- `eb_user_bill`：用户账单。
- `eb_user_recharge`：充值记录。
- `eb_user_integral_record`：积分记录。
- `eb_user_brokerage_record`：佣金记录。
- `eb_user_extract`：提现记录。
- `eb_user_group`：用户分组。
- `eb_user_tag`：用户标签。
- `eb_user_level`：用户等级记录。
- `eb_system_user_level`：会员等级配置。
- `eb_user_token`：用户 token。
- `eb_user_visit_record`：访问记录。

### 3.5 后台权限与系统配置

- `eb_system_admin`：后台管理员。
- `eb_system_role`：角色。
- `eb_system_menu`：菜单。
- `eb_system_role_menu`：角色菜单关联。
- `eb_system_config`：系统配置。
- `eb_system_attachment`：附件。
- `eb_system_group`：组合数据。
- `eb_system_group_data`：组合数据详情。
- `eb_system_form_temp`：表单模板。
- `eb_system_notification`：通知配置。

### 3.6 装修与内容

- `eb_page_diy`：微页面装修数据。
- `eb_page_category`：页面链接分类。
- `eb_page_link`：页面链接。
- `eb_article`：文章。
- `eb_activity_style`：活动气氛样式。

### 3.7 物流与门店

- `eb_express`：快递公司。
- `eb_shipping_templates`：运费模板。
- `eb_shipping_templates_region`：指定区域运费。
- `eb_shipping_templates_free`：包邮配置。
- `eb_system_city`：城市数据。
- `eb_system_store`：门店自提。
- `eb_system_store_staff`：核销员。

### 3.8 微信、短信、任务

- `eb_sms_template`：短信模板。
- `eb_sms_record`：短信记录。
- `eb_template_message`：微信模板。
- `eb_wechat_reply`：公众号回复。
- `eb_wechat_callback`：微信回调。
- `eb_wechat_pay_info`：微信支付信息。
- `eb_schedule_job`：定时任务。
- `eb_schedule_job_log`：定时任务日志。
- `qrtz_*`：Quartz 调度表。

## 4. 数据初始化

Docker Compose 使用以下文件初始化数据库：

```text
legacy-deps/db/single_open_current.sql
```

当前数据库快照包含演示数据、商品数据、分类数据、图片路径和配置数据。

## 5. 资源路径

上传资源目录：

```text
legacy-deps/upload
```

访问路径继续兼容：

```text
/crmebimage/**
/public/**
```

## 6. 维护要求

- 正式交付前如有新增商品、上传图片或配置变更，需要重新导出 SQL 快照。
- 不提交证书和私钥文件。
- 不手动修改生产数据库结构，必要变更需另写迁移说明。
