# CRMEB H5 技术栈升级执行清单

## 目标

用户端不是重新设计，也不是长期维护旧打包产物，而是以老 `app/` 为功能和 UI 母版，迁到 `modern/app-web` 的 Vue 3 + Vite H5 入口。

## 当前口径

- 交付入口优先使用 `modern/app-web`。
- 老 `app/pages/**` 作为页面结构、文案、流程和样式基准。
- 老路径继续兼容，例如 `/pages/goods/goods_details/index`、`/pages/users/order_list/index`、`/pages/order/order_details/index`。
- API 统一走 `/api/front/**`、`/api/public/**`，由 Vite 或 Nginx 代理到 modern front-api。
- 旧 `app/unpackage/dist/build/web` 只作为短期兜底，不作为继续开发主线。
- 老 H5 路径映射已抽到 `modern/app-web/src/legacy/routes.js`，后续补页面先在这里登记路径，再接真实页面和接口。
- `modern/app-web` 已增加 `npm run check:legacy-routes`，会读取老 `app/pages.json` 并检查全部 H5 页面路径是否有显式承接规则。
- 前台 API 请求、上传、`VITE_FRONT_API_BASE` 代理路径和老资源路径兼容已抽到 `modern/app-web/src/api/frontClient.js`，继续保留 `Authori-zation` token、`{ code, message, data }` 校验、上传 multipart 和 `crmebimage/public` 资源域名推导。
- H5 公共纯工具已抽到 `modern/app-web/src/utils/format.js`、`utils/order.js`、`utils/activity.js`、`utils/forms.js`、`utils/display.js`，承接金额、时间戳、订单商品字段兼容、支付/退款状态、订单按钮状态、秒杀/拼团/砍价活动状态、空表单、通用字段更新、手机号脱敏、优惠券/提现/菜单/支付图标文案；保持老字段兜底和原 UI 行为不变。
- 首页/分类/搜索共用的商品列表已抽到 `modern/app-web/src/components/ProductList.vue`，保留原卡片结构、价格、销量、活动/售罄标签和点击事件。
- 首页/分类骨架已继续抽出 `SearchHeader.vue`、`HomeQuickMenu.vue`、`CategoryNav.vue`，保留老搜索栏、首页轮播、快捷菜单、分类宫格和分类横条结构。
- 首页/分类组合页已抽到 `modern/app-web/src/views/HomeCategoryView.vue`，统一承接搜索栏、首页轮播、快捷菜单、分类导航、首页商品 tab 和商品列表，`App.vue` 只保留数据和事件分发。
- 商品评价列表旧路径 `/pages/goods/goods_comment_list/index` 已抽到 `modern/app-web/src/views/ProductRepliesView.vue`，保留好评率、综合评分、全部/好评/中评/差评 tab 和评价图片列表。
- 资讯列表/详情旧路径 `/pages/news/news_list/index`、`/pages/news/news_details/index` 已抽到 `modern/app-web/src/views/ArticlesView.vue`，保留轮播、热门/分类 tab、资讯卡片、富文本详情和关联商品入口。
- 客服旧路径 `/pages/users/kefu/index`、`/pages/users/web_page/index` 已抽到 `modern/app-web/src/views/CustomerServiceView.vue`，保留在线客服 iframe、打开客服、客服电话和本地迁移安全提示。
- 商品详情旧路径 `/pages/goods/goods_details/index`、`/pages/goods/goods_details_store/index` 的详情弹层已抽到 `modern/app-web/src/views/ProductDetailPanel.vue`，保留主图、价格、库存销量、规格、评价入口、富文本详情、收藏、加购和立即购买按钮。
- 购物车旧路径 `/pages/order_addcart/order_addcart` 已抽到 `modern/app-web/src/views/CartView.vue`，保留登录提示、三项保障、管理模式、单选/全选、数量步进、收藏、删除和立即下单入口。
- 确认订单旧路径 `/pages/order/order_confirm/index` 的页面结构已抽到 `modern/app-web/src/views/CheckoutView.vue`，保留配送方式、收货地址、自提门店、商品明细、优惠券、积分抵扣、备注、金额明细和提交订单栏；预下单、价格计算、地址/门店/优惠券选择和提交仍由父级真实接口方法承接。
- 订单详情旧路径 `/pages/users/order_list/index`、`/pages/order/order_details/index` 的页面结构已抽到 `modern/app-web/src/views/OrderDetailView.vue`，保留订单状态、退款信息、收货/自提信息、商品明细、金额明细和底部操作按钮；支付、退款、物流、评价、删除和再次购买仍由父级真实接口方法承接。
- 订单付款和支付状态旧路径 `/pages/order/order_payment/index`、`/pages/order/order_pay_status/index` 的页面结构已抽到 `modern/app-web/src/views/PaymentView.vue`、`modern/app-web/src/views/PayStatusView.vue`，保留订单金额、支付方式、支付结果、查看订单、邀请参团和返回首页按钮；支付配置读取、余额支付和第三方 dry-run 保护仍由父级真实接口方法承接。
- 售后退款旧路径 `/pages/goods/goods_return/index`、`/pages/users/user_return_list/index` 的页面结构已抽到 `modern/app-web/src/views/RefundApplyView.vue`、`modern/app-web/src/views/RefundListView.vue`，保留申请退货、退款原因、备注说明、凭证上传、退款列表、退款状态和订单商品卡；退款申请、凭证上传和退款详情仍由父级真实接口方法承接。
- 物流和评价旧路径 `/pages/goods/goods_logistics/index`、`/pages/goods/goods_comment_con/index` 的页面结构已抽到 `modern/app-web/src/views/LogisticsView.vue`、`modern/app-web/src/views/CommentView.vue`，保留物流商品卡、物流公司、快递单号、轨迹列表、评价商品卡、双评分、评价内容、图片上传和提交按钮；物流查询、复制单号、图片上传和评价提交仍由父级真实接口方法承接。
- 收货地址旧路径 `/pages/users/user_address_list/index`、`/pages/users/user_address/index` 的页面结构已抽到 `modern/app-web/src/views/AddressView.vue`，保留地址列表、新增、编辑、设为默认、删除、选择地址和地址表单；地址读取、保存、默认地址和删除仍由父级真实接口方法承接。
- 账户资料旧路径 `/pages/infos/user_info/index`、`/pages/infos/user_phone/index`、`/pages/infos/user_pwd_edit/index` 的页面结构已抽到 `modern/app-web/src/views/UserInfoView.vue`、`UserPhoneView.vue`、`UserPasswordView.vue`，保留个人资料、头像/昵称、手机号换绑、验证码、密码修改、地址管理和退出登录入口；资料保存、换绑验证、密码修改和本地短信安全提示仍由父级真实接口方法承接。
- 余额账单旧路径 `/pages/users/user_money/index`、`/pages/users/user_bill/index` 的页面结构已抽到 `modern/app-web/src/views/BalanceView.vue`、`BillView.vue`，保留余额资产卡、充值入口、账单/消费/充值记录入口、签到/优惠券入口、账单分类 tab 和分组列表；余额读取、充值面板、账单筛选和账单加载仍由父级真实接口方法承接。
- 积分签到旧路径 `/pages/users/user_integral/index`、`/pages/users/user_sgin/index`、`/pages/users/user_sgin_list/index` 的页面结构已抽到 `modern/app-web/src/views/IntegralView.vue`、`SignView.vue`、`SignRecordsView.vue`，保留积分中心、分值明细/分值提升、签到奖励进度、累计签到、最近签到和签到明细分组列表；积分读取、签到写入和签到明细加载仍由父级真实接口方法承接。
- 会员等级旧路径 `/pages/infos/user_vip/index` 的页面结构已抽到 `modern/app-web/src/views/MemberLevelView.vue`，保留会员头部、当前经验、等级进度、会员权益、获取经验和经验值明细；等级读取、当前等级计算和经验记录加载仍由父级真实接口方法承接。
- 推广中心旧路径 `/pages/users/user_spread_user/index`、`/pages/promoter/user_spread_user/index` 的首页结构已抽到 `modern/app-web/src/views/SpreadView.vue`，保留当前佣金、昨日收益、累计已提、提现、提现记录、推广名片、推广人统计、佣金明细、推广订单和排行入口；推广数据读取和各入口跳转仍由父级真实接口方法承接。
- 佣金提现旧路径 `/pages/users/user_cash/index` 的页面结构已抽到 `modern/app-web/src/views/ExtractCashView.vue`，保留银行卡/微信提现 tab、真实姓名、银行卡号、银行选择、微信账号、收款码、提现金额、最低提现金额、可提现佣金、冻结佣金和冻结天数提示；提现校验和提交仍由父级真实接口方法承接。
- 佣金明细和提现记录旧路径 `/pages/promoter/user_spread_money/index?type=2`、`/pages/promoter/user_spread_money/index?type=1` 的页面结构已抽到 `modern/app-web/src/views/BrokerageRecordsView.vue`、`ExtractRecordsView.vue`，保留老移动端顶部金额、日期分组、收入/支出符号、提现状态和空状态；记录读取和返回推广中心仍由父级真实接口方法承接。
- 推广人统计、推广订单、推广名片、推广人排行和佣金排行已继续抽到 `modern/app-web/src/views/SpreadPeopleView.vue`、`SpreadOrdersView.vue`、`SpreadPosterView.vue`、`SpreadRankView.vue`，保留老移动端层级/排序 tab、月份订单分组、海报轮播、推广链接复制、周榜/月榜和排行列表；筛选、海报切换、复制链接和真实接口读取仍由父级方法承接。
- 秒杀列表和秒杀详情旧路径 `/pages/activity/goods_seckill/index`、`/pages/activity/goods_seckill_details/index` 的页面结构已抽到 `modern/app-web/src/views/SeckillView.vue`、`SeckillDetailView.vue`，保留老移动端红色秒杀头部、轮播、时段切换、商品卡、已抢进度、活动信息和“马上抢/未开始/已结束”状态按钮；时段选择、详情打开、原商品和预下单仍由父级真实接口方法承接。
- 拼团列表和拼团详情旧路径 `/pages/activity/goods_combination/index`、`/pages/activity/goods_combination_details/index` 的页面结构已抽到 `modern/app-web/src/views/CombinationView.vue`、`CombinationDetailView.vue`，保留老移动端拼团头部、活动 banner、商品卡、几人团、已拼数量、正在拼团、团长/团员、详情内容和开团/参团按钮；详情打开、单独购买、开团/参团和预下单仍由父级真实接口方法承接。
- 砍价列表、砍价详情和砍价记录旧路径 `/pages/activity/goods_bargain/index`、`/pages/activity/goods_bargain_details/index`、`/pages/activity/bargain/index` 的页面结构已抽到 `modern/app-web/src/views/BargainView.vue`、`BargainDetailView.vue`、`BargainRecordsView.vue`，保留老移动端砍价头部、成功滚动、商品卡、砍价进度、好友帮砍、成功态支付入口、记录卡和继续砍价/重开/付款按钮；开砍、帮砍、预下单和付款仍由父级真实接口方法承接。
- 活动订单旧入口（拼团记录/秒杀订单）已抽到 `modern/app-web/src/views/ActivityOrdersView.vue`，保留老移动端活动订单标题、订单商品卡、活动类型、订单状态、实付金额和查看详情/去支付/取消/售后/物流按钮；订单筛选、支付、取消、售后和物流仍由父级真实接口方法承接。
- 收藏商品和我的优惠券旧入口已抽到 `modern/app-web/src/views/CollectionView.vue`、`UserCouponsView.vue`，保留老移动端收藏管理、全选、取消收藏、优惠券 tab、券面金额、满减门槛、类型标签和状态文案；收藏删除、商品打开和优惠券筛选仍由父级真实接口方法承接。
- 支付方式、优惠券选择、门店选择和余额充值底部弹层已抽到 `modern/app-web/src/components/PaymentMethodPanel.vue`、`CouponSelectPanel.vue`、`StoreSelectPanel.vue`、`RechargePanel.vue`，保留老移动端底部弹层样式、关闭遮罩、支付可用状态、优惠券选择态、门店选择态、充值套餐/自定义金额/充值说明；执行支付、选择优惠券、选择门店和提交充值仍由父级真实接口方法承接。
- 个人中心首页和订单中心已抽到 `modern/app-web/src/views/ProfileView.vue`，保留老移动端登录卡片、用户资料卡、余额/积分/订单统计、快捷入口、活动记录、个人中心轮播、我的服务、订单 tab、订单商品卡和订单操作按钮；登录、退出、菜单跳转、订单筛选、支付/取消/售后/收货/评价/删除/再次购买仍由父级真实接口方法承接。

## 升级顺序

1. 固定入口和代理：去掉旧 IP，所有 H5 请求走同域 `/api`。
2. 路径兼容层：把老 `pages.json` 中的页面路径逐个映射到 `modern/app-web`。当前 61 个老页面路径已全部显式覆盖，检查命令为 `npm run check:legacy-routes`。
3. 简单页面先迁：搜索、分类、商品列表、商品详情、评价、资讯、客服。
4. 交易页面再迁：购物车、确认订单、订单列表、订单详情、支付状态、售后退款。
   - 已完成第一块：`CartView.vue`。
   - 已完成第二块：`CheckoutView.vue`。
   - 已完成第三块：`OrderDetailView.vue`。
   - 已完成第四块：`PaymentView.vue`、`PayStatusView.vue`。
   - 已完成第五块：`RefundApplyView.vue`、`RefundListView.vue`。
   - 已完成第六块：`LogisticsView.vue`、`CommentView.vue`。
   - 已完成第七块：`AddressView.vue`。
   - 已完成第八块：`UserInfoView.vue`、`UserPhoneView.vue`、`UserPasswordView.vue`。
   - 已完成第九块：`BalanceView.vue`、`BillView.vue`。
   - 已完成第十块：`IntegralView.vue`、`SignView.vue`、`SignRecordsView.vue`。
   - 已完成第十一块：`MemberLevelView.vue`。
   - 已完成第十二块：`SpreadView.vue`。
   - 已完成第十三块：`ExtractCashView.vue`。
   - 已完成第十四块：`BrokerageRecordsView.vue`、`ExtractRecordsView.vue`。
   - 已完成第十五块：`SpreadPeopleView.vue`、`SpreadOrdersView.vue`、`SpreadPosterView.vue`、`SpreadRankView.vue`。
   - 已完成第十六块：`SeckillView.vue`、`SeckillDetailView.vue`。
   - 已完成第十七块：`CombinationView.vue`、`CombinationDetailView.vue`。
   - 已完成第十八块：`BargainView.vue`、`BargainDetailView.vue`、`BargainRecordsView.vue`。
   - 已完成第十九块：`ActivityOrdersView.vue`。
   - 已完成第二十块：`CollectionView.vue`、`UserCouponsView.vue`。
5. 活动页面补深：秒杀、拼团、砍价列表和详情，继续按老 UI 和老字段补齐。
6. 工程拆分：逐步把 `App.vue` 中已迁移功能拆成 `views/`、`components/`、`api/`、`utils/`，不一次性大拆。
   - 已完成第一块：`ProductList.vue`。
   - 已完成第二块：`SearchHeader.vue`、`HomeQuickMenu.vue`、`CategoryNav.vue`。
   - 已完成第三块：`ProductRepliesView.vue`。
   - 已完成第四块：`ArticlesView.vue`。
   - 已完成第五块：`CustomerServiceView.vue`。
   - 已完成第六块：`ProductDetailPanel.vue`。
   - 已完成第七块：`CheckoutView.vue`。
   - 已完成第八块：`OrderDetailView.vue`。
   - 已完成第九块：`PaymentView.vue`、`PayStatusView.vue`。
   - 已完成第十块：`RefundApplyView.vue`、`RefundListView.vue`。
   - 已完成第十一块：`LogisticsView.vue`、`CommentView.vue`。
   - 已完成第十二块：`AddressView.vue`。
   - 已完成第十三块：`UserInfoView.vue`、`UserPhoneView.vue`、`UserPasswordView.vue`。
   - 已完成第十四块：`BalanceView.vue`、`BillView.vue`。
   - 已完成第十五块：`IntegralView.vue`、`SignView.vue`、`SignRecordsView.vue`。
   - 已完成第十六块：`MemberLevelView.vue`。
   - 已完成第十七块：`SpreadView.vue`。
   - 已完成第十八块：`ExtractCashView.vue`。
   - 已完成第十九块：`BrokerageRecordsView.vue`、`ExtractRecordsView.vue`。
   - 已完成第二十块：`SpreadPeopleView.vue`、`SpreadOrdersView.vue`、`SpreadPosterView.vue`、`SpreadRankView.vue`。
   - 已完成第二十一块：`SeckillView.vue`、`SeckillDetailView.vue`。
   - 已完成第二十二块：`CombinationView.vue`、`CombinationDetailView.vue`。
   - 已完成第二十三块：`BargainView.vue`、`BargainDetailView.vue`、`BargainRecordsView.vue`。
   - 已完成第二十四块：`ActivityOrdersView.vue`。
   - 已完成第二十五块：`CollectionView.vue`、`UserCouponsView.vue`。
   - 已完成第二十六块：`PaymentMethodPanel.vue`、`CouponSelectPanel.vue`、`StoreSelectPanel.vue`、`RechargePanel.vue`。
   - 已完成第二十七块：`ProfileView.vue`。
   - 已完成第二十八块：`HomeCategoryView.vue`。
   - 已完成第二十九块：`api/frontClient.js`。
   - 已完成第三十块：`utils/format.js`、`utils/order.js`、`utils/activity.js`、`utils/forms.js`、`utils/display.js`。

## 复制规则

- 可以直接参考和复制老 `app/` 页面结构、样式、字段、校验和交互。
- 复制后必须适配 Vue 3、Vite、现代请求封装和当前接口返回。
- 不复制旧构建配置、旧硬编码域名、旧生产第三方密钥。
- 不改数据库结构，不造假数据。
- 微信、支付宝、短信、真实退款等外部写操作默认禁用或 dry-run。

## 验收

- `npm run build` 通过。
- `npm run check:legacy-routes` 返回 `missing: []`。
- 旧 H5 重点路径打开不白屏。
- 页面外观看起来仍是老 CRMEB，不变成新设计。
- 请求不再出现旧项目里的硬编码内网 IP；验收时按运行代码扫描确认。
- 商品、订单、售后、活动页面读取真实老库数据。
