<template>
  <main class="app-shell">
    <HomeCategoryView
      v-if="currentTab === 'home' || currentTab === 'category'"
      :is-home="currentTab === 'home'"
      :is-category="currentTab === 'category'"
      :keyword="keyword"
      :main-banner="mainBanner"
      :banners="homeBanners"
      :menus="menus"
      :diy-components="homeDiyComponents"
      :categories="sortedCategories"
      :home-categories="homeCategories"
      :flat-categories="flatCategories"
      :active-cid="activeCid"
      :tabs="tabs"
      :active-type="activeType"
      :home-index-products="homeIndexProducts"
      :loading="loading"
      :products="products"
      :product-total="productTotal"
      :title="sectionTitle"
      :product-notice="categoryProductNotice"
      :asset-url="assetUrl"
      :menu-icon-class="menuIconClass"
      @update-keyword="keyword = $event"
      @search="searchProducts"
      @open-banner="openBanner"
      @open-menu="handleMenu"
      @open-diy-link="openHomeDiyLink"
      @select-category="selectCategory"
      @show-more="switchTab('category')"
      @select-type="selectType"
      @open-product="openDetail"
    />

    <ProductRepliesView
      v-else-if="currentTab === 'productReplies'"
      :loading="productReplyLoading"
      :replies="productReplies"
      :config="productReplyConfig"
      :type="productReplyType"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @back="currentTab = 'home'"
      @change-type="changeProductReplyType"
    />

    <SearchView
      v-else-if="currentTab === 'search'"
      :keyword="keyword"
      :hot-keywords="searchHotKeywords"
      :loading="loading"
      :products="products"
      :product-total="productTotal"
      :asset-url="assetUrl"
      @back="switchTab('home')"
      @update-keyword="keyword = $event"
      @search="submitSearch"
      @hot-search="searchByHotKeyword"
      @open-product="openDetail"
    />

    <StoreListView
      v-else-if="currentTab === 'storeList'"
      :loading="storeLoading"
      :stores="storeList"
      :asset-url="assetUrl"
      @back="switchTab('home')"
      @refresh="refreshStoreList"
      @map="openStoreMap"
    />

    <section v-else-if="currentTab === 'smallPage'" class="plain-view small-page-view">
      <div class="view-head">
        <button type="button" @click="switchTab('home')">返回</button>
        <h1>{{ smallPageTitle || '微页面' }}</h1>
        <span></span>
      </div>
      <div v-if="smallPageLoading" class="state">加载中...</div>
      <template v-else>
        <HomeDiyRenderer
          v-if="smallPageComponents.length"
          :components="smallPageComponents"
          :asset-url="assetUrl"
          @open-link="openHomeDiyLink"
        />
        <div v-else class="state">暂无微页面内容</div>
      </template>
    </section>

    <CartView
      v-else-if="currentTab === 'cart'"
      :auth-token="authToken"
      :loading="cartLoading"
      :items="cartItems"
      :selected-ids="selectedCartIds"
      :cart-total="cartTotal"
      :manage="cartManage"
      :selected-total="cartSelectedTotal"
      :is-all-selected="isAllCartSelected"
      :asset-url="assetUrl"
      @login="currentTab = 'profile'"
      @toggle-manage="cartManage = !cartManage"
      @toggle-item="toggleCartItem"
      @open-product="openDetail"
      @change-num="changeCartNum"
      @toggle-all="toggleAllCart"
      @checkout="startCheckout"
      @collect-selected="collectSelectedCart"
      @delete-selected="deleteSelectedCart"
    />

    <SeckillView
      v-else-if="currentTab === 'seckill'"
      :loading="seckillLoading"
      :current-header="currentSeckillHeader"
      :current-slides="currentSeckillSlides"
      :headers="seckillHeaders"
      :active-time-id="activeSeckillTimeId"
      :items="seckillItems"
      :asset-url="assetUrl"
      :button-text="seckillListButtonText"
      @back="switchTab('home')"
      @select-time="selectSeckillTime"
      @open="openSeckillDetail"
    />

    <SeckillDetailView
      v-else-if="currentTab === 'seckillDetail'"
      :loading="seckillDetailLoading"
      :item="selectedSeckill"
      :current-header="currentSeckillHeader"
      :status="seckillDetailStatus"
      :asset-url="assetUrl"
      :date-text="seckillDateText"
      @back="switchTab('seckill')"
      @open-master="openSeckillMasterProduct"
      @checkout="startSeckillCheckout"
    />

    <CombinationView
      v-else-if="currentTab === 'combination'"
      :loading="combinationLoading"
      :header="combinationHeader"
      :items="combinationItems"
      :asset-url="assetUrl"
      :button-text="combinationListButtonText"
      :date-text="seckillDateText"
      @back="switchTab('home')"
      @open="openCombinationDetail"
    />

    <CombinationDetailView
      v-else-if="currentTab === 'combinationDetail'"
      :loading="combinationDetailLoading"
      :item="selectedCombination"
      :status="combinationDetailStatus"
      :pink-list="combinationPinkList"
      :member-list="combinationMemberList"
      :invite-pink-id="combinationInvitePinkId"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :date-text="seckillDateText"
      :pink-text="combinationPinkText"
      :can-join-pink="canJoinPink"
      @back="switchTab('combination')"
      @open-master="openCombinationMasterProduct"
      @checkout="startCombinationCheckout"
      @join="startCombinationCheckout"
    />

    <BargainView
      v-else-if="currentTab === 'bargain'"
      :loading="bargainLoading"
      :header="bargainHeader"
      :success-list="bargainSuccessList"
      :items="bargainItems"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @back="switchTab('home')"
      @open="openBargainDetail"
    />

    <BargainDetailView
      v-else-if="currentTab === 'bargainDetail'"
      :loading="bargainDetailLoading"
      :item="selectedBargain"
      :user-info="bargainUserInfo"
      :help-list="bargainUserHelpList"
      :user-status-text="bargainUserStatusText"
      :is-success="isBargainSuccess"
      :action-text="bargainActionText"
      :action-disabled="bargainActionDisabled"
      :share-link="bargainManualCopyLink"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :money-text="moneyText"
      :master-status-text="bargainMasterStatusText"
      @back="switchTab('bargain')"
      @continue="switchTab('bargain')"
      @action="handleBargainAction"
      @close-share="bargainManualCopyLink = ''"
      @open-master="openBargainMasterProduct"
    />

    <BargainRecordsView
      v-else-if="currentTab === 'bargainRecords'"
      :auth-token="authToken"
      :loading="bargainRecordLoading"
      :items="bargainRecordItems"
      :asset-url="assetUrl"
      :money-text="moneyText"
      :status-text="bargainRecordStatusText"
      :main-status-text="bargainRecordMainStatusText"
      :main-status-class="bargainRecordMainStatusClass"
      :can-pay="canPayBargainRecord"
      :can-create-order="canCreateBargainOrder"
      @back="currentTab = 'profile'"
      @refresh="loadBargainRecords"
      @login="currentTab = 'profile'"
      @pay="payBargainRecord"
      @open="openBargainRecordDetail"
      @restart="switchTab('bargain')"
    />

    <ActivityOrdersView
      v-else-if="currentTab === 'activityOrders'"
      :auth-token="authToken"
      :loading="activityOrderLoading"
      :title="activityOrderTitle"
      :orders="activityOrderItems"
      :paying-order="payingOrder"
      :asset-url="assetUrl"
      :type-text="activityOrderTypeText"
      :is-unpaid="isUnpaid"
      :can-apply-refund="canApplyRefund"
      :can-view-logistics="canViewLogistics"
      @back="currentTab = 'profile'"
      @refresh="loadActivityOrders"
      @login="currentTab = 'profile'"
      @open="openOrderDetail"
      @pay="payOrder"
      @cancel="cancelOrder"
      @refund="openRefund"
      @logistics="openLogistics"
    />

    <CollectionView
      v-else-if="currentTab === 'collection'"
      :auth-token="authToken"
      :loading="collectLoading"
      :items="collectItems"
      :total="collectTotal"
      :manage="collectManage"
      :selected-ids="selectedCollectIds"
      :is-all-selected="isAllCollectSelected"
      :asset-url="assetUrl"
      @back="currentTab = 'profile'"
      @login="currentTab = 'profile'"
      @toggle-manage="collectManage = !collectManage"
      @toggle-item="toggleCollectItem"
      @toggle-all="toggleAllCollect"
      @delete-selected="deleteSelectedCollect"
      @open-product="openDetail"
    />

    <UserCouponsView
      v-else-if="currentTab === 'userCoupons'"
      :auth-token="authToken"
      :loading="userCouponLoading"
      :tabs="userCouponTabs"
      :type="userCouponType"
      :items="userCouponItems"
      :status-text="couponStatusText"
      :coupon-type-text="couponTypeText"
      @back="currentTab = 'profile'"
      @login="currentTab = 'profile'"
      @select-type="selectUserCouponType"
    />

    <CouponCenterView
      v-else-if="currentTab === 'couponCenter'"
      :auth-token="authToken"
      :loading="couponCenterLoading"
      :tabs="couponCenterTabs"
      :type="couponCenterType"
      :items="couponCenterItems"
      :receiving-id="receivingCouponId"
      :coupon-type-text="couponTypeText"
      @back="currentTab = 'profile'"
      @login="currentTab = 'profile'"
      @select-type="selectCouponCenterType"
      @receive="receiveCoupon"
    />

    <ArticlesView
      v-else-if="currentTab === 'articles'"
      mode="list"
      :loading="articleLoading"
      :categories="articleCategories"
      :banners="articleBanners"
      :articles="articleItems"
      :active-cid="activeArticleCid"
      :asset-url="assetUrl"
      @back="currentTab = 'profile'"
      @refresh="loadArticles"
      @select-category="selectArticleCategory"
      @open-detail="openArticleDetail"
    />

    <ArticlesView
      v-else-if="currentTab === 'articleDetail'"
      mode="detail"
      :detail-loading="articleDetailLoading"
      :selected-article="selectedArticle"
      :asset-url="assetUrl"
      @back-detail="currentTab = 'articles'"
      @open-product="openDetail"
    />

    <CustomerServiceView
      v-else-if="currentTab === 'customerService'"
      :loading="customerServiceLoading"
      :config="customerServiceConfig"
      :chat-url="customerChatUrl"
      :title="customerServiceTitle"
      @back="currentTab = customerReturnTab"
      @open-chat="openCustomerChatWindow"
    />

    <LegacySafeView
      v-else-if="currentTab === 'legacySafe'"
      :context="legacySafeContext"
      @back="switchTab"
      @profile="switchTab('profile')"
      @home="switchTab('home')"
      @pay-status="loadPayStatus"
      @payment="openPaymentPage"
    />

    <BalanceView
      v-else-if="currentTab === 'balance'"
      :auth-token="authToken"
      :loading="balanceLoading"
      :info="balanceInfo"
      :money-text="moneyText"
      @back="currentTab = 'profile'"
      @login="currentTab = 'profile'"
      @recharge="openRechargePanel"
      @open-bill="openBill"
      @integral="switchTab('integral')"
      @sign="switchTab('sign')"
      @coupons="switchTab('couponCenter')"
    />

    <BillView
      v-else-if="currentTab === 'bill'"
      :auth-token="authToken"
      :loading="billLoading"
      :tabs="billTabs"
      :active-type="billType"
      :groups="billGroups"
      :money-text="moneyText"
      @back="currentTab = 'balance'"
      @login="currentTab = 'profile'"
      @select="selectBillType"
    />

    <IntegralView
      v-else-if="currentTab === 'integral'"
      :auth-token="authToken"
      :loading="integralLoading"
      :info="integralInfo"
      :panel="integralPanel"
      :records="integralRecords"
      @back="currentTab = 'balance'"
      @login="currentTab = 'profile'"
      @select-panel="integralPanel = $event"
      @home="switchTab('home')"
      @sign="switchTab('sign')"
    />

    <SignView
      v-else-if="currentTab === 'sign'"
      :auth-token="authToken"
      :loading="signLoading"
      :user="userInfo"
      :default-avatar="defaultAvatar"
      :info="signInfo"
      :config="signConfig"
      :list="signList"
      :count-digits="signCountDigits"
      :signing="signing"
      :asset-url="assetUrl"
      @login="currentTab = 'profile'"
      @records="openSignRecords"
      @sign="signNow"
    />

    <SignRecordsView
      v-else-if="currentTab === 'signRecords'"
      :loading="signRecordsLoading"
      :groups="signRecordGroups"
      @back="currentTab = 'sign'"
    />

    <MemberLevelView
      v-else-if="currentTab === 'memberLevel'"
      :auth-token="authToken"
      :loading="memberLevelLoading"
      :user="userInfo"
      :current-level="currentLevel"
      :levels="memberLevels"
      :records="experienceRecords"
      :progress="memberProgress"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @login="currentTab = 'profile'"
      @sign="switchTab('sign')"
      @category="switchTab('category')"
    />

    <SpreadView
      v-else-if="currentTab === 'spread'"
      :auth-token="authToken"
      :loading="spreadLoading"
      :info="spreadInfo"
      :money-text="moneyText"
      @login="currentTab = 'profile'"
      @extract-records="openExtractRecords"
      @extract-cash="openExtractCash"
      @poster="openSpreadPoster"
      @people="switchTab('spreadPeople')"
      @brokerage-records="openBrokerageRecords"
      @orders="openSpreadOrders"
      @spread-rank="openSpreadRank"
      @brokerage-rank="openBrokerageRank"
    />

    <ExtractCashView
      v-else-if="currentTab === 'extractCash'"
      v-model:type="extractCashType"
      :loading="extractCashLoading"
      :submitting="submittingExtractCash"
      :form="extractCashForm"
      :info="extractCashInfo"
      :banks="extractBankList"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
      @records="openExtractRecords"
      @submit="submitExtractCash"
      @update-field="updateExtractCashField"
    />

    <BrokerageRecordsView
      v-else-if="currentTab === 'brokerageRecords'"
      :loading="brokerageRecordLoading"
      :groups="brokerageRecordGroups"
      :total="spreadInfo.commissionCount"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
    />

    <ExtractRecordsView
      v-else-if="currentTab === 'extractRecords'"
      :loading="extractRecordLoading"
      :groups="extractRecordGroups"
      :total="spreadInfo.extractCount"
      :money-text="moneyText"
      :status-text="extractStatusText"
      @back="currentTab = 'spread'"
    />

    <SpreadPeopleView
      v-else-if="currentTab === 'spreadPeople'"
      :loading="spreadPeopleLoading"
      :summary="spreadPeopleSummary"
      :items="spreadPeopleItems"
      :grade="spreadGrade"
      :sort-key="spreadSortKey"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
      @select-grade="selectSpreadGrade"
      @select-sort="selectSpreadSort"
    />

    <SpreadPosterView
      v-else-if="currentTab === 'spreadPoster'"
      :loading="spreadPosterLoading"
      :posters="spreadPosterList"
      :current-poster="currentPoster"
      :poster-index="spreadPosterIndex"
      :user="userInfo"
      :link="spreadPosterLink"
      :manual-copy-link="spreadManualCopyLink"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @back="currentTab = 'spread'"
      @select="selectSpreadPoster"
      @copy="copySpreadPosterLink"
      @close-manual-copy="spreadManualCopyLink = ''"
    />

    <SpreadOrdersView
      v-else-if="currentTab === 'spreadOrders'"
      :loading="spreadOrderLoading"
      :groups="spreadOrderGroups"
      :count="spreadOrderCount"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
    />

    <SpreadRankView
      v-else-if="currentTab === 'spreadRank'"
      title="推广人排行"
      mode="spread"
      :loading="rankLoading"
      :type="rankType"
      :items="spreadRankList"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
      @select-type="selectRankType"
    />

    <SpreadRankView
      v-else-if="currentTab === 'brokerageRank'"
      title="佣金排行"
      mode="brokerage"
      :loading="rankLoading"
      :type="rankType"
      :items="brokerageRankList"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      :money-text="moneyText"
      @back="currentTab = 'spread'"
      @select-type="selectRankType"
    />

    <CheckoutView
      v-else-if="currentTab === 'checkout'"
      v-model:use-integral="checkoutUseIntegral"
      v-model:pickup-contact="pickupContact"
      v-model:pickup-phone="pickupPhone"
      v-model:mark="checkoutMark"
      :loading="checkoutLoading"
      :items="checkoutItems"
      :address="checkoutAddress"
      :price="checkoutPrice"
      :shipping-type="checkoutShippingType"
      :store-self-mention="storeSelfMention"
      :selected-store="selectedStore"
      :selected-coupon="selectedCoupon"
      :activity-type-text="activityOrderTypeText"
      :activity-hint="activityOrderHint"
      :user-info="userInfo"
      :creating="creatingOrder"
      :asset-url="assetUrl"
      @back="switchTab('cart')"
      @select-shipping="selectShippingType"
      @open-address="openAddressBook('checkout')"
      @open-store="openStorePanel"
      @open-coupon="openCouponPanel"
      @recompute="recomputeCheckoutPrice"
      @create="createOrder"
    />

    <AddressView
      v-else-if="currentTab === 'address'"
      :loading="addressLoading"
      :addresses="addresses"
      :editing="addressEditing"
      :form="addressForm"
      :saving="savingAddress"
      @back="currentTab = addressReturnTab"
      @create="createAddress"
      @choose="chooseAddress"
      @edit="editAddress"
      @set-default="setDefaultAddress"
      @delete="deleteAddress"
      @close-edit="addressEditing = false"
      @save="saveAddress"
      @update-field="updateAddressField"
    />

    <OrderDetailView
      v-else-if="currentTab === 'orderDetail'"
      :loading="orderDetailLoading"
      :order="selectedOrder"
      :is-refund-detail="isRefundOrderDetail"
      :paying-order="payingOrder"
      :asset-url="assetUrl"
      :order-goods-list="orderGoodsList"
      :goods-name="goodsName"
      :goods-image="goodsImage"
      :goods-sku="goodsSku"
      :goods-price="goodsPrice"
      :goods-num="goodsNum"
      :refund-status-text="refundStatusText"
      :is-unpaid="isUnpaid"
      :can-apply-refund="canApplyRefund"
      :can-view-logistics="canViewLogistics"
      :can-take-order="canTakeOrder"
      :can-comment-order="canCommentOrder"
      :can-comment-goods="canCommentGoods"
      :can-delete-order="canDeleteOrder"
      :can-order-again="canOrderAgain"
      @back="returnFromOrderDetail"
      @pay="payOrder"
      @cancel="cancelOrder"
      @refund="openRefund"
      @logistics="openLogistics"
      @take="takeOrder"
      @comment-first="openFirstComment"
      @comment-goods="openComment"
      @delete="deleteOrder"
      @again="startAgainCheckout"
    />

    <PaymentView
      v-else-if="currentTab === 'paymentPage'"
      :loading="paymentPageLoading"
      :payment-loading="paymentLoading"
      :order="paymentPageOrder"
      :methods="paymentMethods"
      :paying-order="payingOrder"
      :money-text="moneyText"
      :payment-method-enabled="paymentMethodEnabled"
      :payment-method-sub-text="paymentMethodSubText"
      :payment-icon-class="paymentIconClass"
      :payment-icon-text="paymentIconText"
      @back="currentTab = 'profile'"
      @choose="choosePaymentMethod"
    />

    <PayStatusView
      v-else-if="currentTab === 'payStatus'"
      :loading="payStatusLoading"
      :info="payStatusInfo"
      :title="payStatusTitle"
      :money-text="moneyText"
      :payment-method-text="paymentMethodText"
      :is-unpaid="isUnpaid"
      :can-apply-refund="canApplyRefund"
      :can-view-logistics="canViewLogistics"
      :can-take-order="canTakeOrder"
      :can-comment-order="canCommentOrder"
      :can-order-again="canOrderAgain"
      :activity-type-text="activityOrderTypeText"
      :activity-hint="activityOrderHint"
      :activity-action-text="activityOrderActionText"
      :pink-share-link="pinkManualCopyLink"
      @back="switchTab('home')"
      @open-order="openOrderFromPayStatus"
      @open-pink="openPinkFromPayStatus"
      @open-combination="openCombinationFromPayStatus"
      @open-activity="openActivityFromPayStatus"
      @close-pink-share="pinkManualCopyLink = ''"
      @home="switchTab('home')"
      @pay="payOrder"
      @logistics="openLogistics"
      @take="takeOrder"
      @refund="openRefund"
      @comment-first="openFirstComment"
      @again="startAgainCheckout"
    />

    <LogisticsView
      v-else-if="currentTab === 'logistics'"
      :loading="logisticsLoading"
      :info="logisticsInfo"
      :product="logisticsProduct"
      :tracks="logisticsList"
      :manual-copy-id="deliveryManualCopyId"
      :asset-url="assetUrl"
      @back="currentTab = selectedOrder ? 'orderDetail' : 'profile'"
      @copy="copyDeliveryId"
      @close-manual-copy="deliveryManualCopyId = ''"
    />

    <CommentView
      v-else-if="currentTab === 'comment'"
      v-model:product-score="commentForm.productScore"
      v-model:service-score="commentForm.serviceScore"
      v-model:comment="commentForm.comment"
      :loading="commentLoading"
      :goods="commentGoods"
      :pics="commentPics"
      :uploading="commentUploading"
      :submitting="submittingComment"
      :asset-url="assetUrl"
      @back="returnFromComment"
      @submit="submitComment"
      @upload="uploadCommentImage"
      @remove-pic="removeCommentPic"
    />

    <RefundApplyView
      v-else-if="currentTab === 'refund'"
      v-model:refund-text="refundForm.text"
      v-model:refund-explain="refundForm.explain"
      :loading="refundLoading"
      :order="refundOrder"
      :reasons="refundReasons"
      :pics="refundPics"
      :uploading="refundUploading"
      :submitting="submittingRefund"
      :asset-url="assetUrl"
      :order-goods-list="orderGoodsList"
      :goods-name="goodsName"
      :goods-image="goodsImage"
      :goods-sku="goodsSku"
      :goods-price="goodsPrice"
      :goods-num="goodsNum"
      :order-goods-total="orderGoodsTotal"
      @back="returnFromRefund"
      @submit="submitRefund"
      @upload="uploadRefundImage"
      @remove-pic="removeRefundPic"
    />

    <RefundListView
      v-else-if="currentTab === 'refundList'"
      :loading="refundListLoading"
      :orders="refundOrders"
      :tabs="refundTabs"
      :active-status="refundStatus"
      :asset-url="assetUrl"
      :order-goods-list="orderGoodsList"
      :goods-name="goodsName"
      :goods-image="goodsImage"
      :goods-sku="goodsSku"
      :goods-price="goodsPrice"
      :goods-num="goodsNum"
      :order-goods-total="orderGoodsTotal"
      :refund-status-text="refundStatusText"
      @back="switchTab('profile')"
      @refresh="loadRefundOrders"
      @open="openRefundDetail"
      @change-status="selectRefundStatus"
    />

    <UserInfoView
      v-else-if="currentTab === 'userInfo'"
      :auth-token="authToken"
      :user="userInfo"
      :form="profileForm"
      :saving="savingProfile"
      :avatar-uploading="avatarUploading"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @back="currentTab = 'profile'"
      @login="currentTab = 'profile'"
      @save="saveUserProfile"
      @phone="openPhoneChange"
      @password="openPasswordChange"
      @address="openAddressBook('userInfo')"
      @logout="logout"
      @update-field="updateProfileField"
      @upload-avatar="uploadAvatarImage"
    />

    <UserPhoneView
      v-else-if="currentTab === 'userPhone'"
      :auth-token="authToken"
      :user="userInfo"
      :form="phoneForm"
      :step="phoneStep"
      :saving="savingPhone"
      @back="currentTab = 'userInfo'"
      @login="currentTab = 'profile'"
      @save="savePhoneChange"
      @send-code="sendAccountCode"
      @update-field="updatePhoneField"
    />

    <UserPasswordView
      v-else-if="currentTab === 'userPassword'"
      :auth-token="authToken"
      :user="userInfo"
      :form="passwordForm"
      :saving="savingPassword"
      :mask-phone="maskPhone"
      @back="currentTab = 'userInfo'"
      @login="currentTab = 'profile'"
      @save="savePasswordChange"
      @send-code="sendAccountCode"
      @update-field="updatePasswordField"
    />

    <OrderListView
      v-else-if="currentTab === 'orders'"
      :auth-token="authToken"
      :order-data="orderData"
      :order-count-summary="orderCountSummary"
      :order-tabs="orderTabs"
      :order-type="orderType"
      :order-loading="orderLoading"
      :orders="orders"
      :paying-order="payingOrder"
      :asset-url="assetUrl"
      :order-goods-list="orderGoodsList"
      :goods-name="goodsName"
      :goods-image="goodsImage"
      :goods-sku="goodsSku"
      :goods-price="goodsPrice"
      :goods-num="goodsNum"
      :refund-status-text="refundStatusText"
      :is-unpaid="isUnpaid"
      :can-apply-refund="canApplyRefund"
      :can-take-order="canTakeOrder"
      :can-comment-order="canCommentOrder"
      :can-delete-order="canDeleteOrder"
      :can-order-again="canOrderAgain"
      @back="switchTab('profile')"
      @login="currentTab = 'profile'"
      @refresh-orders="loadOrders"
      @select-order-type="selectOrderType"
      @open-order="openOrderDetail"
      @pay-order="payOrder"
      @cancel-order="cancelOrder"
      @refund-order="openRefund"
      @take-order="takeOrder"
      @comment-order="openFirstComment"
      @delete-order="deleteOrder"
      @again-order="startAgainCheckout"
    />

    <ProfileView
      v-else
      :auth-token="authToken"
      :user="userInfo"
      :login-form="loginForm"
      :login-loading="loginLoading"
      :auth-message="authMessage"
      :default-avatar="defaultAvatar"
      :user-coupon-usable-count="userCouponUsableCount"
      :addresses="addresses"
      :profile-banners="profileBanners"
      :profile-menus="mergedProfileMenus"
      :profile-menu-loading="profileMenuLoading"
      :order-data="orderData"
      :order-count-summary="orderCountSummary"
      :order-tabs="orderTabs"
      :order-type="orderType"
      :order-loading="orderLoading"
      :orders="orders"
      :paying-order="payingOrder"
      :asset-url="assetUrl"
      :order-goods-list="orderGoodsList"
      :goods-name="goodsName"
      :goods-image="goodsImage"
      :goods-sku="goodsSku"
      :goods-price="goodsPrice"
      :goods-num="goodsNum"
      :refund-status-text="refundStatusText"
      :is-unpaid="isUnpaid"
      :can-apply-refund="canApplyRefund"
      :can-take-order="canTakeOrder"
      :can-comment-order="canCommentOrder"
      :can-delete-order="canDeleteOrder"
      :can-order-again="canOrderAgain"
      @logout="logout"
      @login="login"
      @update-login-field="updateLoginField"
      @balance="switchTab('balance')"
      @integral="switchTab('integral')"
      @user-info="openUserInfo"
      @collection="switchTab('collection')"
      @coupons="switchTab('userCoupons')"
      @address="openAddressBook('profile')"
      @refund-list="switchTab('refundList')"
      @bargain-records="switchTab('bargainRecords')"
      @activity-orders="openActivityOrders"
      @profile-menu="handleProfileMenu"
      @refresh-orders="loadOrders"
      @select-order-type="selectOrderType"
      @open-order="openOrderDetail"
      @pay-order="payOrder"
      @cancel-order="cancelOrder"
      @refund-order="openRefund"
      @take-order="takeOrder"
      @comment-order="openFirstComment"
      @delete-order="deleteOrder"
      @again-order="startAgainCheckout"
    />

    <ProductDetailPanel
      :open="detailOpen"
      :loading="detailLoading"
      :detail="detail"
      :detail-image="detailImage"
      :selected-sku="selectedDetailSku"
      :selected-attrs="selectedDetailAttrs"
      :cart-num="detailCartNum"
      :cart-badge-count="cartTotal"
      :receiving-coupon-id="receivingCouponId"
      :collecting="detailCollecting"
      :purchase-disabled-reason="detailPurchaseDisabledReason"
      :default-avatar="defaultAvatar"
      :asset-url="assetUrl"
      @close="detailOpen = false"
      @collect="toggleDetailCollect"
      @service="openDetailCustomerService"
      @cart="openDetailCart"
      @select-attr="selectDetailAttr"
      @change-cart-num="changeDetailCartNum"
      @add-cart="addDetailToCart"
      @buy-now="buyNow"
      @open-replies="openProductReplies"
      @open-product="openDetail"
      @receive-coupon="receiveProductCoupon"
    />

    <PaymentMethodPanel
      v-if="paymentOpen"
      :loading="paymentLoading"
      :methods="paymentMethods"
      :method-enabled="paymentMethodEnabled"
      :method-sub-text="paymentMethodSubText"
      :icon-class="paymentIconClass"
      :icon-text="paymentIconText"
      @close="closePayment"
      @execute="executePayment"
    />

    <CouponSelectPanel
      v-if="couponOpen"
      :loading="couponLoading"
      :coupons="couponList"
      :selected-coupon="selectedCoupon"
      :coupon-type-text="couponTypeText"
      @close="closeCouponPanel"
      @select="selectCoupon"
    />

    <StoreSelectPanel
      v-if="storePanelOpen"
      :loading="storeLoading"
      :stores="storeList"
      :selected-store="selectedStore"
      :asset-url="assetUrl"
      @close="closeStorePanel"
      @choose="chooseStore"
    />

    <RechargePanel
      v-if="rechargeOpen"
      :loading="rechargeLoading"
      :submitting="submittingRecharge"
      :config="rechargeConfig"
      :selected-id="selectedRechargeId"
      :custom-price="customRechargePrice"
      :money-text="moneyText"
      @close="closeRechargePanel"
      @select-quota="selectRechargeQuota"
      @update-custom-price="customRechargePrice = $event"
      @custom-focus="selectedRechargeId = null"
      @submit="submitRecharge"
    />

    <div v-if="toast" class="toast">{{ toast }}</div>
    <div v-if="confirmDialog.open" class="confirm-mask">
      <section class="confirm-dialog">
        <h3>{{ confirmDialog.title }}</h3>
        <p>{{ confirmDialog.message }}</p>
        <div class="confirm-actions">
          <button type="button" @click="resolveConfirm(false)">{{ confirmDialog.cancelText }}</button>
          <button type="button" class="primary" @click="resolveConfirm(true)">{{ confirmDialog.confirmText }}</button>
        </div>
      </section>
    </div>

    <nav class="bottom-tabs">
      <button v-for="item in bottomTabs" :key="item.key" :class="{ active: currentTab === item.key }" type="button" @click="switchTab(item.key)">
        <span>{{ item.icon }}</span>
        {{ item.name }}
      </button>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { assetUrl, createFrontClient } from "./api/frontClient";
import CouponSelectPanel from "./components/CouponSelectPanel.vue";
import PaymentMethodPanel from "./components/PaymentMethodPanel.vue";
import RechargePanel from "./components/RechargePanel.vue";
import StoreSelectPanel from "./components/StoreSelectPanel.vue";
import { legacyActivityOrderType, legacyTabFromLocation, matchedLegacyTab } from "./legacy/routes";
import {
  activityOrderMatches,
  activityOrderActionText,
  activityOrderHint,
  activityOrderTypeText,
  bargainRecordMainStatusClass,
  bargainRecordMainStatusText,
  bargainRecordStatusText,
  canCreateBargainOrder,
  canPayBargainRecord,
  combinationItemEnded,
  combinationPinkText,
  firstActivityAttrValueId
} from "./utils/activity";
import {
  couponStatusText,
  couponTypeText,
  extractStatusText,
  maskPhone,
  menuIconClass,
  paymentIconClass,
  paymentIconText
} from "./utils/display";
import { dateTimeText as seckillDateText, moneyText, timestampMs } from "./utils/format";
import { emptyAddressForm, emptyCommentForm, emptyExtractCashForm, emptyRefundForm, updateFormField } from "./utils/forms";
import {
  canApplyRefund,
  canCommentGoods,
  canCommentOrder,
  canDeleteOrder,
  canOrderAgain,
  canTakeOrder,
  canViewLogistics,
  goodsImage,
  goodsName,
  goodsNum,
  goodsPrice,
  goodsSku,
  isUnpaid,
  orderGoodsList,
  orderGoodsTotal,
  paymentMethodText,
  refundStatusText
} from "./utils/order";
import AddressView from "./views/AddressView.vue";
import ActivityOrdersView from "./views/ActivityOrdersView.vue";
import ArticlesView from "./views/ArticlesView.vue";
import BalanceView from "./views/BalanceView.vue";
import BargainDetailView from "./views/BargainDetailView.vue";
import BargainRecordsView from "./views/BargainRecordsView.vue";
import BargainView from "./views/BargainView.vue";
import BillView from "./views/BillView.vue";
import BrokerageRecordsView from "./views/BrokerageRecordsView.vue";
import CartView from "./views/CartView.vue";
import CollectionView from "./views/CollectionView.vue";
import CommentView from "./views/CommentView.vue";
import CombinationDetailView from "./views/CombinationDetailView.vue";
import CombinationView from "./views/CombinationView.vue";
import CouponCenterView from "./views/CouponCenterView.vue";
import CustomerServiceView from "./views/CustomerServiceView.vue";
import CheckoutView from "./views/CheckoutView.vue";
import ExtractCashView from "./views/ExtractCashView.vue";
import ExtractRecordsView from "./views/ExtractRecordsView.vue";
import HomeCategoryView from "./views/HomeCategoryView.vue";
import HomeDiyRenderer from "./components/HomeDiyRenderer.vue";
import LogisticsView from "./views/LogisticsView.vue";
import LegacySafeView from "./views/LegacySafeView.vue";
import MemberLevelView from "./views/MemberLevelView.vue";
import OrderDetailView from "./views/OrderDetailView.vue";
import OrderListView from "./views/OrderListView.vue";
import PaymentView from "./views/PaymentView.vue";
import PayStatusView from "./views/PayStatusView.vue";
import ProductDetailPanel from "./views/ProductDetailPanel.vue";
import ProductRepliesView from "./views/ProductRepliesView.vue";
import ProfileView from "./views/ProfileView.vue";
import RefundApplyView from "./views/RefundApplyView.vue";
import RefundListView from "./views/RefundListView.vue";
import IntegralView from "./views/IntegralView.vue";
import SearchView from "./views/SearchView.vue";
import SignRecordsView from "./views/SignRecordsView.vue";
import SignView from "./views/SignView.vue";
import SpreadOrdersView from "./views/SpreadOrdersView.vue";
import SpreadPeopleView from "./views/SpreadPeopleView.vue";
import SpreadPosterView from "./views/SpreadPosterView.vue";
import SpreadRankView from "./views/SpreadRankView.vue";
import SpreadView from "./views/SpreadView.vue";
import StoreListView from "./views/StoreListView.vue";
import SeckillDetailView from "./views/SeckillDetailView.vue";
import SeckillView from "./views/SeckillView.vue";
import UserCouponsView from "./views/UserCouponsView.vue";
import UserInfoView from "./views/UserInfoView.vue";
import UserPasswordView from "./views/UserPasswordView.vue";
import UserPhoneView from "./views/UserPhoneView.vue";

const loading = ref(false);
const detailLoading = ref(false);
const detailOpen = ref(false);
const cartLoading = ref(false);
const orderLoading = ref(false);
const checkoutLoading = ref(false);
const addressLoading = ref(false);
const orderDetailLoading = ref(false);
const refundLoading = ref(false);
const commentLoading = ref(false);
const logisticsLoading = ref(false);
const paymentPageLoading = ref(false);
const payStatusLoading = ref(false);
const articleLoading = ref(false);
const articleDetailLoading = ref(false);
const productReplyLoading = ref(false);
const customerServiceLoading = ref(false);
const collectLoading = ref(false);
const userCouponLoading = ref(false);
const couponCenterLoading = ref(false);
const balanceLoading = ref(false);
const billLoading = ref(false);
const rechargeLoading = ref(false);
const integralLoading = ref(false);
const profileMenuLoading = ref(false);
const signLoading = ref(false);
const signRecordsLoading = ref(false);
const memberLevelLoading = ref(false);
const spreadLoading = ref(false);
const brokerageRecordLoading = ref(false);
const extractRecordLoading = ref(false);
const extractCashLoading = ref(false);
const spreadPeopleLoading = ref(false);
const spreadOrderLoading = ref(false);
const rankLoading = ref(false);
const spreadPosterLoading = ref(false);
const seckillLoading = ref(false);
const seckillDetailLoading = ref(false);
const combinationLoading = ref(false);
const combinationDetailLoading = ref(false);
const bargainLoading = ref(false);
const bargainDetailLoading = ref(false);
const bargainRecordLoading = ref(false);
const activityOrderLoading = ref(false);
const refundListLoading = ref(false);
const payingOrder = ref("");
const loginLoading = ref(false);
const creatingOrder = ref(false);
const savingAddress = ref(false);
const savingProfile = ref(false);
const savingPhone = ref(false);
const savingPassword = ref(false);
const submittingRefund = ref(false);
const submittingComment = ref(false);
const commentUploading = ref(false);
const refundUploading = ref(false);
const avatarUploading = ref(false);
const detailCollecting = ref(false);
const signing = ref(false);
const submittingExtractCash = ref(false);
const submittingRecharge = ref(false);
const cartManage = ref(false);
const collectManage = ref(false);
const products = ref([]);
const productTotal = ref(0);
const categories = ref([]);
const categoryProductCounts = ref({});
const categoryProductNotice = ref("");
const menus = ref([]);
const profileMenus = ref([]);
const profileBanners = ref([]);
const homeBanners = ref([]);
const homeIndexTabs = ref([]);
const homeIndexProducts = ref([]);
const homeDiyProducts = ref([]);
const homeSaleListStyle = ref("1");
const homeDiyComponents = ref([]);
const smallPageLoading = ref(false);
const smallPageTitle = ref("");
const smallPageComponents = ref([]);
const detail = ref(null);
const selectedDetailAttrs = ref({});
const detailCartNum = ref(1);
const cartItems = ref([]);
const selectedCartIds = ref([]);
const collectItems = ref([]);
const selectedCollectIds = ref([]);
const userCouponItems = ref([]);
const couponCenterItems = ref([]);
const billGroups = ref([]);
const integralRecords = ref([]);
const signConfig = ref([]);
const signInfo = ref({});
const signList = ref([]);
const signRecordGroups = ref([]);
const memberLevels = ref([]);
const experienceRecords = ref([]);
const brokerageRecordGroups = ref([]);
const extractRecordGroups = ref([]);
const spreadPeopleItems = ref([]);
const spreadOrderGroups = ref([]);
const spreadRankList = ref([]);
const brokerageRankList = ref([]);
const spreadPosterList = ref([]);
const spreadManualCopyLink = ref("");
const deliveryManualCopyId = ref("");
const pinkManualCopyLink = ref("");
const seckillHeaders = ref([]);
const seckillItems = ref([]);
const selectedSeckill = ref(null);
const combinationHeader = ref({});
const combinationItems = ref([]);
const selectedCombination = ref(null);
const combinationInvitePinkId = ref("");
const bargainHeader = ref({});
const bargainItems = ref([]);
const bargainRecordItems = ref([]);
const activityOrderItems = ref([]);
const refundOrders = ref([]);
const articleCategories = ref([]);
const articleBanners = ref([]);
const articleItems = ref([]);
const selectedBargain = ref(null);
const selectedArticle = ref(null);
const bargainUserInfo = ref(null);
const bargainManualCopyLink = ref("");
const productReplyProductId = ref("");
const productReplyType = ref(0);
const productReplyConfig = ref({});
const productReplies = ref([]);
const cartTotal = ref(0);
const collectTotal = ref(0);
const userCouponUsableCount = ref(0);
const orders = ref([]);
const orderData = ref({});
const balanceInfo = ref({});
const rechargeConfig = ref({});
const integralInfo = ref({});
const spreadInfo = ref({});
const extractCashInfo = ref({});
const spreadPeopleSummary = ref({});
const spreadOrderCount = ref(0);
const spreadPosterIndex = ref(0);
const extractCashType = ref("bank");
const extractBankList = ref([]);
const extractCashForm = ref({
  name: "",
  cardum: "",
  bankName: "",
  wechat: "",
  qrcodeUrl: "",
  money: ""
});
const addresses = ref([]);
const checkoutItems = ref([]);
const checkoutAddress = ref({});
const checkoutPrice = ref({});
const preOrderNo = ref("");
const selectedOrder = ref(null);
const paymentPageOrder = ref(null);
const payStatusInfo = ref(null);
const refundOrder = ref(null);
const commentOrder = ref(null);
const commentGoods = ref(null);
const logisticsInfo = ref(null);
const refundReasons = ref([]);
const couponList = ref([]);
const selectedCoupon = ref(null);
const storeList = ref([]);
const selectedStore = ref(null);
const userInfo = ref(null);
const payConfig = ref({});
const customerServiceConfig = ref({});
const customerServiceTitle = ref("联系客服");
const paymentOrder = ref(null);
const rechargeOpen = ref(false);
const selectedRechargeId = ref(null);
const customRechargePrice = ref("");
const checkoutUseIntegral = ref(false);
const checkoutMark = ref("");
const checkoutShippingType = ref(0);
const storeSelfMention = ref(false);
const pickupContact = ref("");
const pickupPhone = ref("");
const paymentOpen = ref(false);
const paymentLoading = ref(false);
const couponOpen = ref(false);
const couponLoading = ref(false);
const receivingCouponId = ref(0);
const storePanelOpen = ref(false);
const storeLoading = ref(false);
const keyword = ref("");
const searchHotKeywords = ref([]);
const activeCid = ref("");
const activeType = ref(0);
const legacyGoodsTitle = ref("");
const legacyProductRank = ref(false);
const orderType = ref(null);
const refundStatus = ref(null);
const userCouponType = ref("usable");
const couponCenterType = ref(1);
const billType = ref("all");
const integralPanel = ref("detail");
const spreadGrade = ref(0);
const spreadSortKey = ref("");
const rankType = ref("week");
const activeSeckillTimeId = ref("");
const activeArticleCid = ref("0");
const initialParams = new URLSearchParams(window.location.search);
const initialLegacyOrderType = legacyOrderStatus(window.location.pathname, initialParams);
if (initialLegacyOrderType !== undefined) {
  orderType.value = initialLegacyOrderType;
}
if (["all", "expenditure", "income"].includes(initialParams.get("type") || "")) {
  billType.value = initialParams.get("type") || "all";
}
const activityOrderType = ref(legacyActivityOrderType(window.location.pathname, initialParams));
const customerReturnTab = ref("profile");
const addressReturnTab = ref("profile");
const addressEditing = ref(false);
const orderDetailFromRefund = ref(false);
const toast = ref("");
const confirmDialog = ref({
  open: false,
  title: "",
  message: "",
  confirmText: "确定",
  cancelText: "取消",
  resolver: null
});
const authMessage = ref("");
const initialBargainId = initialParams.get("id") || initialParams.get("bargainId");
const initialBargainUserId = initialParams.get("storeBargainId") || initialParams.get("bargainUserId");
const initialSeckillId = initialParams.get("id") || initialParams.get("seckillId");
const initialCombinationId = initialParams.get("id") || initialParams.get("combinationId");
const initialCombinationPinkId = initialParams.get("pinkId");
const initialArticleId = initialParams.get("id") || initialParams.get("articleId");
const initialSmallPageId = initialParams.get("id") || initialParams.get("diyId");
const initialProductId = initialParams.get("productId") || initialParams.get("id");
const initialOrderId = initialParams.get("orderId") || initialParams.get("order_id");
const initialCommentUnique = initialParams.get("unique") || initialParams.get("uni");
const initialCommentInfoId = initialParams.get("id");
const initialPaymentOrderNo = initialParams.get("orderNo") || initialParams.get("order_id") || initialOrderId;
const initialPayStatus = initialParams.get("status") || "";
const initialWebUrl = initialParams.get("webUel") || initialParams.get("webUrl") || initialParams.get("url") || "";
const initialOrderIsReturn = initialParams.get("isReturen") === "1" || initialParams.get("isReturn") === "1";
const initialPromotionType = legacyPromotionType(window.location.pathname, initialParams);
const initialPromotionRank = legacyPromotionRank(window.location.pathname, initialParams);
if (initialPromotionType > 0) {
  activeType.value = initialPromotionType;
  legacyGoodsTitle.value = initialParams.get("name") || initialParams.get("title") || "";
}
if (initialPromotionRank) {
  activeType.value = 0;
  legacyProductRank.value = true;
  legacyGoodsTitle.value = initialParams.get("name") || initialParams.get("title") || "商品排行";
}
const currentTab = ref(legacyTabFromLocation(window.location.pathname, initialParams));
const legacySafeContext = ref({
  pathname: window.location.pathname,
  params: Object.fromEntries(initialParams.entries()),
  returnTab: "profile"
});
const spreadStorageKey = "crmeb-front-spread";
const initialSpreadPid = normalizeSpreadPid(initialParams.get("spread") || initialParams.get("spreadPid") || initialParams.get("spread_spid"));
if (initialSpreadPid) {
  localStorage.setItem(spreadStorageKey, String(initialSpreadPid));
}
const urlToken = initialParams.get("token");
if (urlToken) {
  localStorage.setItem("crmeb-front-token", urlToken);
}
const authToken = ref(urlToken || localStorage.getItem("crmeb-front-token") || "");
const frontClient = createFrontClient(() => authToken.value);
const apiGet = frontClient.get;
const apiPost = frontClient.post;
const apiUpload = frontClient.upload;
const loginForm = ref({ account: "", password: "" });
const profileForm = ref({ nickname: "", avatar: "" });
const phoneForm = ref({ phone: "", captcha: "" });
const passwordForm = ref({ password: "", confirmPassword: "", captcha: "" });
const phoneStep = ref(1);
const addressForm = ref(emptyAddressForm());
const refundForm = ref(emptyRefundForm());
const commentForm = ref(emptyCommentForm());
const commentPics = ref([]);
const refundPics = ref([]);
const defaultAvatar = "/crmebimage/perset/staticImg/f.png";

const fallbackTabs = [
  { type: 0, name: "综合" },
  { type: 1, name: "精品" },
  { type: 2, name: "热门" },
  { type: 3, name: "新品" },
  { type: 4, name: "促销" }
];

const fallbackProfileMenus = [
  { id: "fallback-balance", name: "我的余额", url: "/pages/users/user_money/index", icon: "余" },
  { id: "fallback-bill", name: "账单明细", url: "/pages/users/user_bill/index", icon: "账" },
  { id: "fallback-sign", name: "每日签到", url: "/pages/users/user_sgin/index", icon: "签" },
  { id: "fallback-vip", name: "会员中心", url: "/pages/infos/user_vip/index", icon: "会" },
  { id: "fallback-spread", name: "推广中心", url: "/pages/promoter/user_spread_user/index", icon: "推" },
  { id: "fallback-brokerage", name: "佣金记录", url: "/pages/promoter/user_spread_money/index?type=2", icon: "佣" },
  { id: "fallback-extract", name: "提现记录", url: "/pages/promoter/user_spread_money/index?type=1", icon: "提" },
  { id: "fallback-service", name: "联系客服", url: "/pages/service/index", icon: "客" }
];

const orderTabs = [
  { type: null, name: "全部" },
  { type: 0, name: "待付款" },
  { type: 1, name: "待发货" },
  { type: 2, name: "待收货" },
  { type: 3, name: "待评价" },
  { type: -3, name: "售后" }
];

const refundTabs = [
  { value: null, name: "全部" },
  { value: 1, name: "申请中" },
  { value: 3, name: "退款中" },
  { value: 2, name: "已退款" },
  { value: 4, name: "已拒绝" }
];

const userCouponTabs = [
  { type: "usable", name: "未使用" },
  { type: "unusable", name: "已使用/过期" }
];

const couponCenterTabs = [
  { type: 1, name: "通用券" },
  { type: 2, name: "商品券" },
  { type: 3, name: "品类券" }
];

const billTabs = [
  { type: "all", name: "全部" },
  { type: "expenditure", name: "消费" },
  { type: "income", name: "充值" }
];

const bottomTabs = [
  { key: "home", name: "首页", icon: "home" },
  { key: "category", name: "分类", icon: "cate" },
  { key: "cart", name: "购物车", icon: "cart" },
  { key: "profile", name: "我的", icon: "user" }
];

const sortedCategories = computed(() =>
  categories.value.filter(shouldShowCategory).map((category) => ({
    ...category,
    name: cleanDisplayText(category.name),
    child: sortCategoriesByProducts((category.child || []).filter(shouldShowCategory))
  }))
);
const flatCategories = computed(() => sortedCategories.value.flatMap((item) => [item, ...(item.child || [])]).slice(0, 18));
const homeCategories = computed(() => flatCategories.value.slice(0, 9));
const mainBanner = computed(() => homeBanners.value[0] || null);
const tabs = computed(() => homeIndexTabs.value.length ? homeIndexTabs.value : fallbackTabs);
const mergedProfileMenus = computed(() => {
  const map = new Map();
  for (const item of [...profileMenus.value, ...fallbackProfileMenus]) {
    const name = String(item?.name || "").trim();
    const url = legacyMenuUrl(item);
    const key = name || url || item?.id;
    if (!key || map.has(key)) {
      continue;
    }
    map.set(key, item);
  }
  return [...map.values()];
});
const currentPoster = computed(() => spreadPosterList.value[spreadPosterIndex.value] || null);
const currentSeckillHeader = computed(() =>
  seckillHeaders.value.find((item) => Number(item.id) === Number(activeSeckillTimeId.value)) || seckillHeaders.value[0] || null
);
const currentSeckillSlides = computed(() => currentSeckillHeader.value?.slide || []);
const currentSeckillStatus = computed(() => Number(currentSeckillHeader.value?.status || 0));
const seckillDetailStatus = computed(() => {
  if (!selectedSeckill.value) {
    return "-";
  }
  if (Number(selectedSeckill.value.quota || 0) <= 0 || Number(selectedSeckill.value.stock || 0) <= 0) {
    return "已售罄";
  }
  const now = Date.now();
  const start = timestampMs(selectedSeckill.value.startTime);
  const stop = timestampMs(selectedSeckill.value.stopTime);
  if ((start && now < start) || currentSeckillStatus.value === 1) {
    return "未开始";
  }
  if (stop && now >= stop) {
    return "已结束";
  }
  if (currentSeckillStatus.value === 2 || (!currentSeckillStatus.value && (!start || now >= start) && (!stop || now < stop))) {
    return "抢购中";
  }
  return "已结束";
});
const combinationPinkList = computed(() => selectedCombination.value?.pinkList || selectedCombination.value?.pinkingList || []);
const combinationMemberList = computed(() =>
  selectedCombination.value?.pinkMemberList
  || selectedCombination.value?.memberList
  || selectedCombination.value?.pinkUserList
  || selectedCombination.value?.pinkOrderList
  || []
);
const combinationDetailStatus = computed(() => {
  if (!selectedCombination.value) {
    return "-";
  }
  if (Number(selectedCombination.value.quota || selectedCombination.value.stock || 0) <= 0) {
    return "已售罄";
  }
  const now = Date.now();
  const start = timestampMs(selectedCombination.value.startTime);
  if (start && now < start) {
    return "未开始";
  }
  if (combinationItemEnded(selectedCombination.value)) {
    return "已结束";
  }
  return "拼团中";
});
const bargainSuccessList = computed(() => bargainHeader.value?.bargainSuccessList || []);
const bargainDetailStatus = computed(() => {
  if (!selectedBargain.value) {
    return "-";
  }
  if (selectedBargain.value.isSoldOut || Number(selectedBargain.value.quota || 0) <= 0) {
    return "已售罄";
  }
  const now = Date.now();
  const start = timestampMs(selectedBargain.value.startTime);
  const stop = timestampMs(selectedBargain.value.stopTime);
  if (start && start > now) {
    return "未开始";
  }
  if (stop && stop < now) {
    return "已结束";
  }
  return "进行中";
});
const bargainUserHelpList = computed(() => bargainUserInfo.value?.userHelpList || []);
const bargainUserStatusText = computed(() => {
  const status = Number(bargainUserInfo.value?.bargainStatus || 0);
  const map = {
    1: "可以参与",
    2: "参与次数已满",
    3: "砍价中",
    4: "砍价成功",
    5: "可以帮砍",
    6: "已帮砍",
    7: "帮砍次数已满",
    8: "待支付",
    9: "已支付",
    10: "订单已取消"
  };
  return map[status] || bargainDetailStatus.value;
});
const isBargainSuccess = computed(() => {
  if (!bargainUserInfo.value) {
    return false;
  }
  const status = Number(bargainUserInfo.value?.bargainStatus || 0);
  return status === 4 || status === 8 || status === 9 || Number(bargainUserInfo.value?.surplusPrice || 0) === 0;
});
const isRefundOrderDetail = computed(() =>
  orderDetailFromRefund.value || Number(selectedOrder.value?.refundStatus || 0) > 0
);
const bargainActionText = computed(() => {
  if (!authToken.value) return "登录后参与";
  if (!selectedBargain.value || selectedBargain.value.isSoldOut || bargainDetailStatus.value === "已售罄") return "已售罄";
  if (bargainDetailStatus.value === "未开始" || bargainDetailStatus.value === "已结束") return bargainDetailStatus.value;
  const status = Number(bargainUserInfo.value?.bargainStatus || 0);
  if (status === 1) return "参与砍价";
  if (status === 3) return "邀请好友帮砍价";
  if (status === 4) return "立即支付";
  if (status === 5) return "帮好友砍一刀";
  if (status === 6) return "我也要参与";
  if (status === 7) return "我也要参与";
  if (status === 8) return "查看砍价记录";
  if (status === 9) return "已支付";
  if (status === 10) return "重新参与";
  return selectedBargain.value.isSoldOut ? "已售罄" : "参与砍价";
});
const bargainActionDisabled = computed(() =>
  ["已售罄", "未开始", "已结束", "已支付"].includes(bargainActionText.value)
);
const activityOrderTitle = computed(() => {
  if (activityOrderType.value === "seckill") {
    return "秒杀订单";
  }
  if (activityOrderType.value === "bargain") {
    return "砍价订单";
  }
  return "拼团记录";
});
const spreadPosterLink = computed(() => {
  const uid = userInfo.value?.uid || userInfo.value?.id || "";
  const base = window.location.origin + window.location.pathname;
  return uid ? `${base}?spread=${uid}` : base;
});
const bargainShareLink = computed(() => {
  if (!selectedBargain.value?.id || !bargainUserInfo.value?.storeBargainUserId) {
    return "";
  }
  const url = new URL(window.location.origin + window.location.pathname);
  url.searchParams.set("tab", "bargainDetail");
  url.searchParams.set("id", selectedBargain.value.id);
  url.searchParams.set("storeBargainId", bargainUserInfo.value.storeBargainUserId);
  return url.toString();
});
const pinkShareLink = computed(() => {
  const info = payStatusInfo.value;
  const pinkId = info?.pinkId;
  if (!pinkId) {
    return "";
  }
  return combinationShareUrl(pinkId, info?.combinationId, info?.orderId || info?.orderNo);
});
const detailImage = computed(() => {
  const images = detail.value?.productInfo?.sliderImage || [];
  return images[0] || detail.value?.productInfo?.image;
});
const firstSku = computed(() => {
  const values = Object.values(detail.value?.productValue || {});
  return values[0] || null;
});
const selectedDetailSku = computed(() => {
  const values = Object.values(detail.value?.productValue || {});
  if (!values.length) {
    return null;
  }
  const attrs = detail.value?.productAttr || [];
  const selectedValues = attrs.map((attr) => selectedDetailAttrs.value[attr.attrName]).filter(Boolean);
  if (attrs.length && selectedValues.length === attrs.length) {
    return values.find((item) => skuMatches(item, selectedValues)) || null;
  }
  return firstSku.value;
});
const detailPurchaseDisabledReason = computed(() => {
  const product = detail.value?.productInfo;
  if (!product) {
    return "";
  }
  if (Number(product.isShow ?? 1) !== 1) {
    return "商品已下架";
  }
  if (Number(product.isDel || 0) === 1 || Number(product.isRecycle || 0) === 1) {
    return "商品已失效";
  }
  if (Object.keys(detail.value?.productValue || {}).length && !selectedDetailSku.value?.id) {
    return "请选择规格";
  }
  const stock = Number(selectedDetailSku.value?.stock ?? product.stock ?? 0);
  if (stock <= 0) {
    return "已售罄";
  }
  return "";
});
const sectionTitle = computed(() => {
  if (legacyGoodsTitle.value) {
    return legacyGoodsTitle.value;
  }
  if (keyword.value) {
    return `搜索「${keyword.value}」`;
  }
  if (currentTab.value === "category") {
    return "分类商品";
  }
  return "推荐商品";
});
const orderCountSummary = computed(() =>
  Object.values(orderData.value).reduce((sum, value) => sum + (Number(value) || 0), 0)
);
const cartPayTotal = computed(() =>
  cartItems.value
    .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.cartNum || 0), 0)
    .toFixed(2)
);
const selectedCartItems = computed(() =>
  cartItems.value.filter((item) => selectedCartIds.value.includes(Number(item.id)))
);
const cartSelectedTotal = computed(() =>
  selectedCartItems.value
    .reduce((sum, item) => sum + Number(item.vipPrice || item.price || 0) * Number(item.cartNum || 0), 0)
    .toFixed(2)
);
const isAllCartSelected = computed(() =>
  cartItems.value.length > 0 && selectedCartIds.value.length === cartItems.value.length
);
const isAllCollectSelected = computed(() =>
  collectItems.value.length > 0 && selectedCollectIds.value.length === collectItems.value.length
);
const payBalance = computed(() =>
  Number(payConfig.value.userBalance ?? userInfo.value?.nowMoney ?? 0).toFixed(2)
);
const paymentMethods = computed(() => {
  const list = Array.isArray(payConfig.value.payConfig) ? payConfig.value.payConfig : [];
  if (list.length) {
    return list;
  }
  return [
    { name: "微信支付", value: "weixin", title: "微信快捷支付", payStatus: payConfig.value.payWechatOpen ? 1 : 0 },
    { name: "余额支付", value: "yue", title: "可用余额", payStatus: payConfig.value.yuePayStatus === false ? 0 : 1, userBalance: payBalance.value },
    { name: "支付宝支付", value: "alipay", title: "支付宝快捷支付", payStatus: payConfig.value.aliPayStatus ? 1 : 0 }
  ];
});
const payStatusTitle = computed(() => {
  if (initialPayStatus === "2") {
    return "订单取消支付";
  }
  if (Number(payStatusInfo.value?.paid || 0) === 1) {
    return "支付成功";
  }
  return "支付失败";
});
const customerChatUrl = computed(() => {
  const value = customerServiceConfig.value.chatUrl || customerServiceConfig.value.yzfUrl || "";
  return /^https?:\/\//.test(value) ? value : "";
});
const logisticsProduct = computed(() => logisticsInfo.value?.order?.info?.[0] || null);
const logisticsList = computed(() => logisticsInfo.value?.express?.list || []);
const signCountDigits = computed(() => String(signInfo.value.sumSignDay || 0).padStart(4, "0").slice(-4).split(""));
const currentLevel = computed(() => {
  const experience = Number(userInfo.value?.experience || 0);
  const userLevelId = Number(userInfo.value?.level || 0);
  const matchedLevel = memberLevels.value.find((item) => Number(item.id) === userLevelId);
  if (matchedLevel) {
    return matchedLevel;
  }
  return [...memberLevels.value]
    .sort((left, right) => Number(right.experience || 0) - Number(left.experience || 0))
    .find((item) => experience >= Number(item.experience || 0)) || memberLevels.value[0] || null;
});
const memberProgress = computed(() => {
  if (!memberLevels.value.length) {
    return 0;
  }
  const experience = Number(userInfo.value?.experience || 0);
  const maxExperience = Math.max(...memberLevels.value.map((item) => Number(item.experience || 0)), 1);
  return Math.max(0, Math.min(100, Math.round((experience / maxExperience) * 100)));
});

async function loadHome() {
  const [indexData, categoryData, pageDiyData] = await Promise.all([
    apiGet("/api/front/index"),
    apiGet("/api/front/category"),
    apiGet("/api/front/page/diy/default").catch(() => null)
  ]);
  menus.value = (indexData.menus || []).slice(0, 8);
  homeBanners.value = indexData.banner || [];
  homeIndexTabs.value = normalizeHomeIndexTabs(indexData.indexTable || indexData.explosiveMoney || []);
  homeSaleListStyle.value = String(indexData.homePageSaleListStyle || "1");
  homeDiyComponents.value = parseHomeDiyComponents(pageDiyData?.value);
  homeDiyProducts.value = extractHomeDiyProducts(homeDiyComponents.value);
  homeIndexProducts.value = mergeHomeProducts(
    normalizeHomeIndexProducts(indexData.explosiveMoney || []),
    homeDiyProducts.value
  );
  categories.value = normalizeCategoryTree(categoryData || []);
  loadCategoryProductCounts();
  recordVisit(1);
}

function normalizeHomeIndexTabs(list) {
  return (Array.isArray(list) ? list : [])
    .map((item, index) => ({
      ...item,
      type: `layout-${item.id || item.tempid || index}`,
      name: item.name || item.title || `推荐${index + 1}`,
      info: item.info || item.desc || item.subtitle || "",
      image: item.image || item.pic || "",
      url: legacyMenuUrl(item),
      sourceIndex: index
    }))
    .filter((item) => item.name && item.status !== false)
    .slice(0, 8);
}

function normalizeHomeIndexProducts(list) {
  return (Array.isArray(list) ? list : [])
    .filter((item) => item && (item.storeName || item.name || item.title || item.image || item.pic))
    .map((item, index) => normalizeDisplayProduct({
      id: item.productId || item.id || `layout-${index}`,
      storeName: item.storeName || item.name || item.title || "精选商品",
      image: item.image || item.pic || item.cover || "",
      price: item.price || item.payPrice || item.productPrice || "",
      otPrice: item.otPrice || item.ot_price || "",
      sales: Number(item.sales || item.ficti || 0),
      ficti: Number(item.ficti || 0),
      unitName: item.unitName || "件",
      url: legacyMenuUrl(item),
      layoutItem: true
    }))
    .filter(isDisplayReadyProduct)
    .slice(0, 8);
}

function extractHomeDiyProducts(components) {
  return mergeHomeProducts(
    [],
    (components || [])
      .filter((component) => component?.type === "home_goods_list")
      .flatMap((component) => component.productItems || [])
      .map((item, index) => normalizeDisplayProduct({
        id: item.id || item.productId || `diy-product-${index}`,
        storeName: item.storeName || item.title || item.name || "推荐商品",
        image: item.image || item.pic || item.cover || "",
        price: item.price || item.payPrice || item.productPrice || "0.00",
        otPrice: item.otPrice || item.oldPrice || item.ot_price || "",
        sales: Number(item.sales || item.ficti || 0),
        ficti: 0,
        unitName: item.unitName || "件",
        url: item.link || (item.id ? `/pages/goods/goods_details/index?id=${item.id}` : ""),
        layoutItem: true
      }))
      .filter(isDisplayReadyProduct)
  );
}

function mergeHomeProducts(primary, extra) {
  const seen = new Set();
  return [...(primary || []), ...(extra || [])]
    .map(normalizeDisplayProduct)
    .filter(isDisplayReadyProduct)
    .filter((item) => {
      const key = String(item.id || item.url || `${item.storeName}-${item.image}`);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    })
    .slice(0, 20);
}

function isLayoutType(type) {
  return String(type || "").startsWith("layout-");
}

function activeLayoutTab(type = activeType.value) {
  if (!isLayoutType(type)) {
    return null;
  }
  return homeIndexTabs.value.find((item) => String(item.type) === String(type)) || null;
}

async function loadSmallPage(id = initialSmallPageId || 0) {
  smallPageLoading.value = true;
  try {
    const data = await apiGet(`/api/front/page/diy/${Number(id || 0)}`);
    smallPageTitle.value = data?.title || data?.name || "微页面";
    smallPageComponents.value = parseHomeDiyComponents(data?.value);
  } catch (error) {
    smallPageComponents.value = [];
    showToast(error.message || "微页面当前无法打开，请稍后重试");
  } finally {
    smallPageLoading.value = false;
  }
}

function parseHomeDiyComponents(value) {
  if (!value) {
    return [];
  }
  let parsed;
  try {
    parsed = typeof value === "string" ? JSON.parse(value) : value;
  } catch {
    return [];
  }
  const list = Array.isArray(parsed) ? parsed : Array.isArray(parsed?.list) ? parsed.list : Object.values(parsed || {});
  return list
    .filter((item) => item && typeof item === "object")
    .map((item, index) => normalizeHomeDiyComponent(item, index));
}

function normalizeHomeDiyComponent(item, index) {
  const type = normalizeHomeDiyType(item.type || item.name || item.id || "custom");
  const rawLink = typeof item.link === "object" ? item.link?.value : item.link;
  const componentTitle = firstValue(item.titleConfig?.val, item.title, item.nameText, item.txt, item.cname, item.label, item.setUp?.cname);
  const images = normalizeHomeDiyItems(
    item.images || item.imgList || item.picList || item.pictureList || item.list || item.pic || item.picStyle?.picList
      || item.pictureCure?.list || item.tpmf?.list || item.swiperConfig?.list || item.contentStyle?.list
  );
  const menus = normalizeHomeDiyItems(
    item.menus || item.menuList?.list || item.menuList || item.navList || item.menuConfig?.list || item.listConfig?.list
      || item.tabConfig?.list || item.tabVal?.list || item.buttonConfig?.list
  );
  const tabMenus = normalizeHomeDiyItems(item.tabItemConfig?.list);
  const notices = normalizeHomeDiyItems(item.notices || item.newsList || item.listConfig?.list || item.newsConfig?.list);
  const merchants = normalizeHomeDiyMerchants(item.merchants || item.merchantList || item.merList || item.activeValueMer?.list || item.activeValueMer?.activeValue);
  const hotspots = normalizeHomeDiyHotspots(item.hotspots || item.checkoutConfig?.hotspot || item.areaData || item.imgAreaData);
  const activeTab = item.tabItemConfig?.list?.[Number(item.tabItemConfig?.tabVal || 0)]?.activeList || {};
  const productItems = normalizeHomeDiyProducts(
    item.productItems || item.products || item.goods || item.goodsList?.list || item.productList?.list
      || item.selectConfig?.goodsList || item.selectConfigList || item.brandGoodsList || activeTab.goods || activeTab.selectConfigList
  );
  const couponItems = normalizeHomeDiyCoupons(item.couponItems || item.coupons || item.couponList || item.selectConfig?.couponList);
  const articleItems = normalizeHomeDiyArticles(item.articleItems || item.articles || item.articleList || item.selectConfig?.articleList);
  const hotspotImage = firstImage(
    item.image,
    item.pic,
    item.picStyle?.picList?.[0]?.image,
    item.picStyle?.picList?.[0]?.img,
    images[0]?.image
  );
  return {
    ...item,
    uid: item.uid || `home-diy-${type}-${index}`,
    id: item.id || type,
    name: item.name || type,
    type,
    cname: item.cname || item.label || item.title || firstValue(item.setUp?.cname) || type,
    title: componentTitle,
    subTitle: firstValue(item.subTitle, item.subtitle, item.titleFuConfig?.val),
    rightText: firstValue(item.rightText, item.moreText, item.titleRightConfig?.val, "更多"),
    showMore: item.showMore !== undefined
      ? Boolean(item.showMore)
      : item.selectShow?.tabVal !== undefined
        ? Number(item.selectShow.tabVal) === 0
        : Boolean(item.titleRightConfig?.val || rawLink || item.linkConfig?.val || item.url),
    bgImage: item.bgImage || item.bgImg?.url || "",
    isHide: item.isHide === true || item.isHide === 1 || item.isHide === "1",
    bgColor: item.bgColor || item.backgroundColor || colorValue(item.bgColor || item.bgColorConfig || item.borderColor),
    link: firstLink(rawLink, item.url, item.path, item.info, item.linkConfig),
    videoUrl: firstLink(item.videoUrl, item.uploadVideo?.url, item.link?.value, item.video),
    coverImage: item.coverImage || item.cover?.url || item.cover?.val || "",
    text: htmlText(item.text, item.content, item.html, item.richText, item.ueditor),
    limit: Number(item.limit || item.numConfig?.val || item.numConfig || item.count || item.menuConfig?.maxList || activeTab.num || 4),
    itemStyle: Number(item.itemStyle || item.itemStyle?.tabVal || activeTab.styleType || 0),
    height: Number(item.height || item.boxHeight || item.space || item.heightConfig?.val || item.blankConfig?.val || 18),
    hotWord: firstValue(item.hotWord, item.hotWords?.list?.[0]?.val, item.hotWords?.value, item.placeWords?.value, item.placeWords?.val),
    logo: item.logo || item.logoUrl || item.logoConfig?.url || "",
    images,
    menus: tabMenus.length ? tabMenus : menus,
    notices,
    merchants,
    productItems,
    couponItems,
    articleItems,
    image: hotspotImage,
    hotspots,
    footerItems: normalizeHomeDiyItems(item.footerItems || item.menuList?.list || item.menuList)
  };
}

function normalizeHomeDiyType(type) {
  const key = String(type || "").trim();
  const map = {
    headerSerch: "search_box",
    search: "search_box",
    swiperBg: "banner",
    swiper: "banner",
    pictureCube: "picture_cube",
    pictureCure: "picture_cube",
    menus: "home_menu",
    homeMenu: "home_menu",
    tabNav: "nav_bar",
    news: "home_news_roll",
    homeComb: "home_comb",
    homeHotspot: "home_hotspot",
    footer: "home_footer",
    titles: "home_title",
    title: "home_title",
    homeTab: "home_tab",
    richTextEditor: "z_ueditor",
    richText: "z_ueditor",
    ueditor: "z_ueditor",
    guide: "z_auxiliary_line",
    auxiliaryLine: "z_auxiliary_line",
    blankPage: "z_auxiliary_box",
    auxiliaryBox: "z_auxiliary_box",
    homeGoods: "home_goods_list",
    goodList: "home_goods_list",
    goodsList: "home_goods_list",
    homeCoupons: "home_coupon",
    coupon: "home_coupon",
    homeMerchant: "home_merchant",
    merchant: "home_merchant",
    seckill: "home_seckill",
    group: "home_group",
    bargain: "home_bargain",
    homeArticle: "home_article",
    article: "home_article",
    video: "home_video",
    combination: "home_group"
  };
  return map[key] || key;
}

function normalizeHomeDiyItems(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    if (typeof item === "string") {
      return { uid: `home-diy-item-${index}`, title: `项目${index + 1}`, image: item, link: "" };
    }
    const info = Array.isArray(item.info) ? item.info : [];
    const children = Array.isArray(item.chiild) ? item.chiild : Array.isArray(item.child) ? item.child : [];
    return {
      uid: item.uid || `home-diy-item-${index}-${item.id || ""}`,
      title: firstValue(item.title, item.name, item.val, info[0]?.value, info[0]?.val, children[0]?.val, `项目${index + 1}`),
      image: firstImage(item.image, item.pic, item.img, item.url, item.info?.image, item.checked, item.unchecked),
      activeImage: item.checked || item.activeImage || item.image || item.pic || "",
      inactiveImage: item.unchecked || item.inactiveImage || item.image || item.pic || "",
      link: firstLink(item.link, item.path, item.href, item.urlLink, item.url, item.value, item.activeList?.link, info[1]?.value, info[0]?.value, children[1]?.val)
    };
  });
}

function normalizeHomeDiyHotspots(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const parentWidth = Number(item.parentWidth || item.nowImgWidth || item.imgWidth || 390) || 390;
    const parentHeight = Number(item.parentHeight || item.imgHeight || 220) || 220;
    const fromPercent = item.x !== undefined || item.y !== undefined || item.width !== undefined || item.height !== undefined;
    return {
      uid: item.uid || `home-diy-hotspot-${index}`,
      title: firstValue(item.title, item.name, `热区${index + 1}`),
      link: firstLink(item.link, item.url, item.value),
      x: clampPercent(fromPercent ? item.x : Number(item.starX || 0) / parentWidth * 100),
      y: clampPercent(fromPercent ? item.y : Number(item.starY || 0) / parentHeight * 100),
      width: clampPercent(fromPercent ? item.width : Number(item.areaWidth || 120) / parentWidth * 100, 1),
      height: clampPercent(fromPercent ? item.height : Number(item.areaHeight || 60) / parentHeight * 100, 1)
    };
  });
}

function normalizeHomeDiyMerchants(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    if (typeof item === "string" || typeof item === "number") {
      return {
        uid: `home-diy-merchant-${index}`,
        id: item,
        title: `商户${item}`,
        subTitle: "品质好店",
        image: "",
        link: `/pages/goods/goods_list/index?merId=${item}`
      };
    }
    const id = item.id || item.merId || item.value || "";
    return {
      uid: item.uid || `home-diy-merchant-${index}-${id}`,
      id,
      title: firstValue(item.title, item.name, item.merName, `推荐店铺${index + 1}`),
      subTitle: firstValue(item.subTitle, item.label, item.typeName, "品质好店"),
      image: firstImage(item.image, item.logo, item.avatar, item.pic),
      link: firstLink(item.link, item.url, item.path, id ? `/pages/goods/goods_list/index?merId=${id}` : "/pages/goods/goods_list/index")
    };
  });
}

function normalizeHomeDiyProducts(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const id = item.id || item.productId || item.product_id || item.storeId || "";
    const price = firstValue(item.price, item.otPrice, item.ot_price, item.seckillPrice, item.pinkPrice, item.minPrice, item.bargainPrice);
    return {
      uid: item.uid || `home-diy-product-${index}-${id}`,
      id,
      title: firstValue(item.storeName, item.store_name, item.title, item.name, `商品${index + 1}`),
      image: firstImage(item.image, item.pic, item.cover, item.imageInput, item.sliderImage),
      price,
      oldPrice: firstValue(item.otPrice, item.ot_price, item.originalPrice, item.productPrice),
      sales: firstValue(item.sales, item.ficti, item.salesNum, item.stock),
      unitName: firstValue(item.unitName, item.unit_name, "件"),
      link: firstLink(item.link, item.url, item.path, id ? `/pages/goods/goods_details/index?id=${id}` : "")
    };
  });
}

function normalizeHomeDiyCoupons(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const id = item.id || item.couponId || "";
    const useMinPrice = firstValue(item.useMinPrice, item.use_min_price, item.minPrice);
    return {
      uid: item.uid || `home-diy-coupon-${index}-${id}`,
      id,
      title: firstValue(item.name, item.title, item.couponTitle, "优惠券"),
      money: firstValue(item.money, item.couponPrice, item.coupon_price, item.price, item.discount),
      useMinPrice,
      limitText: useMinPrice ? `满${useMinPrice}可用` : firstValue(item.limitText, item.desc, "领取后可用"),
      link: firstLink(item.link, item.url, item.path, id ? `/pages/activity/couponList/index?id=${id}` : "/pages/activity/couponList/index")
    };
  });
}

function normalizeHomeDiyArticles(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === "object" ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const id = item.id || item.articleId || "";
    return {
      uid: item.uid || `home-diy-article-${index}-${id}`,
      id,
      title: firstValue(item.title, item.name, `文章${index + 1}`),
      image: firstImage(item.imageInput, item.image, item.pic),
      time: firstValue(item.updateTime, item.addTime, item.createTime, item.date),
      link: firstLink(item.link, item.url, id ? `/pages/news/news_details/index?id=${id}` : "/pages/news/news_list/index")
    };
  });
}

function clampPercent(value, min = 0) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return min;
  }
  return Math.max(min, Math.min(100, Number(number.toFixed(1))));
}

function firstValue(...values) {
  return values.map(plainText).find((value) => value) || "";
}

function firstLink(...values) {
  return values.map(plainLink).find((value) => value) || "";
}

function firstImage(...values) {
  return values.map(plainLink).find((value) => isImageLike(value)) || "";
}

function htmlText(...values) {
  return values.map((value) => (typeof value === "string" ? value.trim() : "")).find((value) => value && value !== "[object Object]") || "";
}

function plainText(value) {
  if (value === undefined || value === null) {
    return "";
  }
  if (typeof value === "string" || typeof value === "number") {
    const text = String(value).trim();
    return text === "[object Object]" ? "" : text;
  }
  return "";
}

function plainLink(value) {
  if (typeof value === "string" || typeof value === "number") {
    return plainText(value);
  }
  if (value && typeof value === "object") {
    return firstValue(value.url, value.path, value.link, value.value, value.val, value.href, value.image, value.pic, value.img);
  }
  return "";
}

function isImageLike(value) {
  if (!value) {
    return false;
  }
  const text = String(value).trim();
  return !isInvalidDiyImage(text)
    && /(^https?:\/\/|^\/|^crmebimage\/|^public\/|\\.(png|jpe?g|gif|webp|svg)(\\?|$))/i.test(text);
}

function isInvalidDiyImage(value) {
  const text = String(value || "").trim();
  return /^data:image\//i.test(text)
    || text.includes("704a508f4d4e44e6a05a65d2dcb7aebbcg9yr5u6xn.jpg");
}

function colorValue(config) {
  const color = Array.isArray(config?.color) ? config.color[0]?.item : "";
  return color || "";
}

async function loadProfileMenus() {
  profileMenuLoading.value = true;
  try {
    const data = await apiGet("/api/front/menu/user");
    profileMenus.value = data.routine_my_menus || [];
    profileBanners.value = data.routine_my_banner || [];
  } catch (error) {
    showToast(error.message);
  } finally {
    profileMenuLoading.value = false;
  }
}

async function loadArticles() {
  articleLoading.value = true;
  try {
    const [categories, banners] = await Promise.all([
      apiGet("/api/front/article/category/list"),
      apiGet("/api/front/article/banner/list")
    ]);
    articleCategories.value = categories.list || [];
    articleBanners.value = banners.list || [];
    await loadArticleItems();
  } catch (error) {
    showToast(error.message);
  } finally {
    articleLoading.value = false;
  }
}

async function loadArticleItems() {
  const data = activeArticleCid.value === "0"
    ? await apiGet("/api/front/article/hot/list")
    : await apiGet(`/api/front/article/list/${activeArticleCid.value}`, { page: 1, limit: 20 });
  articleItems.value = data.list || [];
}

async function selectArticleCategory(cid) {
  activeArticleCid.value = cid;
  articleLoading.value = true;
  try {
    await loadArticleItems();
  } catch (error) {
    showToast(error.message);
  } finally {
    articleLoading.value = false;
  }
}

async function openArticleDetail(article) {
  const id = article?.id || article;
  if (!id) {
    showToast("文章不存在");
    return;
  }
  currentTab.value = "articleDetail";
  articleDetailLoading.value = true;
  try {
    selectedArticle.value = await apiGet("/api/front/article/info", { id });
  } catch (error) {
    selectedArticle.value = null;
    showToast(error.message);
  } finally {
    articleDetailLoading.value = false;
  }
}

async function openCustomerService(returnTab = currentTab.value, overrideChatUrl = "", title = "联系客服") {
  customerReturnTab.value = returnTab || "profile";
  customerServiceTitle.value = title || "联系客服";
  currentTab.value = "customerService";
  customerServiceLoading.value = true;
  try {
    const config = await apiGet("/api/front/customer/service/config");
    customerServiceConfig.value = {
      ...config,
      chatUrl: overrideChatUrl || initialWebUrl || config.chatUrl || config.yzfUrl || "",
      yzfUrl: overrideChatUrl || config.yzfUrl || config.chatUrl || ""
    };
  } catch (error) {
    showToast(error.message);
  } finally {
    customerServiceLoading.value = false;
  }
}

function openCustomerChatWindow() {
  if (!customerChatUrl.value) {
    showToast("在线客服需开通后启用");
    return;
  }
  window.open(customerChatUrl.value, "_blank", "noopener");
}

async function loadProducts() {
  loading.value = true;
  categoryProductNotice.value = "";
  try {
    const layoutTab = activeCid.value || keyword.value ? null : activeLayoutTab();
    if (layoutTab) {
      if (layoutTab.url) {
        openHomeDiyLink(layoutTab);
      }
      const product = homeIndexProducts.value[layoutTab.sourceIndex];
      products.value = product ? mergeHomeProducts([product], homeDiyProducts.value) : homeIndexProducts.value;
      productTotal.value = products.value.length;
      return;
    }
    const params = {
      page: 1,
      limit: activeCid.value || keyword.value ? 24 : 40,
      keyword: keyword.value || undefined,
      cid: categoryQueryCid(activeCid.value) || undefined,
      salesOrder: legacyProductRank.value ? "desc" : undefined
    };
    const data =
      activeType.value > 0 && !activeCid.value && !legacyProductRank.value
        ? await apiGet(`/api/front/index/product/${activeType.value}`, params)
        : await apiGet("/api/front/products", params);
    products.value = activeCid.value || keyword.value
      ? displayProductList(data.list || [])
      : mergeHomeProducts(data.list || [], homeDiyProducts.value);
    productTotal.value = activeCid.value || keyword.value ? products.value.length : products.value.length;
  } finally {
    loading.value = false;
  }
}

function categoryQueryCid(id) {
  if (!id) {
    return "";
  }
  const found = findCategoryNode(id);
  if (!found) {
    return String(id);
  }
  if (found.type === "parent") {
    const ids = [found.category.id, ...(found.category.child || []).map((item) => item.id)];
    return ids.filter(Boolean).join(",");
  }
  return String(found.category.id);
}

function findCategoryNode(id) {
  const target = String(id);
  for (const parent of categories.value) {
    if (String(parent.id) === target) {
      return { type: "parent", category: parent, parent };
    }
    const child = (parent.child || []).find((item) => String(item.id) === target);
    if (child) {
      return { type: "child", category: child, parent };
    }
  }
  return null;
}

function sortCategoriesByProducts(list) {
  return [...list].sort((left, right) => {
    const rightCount = Number(categoryProductCounts.value[right.id] || 0);
    const leftCount = Number(categoryProductCounts.value[left.id] || 0);
    if (rightCount !== leftCount) {
      return rightCount - leftCount;
    }
    return Number(left.sort || 0) - Number(right.sort || 0);
  });
}

function normalizeCategoryTree(list) {
  return (Array.isArray(list) ? list : [])
    .map((category) => ({
      ...category,
      name: cleanDisplayText(category.name),
      child: (category.child || []).map((child) => ({
        ...child,
        name: cleanDisplayText(child.name)
      }))
    }))
    .filter(shouldShowCategory);
}

function shouldShowCategory(category) {
  const name = cleanDisplayText(category?.name);
  if (!name) return false;
  return !/(测试|test|demo|样例|临时)/i.test(name);
}

function displayProductList(list) {
  return (Array.isArray(list) ? list : []).map(normalizeDisplayProduct).filter(isDisplayReadyProduct);
}

function normalizeDisplayProduct(product) {
  if (!product) return product;
  return {
    ...product,
    storeName: cleanDisplayText(product.storeName || product.name || product.title),
    image: typeof product.image === "string" ? product.image.trim() : product.image,
    unitName: cleanDisplayText(product.unitName) || "件"
  };
}

function isDisplayReadyProduct(product) {
  if (!product) return false;
  const name = cleanDisplayText(product.storeName || product.name || product.title);
  const image = typeof product.image === "string" ? product.image.trim() : "";
  const price = product.price;
  if (!name || !image || price === "" || price === undefined || price === null) return false;
  if (Number(product.stock ?? 1) <= 0) return false;
  if (/(测试|test|demo|样例|临时|占位|placeholder|lorem)/i.test(name)) return false;
  if (isPoorDisplayImage(name, image)) return false;
  if (/^[a-z0-9_ -]{1,8}$/i.test(name) && !/[\u4e00-\u9fa5]/.test(name)) return false;
  if (name.length <= 1) return false;
  return true;
}

function isPoorDisplayImage(name, image) {
  const text = `${name} ${image}`.toLowerCase();
  return [
    "276-cookware-fast-1.webp",
    "276-cookware-fast-3.webp"
  ].some((keyword) => text.includes(keyword));
}

function cleanDisplayText(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

async function loadCategoryProductCounts() {
  const children = categories.value.flatMap((category) => category.child || []);
  const pairs = await Promise.all(children.map(async (category) => {
    try {
      const data = await apiGet("/api/front/products", { page: 1, limit: 1, cid: category.id });
      return [category.id, Number(data.total || 0)];
    } catch {
      return [category.id, 0];
    }
  }));
  categoryProductCounts.value = Object.fromEntries(pairs);
}

async function loadSearchHotKeywords() {
  try {
    const data = await apiGet("/api/front/search/keyword");
    searchHotKeywords.value = Array.isArray(data) ? data : [];
  } catch {
    searchHotKeywords.value = [];
  }
}

function applyLegacyGoodsFilters() {
  const path = window.location.pathname;
  const isGoodsList = path.includes("/pages/goods/goods_list/index") || path.includes("/pages/goods_list/index");
  const isGoodsSearch = path.includes("/pages/goods/goods_search/index") || path.includes("/pages/goods_search/index");
  const isGoodsCategory = path.includes("/pages/goods_cate/goods_cate");
  if (!isGoodsList && !isGoodsSearch && !isGoodsCategory) {
    return;
  }
  const legacyKeyword = initialParams.get("searchValue") || initialParams.get("keyword") || "";
  const legacyCid = initialParams.get("cid") || initialParams.get("cateId") || initialParams.get("categoryId") || "";
  const legacyTitle = initialParams.get("title") || initialParams.get("name") || "";
  keyword.value = legacyKeyword;
  activeCid.value = legacyCid;
  activeType.value = Number(initialParams.get("type") || 0);
  legacyGoodsTitle.value = legacyTitle || (legacyKeyword ? `搜索「${legacyKeyword}」` : "");
  if ((isGoodsList || isGoodsCategory) && legacyCid) {
    currentTab.value = "category";
  }
}

function legacyPromotionType(pathname, params) {
  if (!pathname.includes("/pages/activity/promotionList/index")) {
    return 0;
  }
  if (legacyPromotionRank(pathname, params)) {
    return 0;
  }
  const rawType = Number(params.get("type") || params.get("typeId") || 0);
  if ([1, 2, 3, 4].includes(rawType)) {
    return rawType;
  }
  const title = String(params.get("name") || params.get("title") || "");
  if (title.includes("精品")) return 1;
  if (title.includes("热门") || title.includes("排行")) return 2;
  if (title.includes("新品")) return 3;
  if (title.includes("促销") || title.includes("优惠")) return 4;
  return 1;
}

function legacyPromotionRank(pathname, params) {
  if (!pathname.includes("/pages/activity/promotionList/index")) {
    return false;
  }
  const title = String(params.get("name") || params.get("title") || "");
  return title.includes("商品排行") || title.includes("排行榜") || title.includes("榜单");
}

function productTypeFromMenuLabel(label) {
  if (label.includes("精品") || label.includes("推荐")) return 1;
  if (label.includes("热门") || label.includes("热销") || label.includes("排行") || label.includes("榜单")) return 2;
  if (label.includes("新品") || label.includes("新货") || label.includes("上新")) return 3;
  if (label.includes("促销") || label.includes("特价") || label.includes("优惠商品") || label.includes("折扣")) return 4;
  return 0;
}

async function openProductReplies(productId = initialProductId) {
  if (!productId) {
    showToast("缺少商品ID");
    return;
  }
  detailOpen.value = false;
  productReplyProductId.value = String(productId);
  productReplyType.value = 0;
  currentTab.value = "productReplies";
  await loadProductReplies();
}

async function loadProductReplies() {
  if (!productReplyProductId.value) {
    productReplies.value = [];
    productReplyConfig.value = {};
    return;
  }
  productReplyLoading.value = true;
  try {
    const [config, data] = await Promise.all([
      apiGet(`/api/front/reply/config/${productReplyProductId.value}`),
      apiGet(`/api/front/reply/list/${productReplyProductId.value}`, {
        type: productReplyType.value,
        page: 1,
        limit: 20
      })
    ]);
    productReplyConfig.value = config || {};
    productReplies.value = data.list || [];
  } catch (error) {
    showToast(error.message);
    productReplies.value = [];
  } finally {
    productReplyLoading.value = false;
  }
}

async function changeProductReplyType(type) {
  if (productReplyType.value === type) {
    return;
  }
  productReplyType.value = type;
  await loadProductReplies();
}

async function loadSeckill() {
  seckillLoading.value = true;
  try {
    const headers = await apiGet("/api/front/seckill/header");
    seckillHeaders.value = headers || [];
    const checked = seckillHeaders.value.find((item) => item.isCheck) || seckillHeaders.value[0];
    activeSeckillTimeId.value = checked?.id || "";
    if (activeSeckillTimeId.value) {
      await loadSeckillItems(activeSeckillTimeId.value);
    } else {
      seckillItems.value = [];
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    seckillLoading.value = false;
  }
}

async function loadCombination() {
  combinationLoading.value = true;
  try {
    const [header, data] = await Promise.all([
      apiGet("/api/front/combination/header").catch(() => ({})),
      apiGet("/api/front/combination/list", { page: 1, limit: 20 })
    ]);
    combinationHeader.value = header || {};
    combinationItems.value = data.list || data || [];
  } catch (error) {
    combinationItems.value = [];
    showToast(error.message || "拼团活动当前无法打开，请稍后重试");
  } finally {
    combinationLoading.value = false;
  }
}

async function loadSeckillItems(timeId) {
  if (!timeId) {
    seckillItems.value = [];
    return;
  }
  const data = await apiGet(`/api/front/seckill/list/${timeId}`, { page: 1, limit: 20 });
  seckillItems.value = data.list || [];
}

async function ensureSeckillHeaders(preferredTimeId = "") {
  if (!seckillHeaders.value.length) {
    const headers = await apiGet("/api/front/seckill/header");
    seckillHeaders.value = headers || [];
  }
  const preferred = seckillHeaders.value.find((item) => Number(item.id) === Number(preferredTimeId));
  const checked = seckillHeaders.value.find((item) => item.isCheck);
  activeSeckillTimeId.value = preferred?.id || activeSeckillTimeId.value || checked?.id || seckillHeaders.value[0]?.id || "";
}

async function selectSeckillTime(item) {
  if (!item || Number(item.id) === Number(activeSeckillTimeId.value)) {
    return;
  }
  activeSeckillTimeId.value = item.id;
  seckillLoading.value = true;
  try {
    await loadSeckillItems(item.id);
  } catch (error) {
    showToast(error.message);
  } finally {
    seckillLoading.value = false;
  }
}

async function loadBargain() {
  bargainLoading.value = true;
  try {
    const [header, data] = await Promise.all([
      apiGet("/api/front/bargain/header"),
      apiGet("/api/front/bargain/list", { page: 1, limit: 20 })
    ]);
    bargainHeader.value = header || {};
    bargainItems.value = data.list || [];
  } catch (error) {
    showToast(error.message);
  } finally {
    bargainLoading.value = false;
  }
}

async function loadCart() {
  if (!authToken.value) {
    return;
  }
  cartLoading.value = true;
  try {
    const data = await apiGet("/api/front/cart/list", { isValid: true, page: 1, limit: 50 }, true);
    cartItems.value = data.list || [];
    cartTotal.value = data.total || cartItems.value.length;
    const currentIds = new Set(cartItems.value.map((item) => Number(item.id)));
    selectedCartIds.value = selectedCartIds.value.filter((id) => currentIds.has(Number(id)));
    if (selectedCartIds.value.length === 0 && cartItems.value.length) {
      selectedCartIds.value = cartItems.value.map((item) => Number(item.id));
    }
  } catch (error) {
    handleAuthError(error);
  } finally {
    cartLoading.value = false;
  }
}

async function loadOrders() {
  if (!authToken.value) {
    return;
  }
  orderLoading.value = true;
  try {
    const [listData, countData] = await Promise.all([
      apiGet("/api/front/order/list", { type: orderType.value ?? undefined, page: 1, limit: 20 }, true),
      apiGet("/api/front/order/data", {}, true)
    ]);
    orders.value = listData.list || [];
    orderData.value = countData || {};
  } catch (error) {
    handleAuthError(error);
  } finally {
    orderLoading.value = false;
  }
}

async function loadRefundOrders() {
  if (!authToken.value) {
    return;
  }
  refundListLoading.value = true;
  try {
    const data = await apiGet(
      "/api/front/order/refund/list",
      { refundStatus: refundStatus.value ?? undefined, page: 1, limit: 20 },
      true
    );
    refundOrders.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    refundListLoading.value = false;
  }
}

async function loadAddresses() {
  if (!authToken.value) {
    return;
  }
  addressLoading.value = true;
  try {
    const data = await apiGet("/api/front/address/list", { page: 1, limit: 50 }, true);
    addresses.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    addressLoading.value = false;
  }
}

async function loadUserInfo() {
  if (!authToken.value) {
    return;
  }
  try {
    userInfo.value = await apiGet("/api/front/user", {}, true);
    profileForm.value = {
      nickname: userInfo.value?.nickname || "",
      avatar: userInfo.value?.avatar || ""
    };
  } catch (error) {
    handleAuthError(error);
  }
}

async function openUserInfo() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "userInfo";
  await loadUserInfo();
}

function openPhoneChange() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  phoneStep.value = userInfo.value?.phone ? 1 : 2;
  phoneForm.value = { phone: "", captcha: "" };
  currentTab.value = "userPhone";
}

function openPasswordChange() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  passwordForm.value = { password: "", confirmPassword: "", captcha: "" };
  currentTab.value = "userPassword";
}

function updateProfileField(key, value) {
  updateFormField(profileForm, key, value);
}

function updateLoginField(key, value) {
  updateFormField(loginForm, key, value);
}

function updatePhoneField(key, value) {
  updateFormField(phoneForm, key, value);
}

function updatePasswordField(key, value) {
  updateFormField(passwordForm, key, value);
}

async function saveUserProfile() {
  if (!profileForm.value.nickname) {
    showToast("请输入昵称");
    return;
  }
  savingProfile.value = true;
  try {
    userInfo.value = await apiPost("/api/front/user/edit", profileForm.value, true);
    showToast("保存成功");
  } catch (error) {
    handleAuthError(error);
  } finally {
    savingProfile.value = false;
  }
}

async function uploadAvatarImage(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) {
    return;
  }
  avatarUploading.value = true;
  try {
    const formData = new FormData();
    formData.append("multipart", file);
    formData.append("model", "user");
    formData.append("pid", String(userInfo.value?.uid || 0));
    const uploaded = await apiUpload("/api/front/user/upload/image", formData, true);
    const url = uploaded.url || uploaded.sattDir || uploaded.attDir || "";
    if (!url) {
      showToast("上传成功但未返回图片路径");
      return;
    }
    profileForm.value = { ...profileForm.value, avatar: url };
    showToast("头像已上传，保存后生效");
  } catch (error) {
    handleAuthError(error);
  } finally {
    avatarUploading.value = false;
  }
}

async function sendAccountCode(phone) {
  if (!phone) {
    showToast("请先填写手机号");
    return;
  }
  try {
    const message = await apiPost("/api/front/sendCode", { phone }, false);
    showToast(message || "验证码已发送");
  } catch (error) {
    showToast(error.message);
  }
}

async function savePhoneChange() {
  if (phoneStep.value === 1) {
    if (!phoneForm.value.captcha) {
      showToast("请填写验证码");
      return;
    }
    savingPhone.value = true;
    try {
      await apiPost("/api/front/update/binding/verify", {
        phone: userInfo.value?.phone,
        captcha: phoneForm.value.captcha
      }, true);
      phoneStep.value = 2;
      phoneForm.value = { phone: "", captcha: "" };
      showToast("验证成功");
    } catch (error) {
      handleAuthError(error);
    } finally {
      savingPhone.value = false;
    }
    return;
  }
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) {
    showToast("请输入正确的手机号码");
    return;
  }
  if (!phoneForm.value.captcha) {
    showToast("请填写验证码");
    return;
  }
  savingPhone.value = true;
  try {
    userInfo.value = await apiPost("/api/front/update/binding", phoneForm.value, true);
    showToast("绑定成功");
    currentTab.value = "userInfo";
  } catch (error) {
    handleAuthError(error);
  } finally {
    savingPhone.value = false;
  }
}

async function savePasswordChange() {
  if (passwordForm.value.password !== passwordForm.value.confirmPassword) {
    showToast("两次输入的密码不一致");
    return;
  }
  if (!/^[a-zA-Z]\w{5,17}$/.test(passwordForm.value.password)) {
    showToast("密码格式错误");
    return;
  }
  if (!passwordForm.value.captcha) {
    showToast("请填写验证码");
    return;
  }
  savingPassword.value = true;
  try {
    await apiPost("/api/front/register/reset", {
      account: userInfo.value?.phone || userInfo.value?.account,
      password: passwordForm.value.password,
      captcha: passwordForm.value.captcha
    }, true);
    passwordForm.value = { password: "", confirmPassword: "", captcha: "" };
    showToast("修改成功，请重新登录");
    logout();
  } catch (error) {
    handleAuthError(error);
  } finally {
    savingPassword.value = false;
  }
}

async function openOrderDetail(order, fromRefund = false) {
  if (!order?.orderId) {
    showToast("缺少订单号无法查看订单详情");
    return;
  }
  selectedOrder.value = order;
  orderDetailFromRefund.value = fromRefund;
  currentTab.value = "orderDetail";
  orderDetailLoading.value = true;
  try {
    selectedOrder.value = await apiGet(`/api/front/order/detail/${order.orderId}`, {}, true);
  } catch (error) {
    showToast(error.message);
    currentTab.value = "profile";
  } finally {
    orderDetailLoading.value = false;
  }
}

async function openPaymentPage(orderNo) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!orderNo) {
    showToast("缺少订单号无法付款");
    return;
  }
  currentTab.value = "paymentPage";
  paymentPageLoading.value = true;
  try {
    const [orderInfo, config] = await Promise.all([
      apiGet(`/api/front/order/detail/${orderNo}`, {}, true),
      apiGet("/api/front/order/get/pay/config", {}, true)
    ]);
    paymentPageOrder.value = orderInfo;
    payConfig.value = config || {};
  } catch (error) {
    showToast(error.message);
    currentTab.value = "profile";
  } finally {
    paymentPageLoading.value = false;
  }
}

async function loadPayStatus(orderNo = initialPaymentOrderNo) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!orderNo) {
    payStatusInfo.value = null;
    return;
  }
  currentTab.value = "payStatus";
  payStatusLoading.value = true;
  try {
    const [payResult, orderDetail] = await Promise.allSettled([
      apiGet("/api/front/pay/queryPayResult", { orderNo }, true),
      apiGet(`/api/front/order/detail/${orderNo}`, {}, true)
    ]);
    if (payResult.status === "rejected" && orderDetail.status === "rejected") {
      throw payResult.reason;
    }
    const payInfo = payResult.status === "fulfilled" ? payResult.value : {};
    const orderInfo = orderDetail.status === "fulfilled" ? orderDetail.value : {};
    payStatusInfo.value = {
      ...payInfo,
      ...orderInfo,
      payStatus: payInfo.payStatus || orderInfo.payStatus,
      payResult: payInfo.payResult ?? (Number(orderInfo.paid || 0) > 0),
      dryRun: payInfo.dryRun,
      paid: Number((payInfo.paid ?? orderInfo.paid) || 0) > 0 ? 1 : 0,
      message: initialPayStatus === "2" ? "取消支付" : (payInfo.message || orderInfo.statusText)
    };
  } catch (error) {
    try {
      const order = await apiGet(`/api/front/order/detail/${orderNo}`, {}, true);
      payStatusInfo.value = {
        ...order,
        paid: Number(order.paid || 0) > 0 ? 1 : 0,
        message: initialPayStatus === "2" ? "取消支付" : order.statusText
      };
    } catch (detailError) {
      showToast(detailError.message || error.message);
      payStatusInfo.value = null;
    }
  } finally {
    payStatusLoading.value = false;
  }
}

async function openOrderFromPayStatus() {
  const orderNo = payStatusInfo.value?.orderId || payStatusInfo.value?.orderNo;
  if (!orderNo) {
    showToast("缺少订单号无法查看订单详情");
    return;
  }
  await openOrderDetail({ orderId: orderNo });
}

async function openPinkFromPayStatus() {
  const link = pinkShareLink.value;
  if (!link) {
    showToast("未找到拼团邀请链接");
    await openCombinationFromPayStatus();
    return;
  }
  try {
    await navigator.clipboard.writeText(link);
    pinkManualCopyLink.value = "";
    showToast("参团链接已复制，快分享给好友吧");
  } catch {
    pinkManualCopyLink.value = link;
    showToast("已显示参团链接，请长按或全选复制");
  }
}

async function openCombinationFromPayStatus() {
  if (payStatusInfo.value?.combinationId) {
    await openCombinationDetail({ id: payStatusInfo.value.combinationId });
    return;
  }
  switchTab("combination");
}

async function openActivityFromPayStatus() {
  const info = payStatusInfo.value || {};
  if (Number(info.seckillId || 0) > 0) {
    await openSeckillDetail({ id: info.seckillId });
    return;
  }
  if (Number(info.bargainId || 0) > 0) {
    await openBargainDetail({ id: info.bargainId, bargainUserId: info.bargainUserId });
    return;
  }
  if (Number(info.combinationId || 0) > 0) {
    await openCombinationDetail({ id: info.combinationId, pinkId: info.pinkId });
    return;
  }
  switchTab("home");
}

function openRefundDetail(order) {
  openOrderDetail(order, true);
}

function returnFromOrderDetail() {
  currentTab.value = orderDetailFromRefund.value ? "refundList" : "profile";
}

function parseDetailAttrValues(raw) {
  if (Array.isArray(raw)) {
    return raw.map((item) => String(item)).filter(Boolean);
  }
  if (raw == null) {
    return [];
  }
  const text = String(raw).trim();
  if (!text) {
    return [];
  }
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item)).filter(Boolean);
    }
  } catch {
    // Old CRMEB data also stores comma separated attr values.
  }
  return text
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function detailSkuValues(sku) {
  if (!sku) {
    return [];
  }
  const attrValue = sku.attrValue;
  if (attrValue) {
    try {
      const parsed = JSON.parse(attrValue);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item)).filter(Boolean);
      }
      if (parsed && typeof parsed === "object") {
        return Object.values(parsed).map((item) => String(item)).filter(Boolean);
      }
    } catch {
      // Fall back to `suk`, which is the old SKU display string.
    }
  }
  return String(sku.suk || attrValue || "")
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function skuMatches(sku, selectedValues) {
  const values = detailSkuValues(sku);
  if (values.length === selectedValues.length && selectedValues.every((value, index) => values[index] === value)) {
    return true;
  }
  return selectedValues.every((value) => values.includes(value));
}

function resetDetailSkuSelection(data) {
  const attrs = data?.productAttr || [];
  const sku = Object.values(data?.productValue || {})[0] || null;
  const skuValues = detailSkuValues(sku);
  const next = {};
  attrs.forEach((attr, index) => {
    const options = parseDetailAttrValues(attr.attrValues);
    next[attr.attrName] = skuValues[index] || options[0] || "";
  });
  selectedDetailAttrs.value = next;
  detailCartNum.value = 1;
}

function selectDetailAttr(payload) {
  selectedDetailAttrs.value = {
    ...selectedDetailAttrs.value,
    [payload.attrName]: payload.value
  };
  const stock = Number(selectedDetailSku.value?.stock ?? detail.value?.productInfo?.stock ?? 0);
  if (stock > 0 && detailCartNum.value > stock) {
    detailCartNum.value = stock;
  }
}

function changeDetailCartNum(value) {
  const stock = Math.max(0, Number(selectedDetailSku.value?.stock ?? detail.value?.productInfo?.stock ?? 0));
  if (stock <= 0) {
    detailCartNum.value = 1;
    return;
  }
  detailCartNum.value = Math.min(stock, Math.max(1, Number(value) || 1));
}

async function openDetail(product) {
  detailOpen.value = true;
  detailLoading.value = true;
  detail.value = null;
  selectedDetailAttrs.value = {};
  detailCartNum.value = 1;
  try {
    const data = await apiGet(`/api/front/product/detail/${product.id}`, {}, Boolean(authToken.value));
    detail.value = data;
    resetDetailSkuSelection(data);
    if (authToken.value) {
      await loadCart();
    }
  } finally {
    detailLoading.value = false;
  }
}

async function openSeckillDetail(item) {
  currentTab.value = "seckillDetail";
  seckillDetailLoading.value = true;
  selectedSeckill.value = null;
  try {
    selectedSeckill.value = await apiGet(`/api/front/seckill/detail/${item.id}`);
    await ensureSeckillHeaders(selectedSeckill.value?.timeId);
  } catch (error) {
    showToast(error.message);
    currentTab.value = "seckill";
    await loadSeckill();
  } finally {
    seckillDetailLoading.value = false;
  }
}

async function openSeckillMasterProduct() {
  if (!selectedSeckill.value?.productId) {
    showToast("未找到原商品");
    return;
  }
  await openDetail({ id: selectedSeckill.value.productId });
}

async function startSeckillCheckout() {
  if (seckillDetailStatus.value !== "抢购中") {
    showToast(seckillDetailStatus.value);
    return;
  }
  await startActivityCheckout("seckill", selectedSeckill.value);
}

function seckillListButtonText(item) {
  if (Number(item?.quota || item?.stock || 0) <= 0) {
    return "已售罄";
  }
  const now = Date.now();
  const start = timestampMs(item?.startTime);
  const stop = timestampMs(item?.stopTime);
  if ((start && now < start) || currentSeckillStatus.value === 1) {
    return "未开始";
  }
  if ((stop && now >= stop) || currentSeckillStatus.value === 3) {
    return "已结束";
  }
  return currentSeckillStatus.value === 2 || !currentSeckillStatus.value ? "马上抢" : "已结束";
}

async function openBargainDetail(item) {
  currentTab.value = "bargainDetail";
  bargainDetailLoading.value = true;
  selectedBargain.value = null;
  bargainUserInfo.value = null;
  try {
    selectedBargain.value = await apiGet(`/api/front/bargain/detail/${item.id}`);
    if (authToken.value) {
      await loadBargainUserInfo(item.bargainUserId);
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = "bargain";
  } finally {
    bargainDetailLoading.value = false;
  }
}

async function openCombinationDetail(item) {
  currentTab.value = "combinationDetail";
  combinationDetailLoading.value = true;
  selectedCombination.value = null;
  combinationInvitePinkId.value = item.pinkId || "";
  try {
    const detail = await apiGet(`/api/front/combination/detail/${item.id}`);
    selectedCombination.value = detail;
    if (item.pinkId) {
      const pinkData = await apiGet(`/api/front/combination/pink/${item.pinkId}`);
      selectedCombination.value = mergeCombinationPinkDetail(detail, pinkData);
    }
  } catch (error) {
    showToast(error.message || "拼团详情当前无法打开，请稍后重试");
    currentTab.value = "combination";
  } finally {
    combinationDetailLoading.value = false;
  }
}

async function openCombinationPinkDetail(pinkId) {
  currentTab.value = "combinationDetail";
  combinationDetailLoading.value = true;
  selectedCombination.value = null;
  combinationInvitePinkId.value = pinkId || "";
  try {
    const pinkData = await apiGet(`/api/front/combination/pink/${pinkId}`);
    const combination = pinkData?.storeCombination || {};
    const combinationId = combination.id || combination.combinationId || combination.cid;
    const detail = combinationId ? await apiGet(`/api/front/combination/detail/${combinationId}`) : combination;
    selectedCombination.value = mergeCombinationPinkDetail(detail, pinkData);
    return selectedCombination.value;
  } catch (error) {
    showToast(error.message || "拼团状态当前无法打开，请稍后重试");
    currentTab.value = "combination";
    return null;
  } finally {
    combinationDetailLoading.value = false;
  }
}

function mergeCombinationPinkDetail(detail, pinkData) {
  const pinkHead = pinkData?.pinkT || null;
  const pinkMembers = [pinkHead, ...(pinkData?.pinkAll || [])].filter(Boolean);
  const activePinkList = [
    pinkHead,
    ...(detail?.pinkList || detail?.pinkingList || [])
  ].filter(Boolean);
  const seen = new Set();
  const uniquePinkList = activePinkList.filter((item) => {
    const key = String(item.id || item.orderId || `${item.uid}-${item.kId}`);
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
  return {
    ...(detail || {}),
    pinkStatus: pinkData,
    pinkList: uniquePinkList,
    pinkingList: uniquePinkList,
    pinkMemberList: pinkMembers,
    memberList: pinkMembers,
    pinkUserList: pinkMembers,
    inviteCount: pinkData?.count,
    userInPink: pinkData?.userBool,
    pinkIsOk: pinkData?.isOk
  };
}

function combinationShareUrl(pinkId, combinationId = "", fromOrder = "") {
  if (!pinkId) {
    return "";
  }
  const url = new URL(window.location.origin + window.location.pathname);
  url.searchParams.set("tab", "combinationDetail");
  if (combinationId) {
    url.searchParams.set("id", combinationId);
  }
  url.searchParams.set("pinkId", pinkId);
  if (fromOrder) {
    url.searchParams.set("fromOrder", fromOrder);
  }
  return url.toString();
}

async function openCombinationMasterProduct() {
  if (!selectedCombination.value?.productId) {
    showToast("未找到原商品");
    return;
  }
  await openDetail({ id: selectedCombination.value.productId });
}

async function startCombinationCheckout(pinkId = 0) {
  if (combinationDetailStatus.value !== "拼团中") {
    showToast(combinationDetailStatus.value);
    return;
  }
  await startActivityCheckout("combination", selectedCombination.value, { pinkId });
}

function combinationListButtonText(item) {
  if (Number(item?.quota || item?.stock || 0) <= 0) {
    return "已售罄";
  }
  const start = timestampMs(item?.startTime);
  if (start && Date.now() < start) {
    return "未开始";
  }
  return combinationItemEnded(item) ? "已结束" : "去拼团";
}

function canJoinPink(pink) {
  if (combinationDetailStatus.value !== "拼团中") {
    return false;
  }
  if (Number(pink?.status || 1) !== 1) {
    return false;
  }
  const stop = timestampMs(pink?.stopTime);
  return !stop || stop > Date.now();
}

async function loadBargainUserInfo(bargainUserId) {
  if (!authToken.value || !selectedBargain.value?.id) {
    bargainUserInfo.value = null;
    return;
  }
  try {
    bargainUserInfo.value = await apiGet(
      "/api/front/bargain/user",
      {
        bargainId: selectedBargain.value.id,
        bargainUserId: bargainUserId || bargainUserInfo.value?.storeBargainUserId || undefined
      },
      true
    );
  } catch (error) {
    handleAuthError(error);
  }
}

async function openBargainMasterProduct() {
  if (!selectedBargain.value?.productId) {
    showToast("未找到原商品");
    return;
  }
  await openDetail({ id: selectedBargain.value.productId });
}

async function handleBargainAction() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const status = Number(bargainUserInfo.value?.bargainStatus || 0);
  if (status === 8) {
    switchTab("bargainRecords");
    return;
  }
  if (status === 6 || status === 7 || status === 10) {
    bargainUserInfo.value = null;
    await loadBargainUserInfo(null);
    showToast("已切换到自己的砍价状态");
    return;
  }
  if (status === 9) {
    showToast("该砍价订单已支付");
    return;
  }
  if (status === 4) {
    await startActivityCheckout("bargain", selectedBargain.value, {
      bargainUserId: bargainUserInfo.value?.storeBargainUserId
    });
    return;
  }
  if (status === 1) {
    await startBargain();
    return;
  }
  if (status === 5) {
    await helpBargain();
    return;
  }
  if (status === 3) {
    await shareBargainLink();
    return;
  }
  showToast("当前砍价状态暂不能操作");
}

async function shareBargainLink() {
  const link = bargainShareLink.value;
  if (!link) {
    showToast("未找到砍价分享链接");
    return;
  }
  try {
    await navigator.clipboard.writeText(link);
    bargainManualCopyLink.value = "";
    showToast("砍价链接已复制，快分享给好友吧");
  } catch {
    bargainManualCopyLink.value = link;
    showToast("已显示砍价链接，请长按或全选复制");
  }
}

async function startBargain() {
  if (!selectedBargain.value?.id) {
    return;
  }
  try {
    const data = await apiPost("/api/front/bargain/start", { bargainId: selectedBargain.value.id }, true);
    bargainUserInfo.value = {
      ...(bargainUserInfo.value || {}),
      storeBargainUserId: data.storeBargainUserId,
      storeBargainUserUid: userInfo.value?.uid
    };
    await helpBargain();
  } catch (error) {
    showToast(error.message);
    await loadBargainUserInfo(null);
  }
}

async function helpBargain() {
  const bargainUserId = bargainUserInfo.value?.storeBargainUserId;
  if (!selectedBargain.value?.id || !bargainUserId) {
    showToast("未找到砍价活动");
    return;
  }
  try {
    const data = await apiPost(
      "/api/front/bargain/help",
      {
        bargainId: selectedBargain.value.id,
        bargainUserId,
        bargainUserUid: bargainUserInfo.value?.storeBargainUserUid || userInfo.value?.uid
      },
      true
    );
    showToast(`已砍掉￥${moneyText(data.bargainPrice)}`);
    await loadBargainUserInfo(bargainUserId);
  } catch (error) {
    showToast(error.message);
    await loadBargainUserInfo(bargainUserId);
  }
}

function bargainMasterStatusText(status) {
  const map = {
    normal: "正常",
    soldOut: "已下架",
    sellOut: "已售罄",
    delete: "已删除"
  };
  return map[status] || "未知";
}

async function loadBargainRecords() {
  if (!authToken.value) {
    bargainRecordItems.value = [];
    return;
  }
  bargainRecordLoading.value = true;
  try {
    const data = await apiGet("/api/front/bargain/record", { page: 1, limit: 50 }, true);
    bargainRecordItems.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    bargainRecordLoading.value = false;
  }
}

async function openActivityOrders(type) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  activityOrderType.value = type;
  currentTab.value = "activityOrders";
  await loadActivityOrders();
}

async function loadActivityOrders() {
  if (!authToken.value) {
    activityOrderItems.value = [];
    return;
  }
  activityOrderLoading.value = true;
  try {
    const data = await apiGet("/api/front/order/list", { page: 1, limit: 100 }, true);
    activityOrderItems.value = (data.list || []).filter((order) => activityOrderMatches(order, activityOrderType.value));
  } catch (error) {
    handleAuthError(error);
  } finally {
    activityOrderLoading.value = false;
  }
}

async function openBargainRecordDetail(item) {
  await openBargainDetail({ id: item.id || item.bargainId, bargainUserId: item.bargainUserId || item.storeBargainUserId });
}

async function payBargainRecord(item) {
  if (!canPayBargainRecord(item)) {
    await openBargainRecordDetail(item);
    return;
  }
  await payOrder({
    orderId: item.orderNo,
    payPrice: item.surplusPrice,
    id: item.orderNo
  });
}

async function startActivityCheckout(type, activity, extra = {}) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!activity?.id || !activity?.productId) {
    showToast("活动商品数据不完整");
    return;
  }
  const activityIdKey = `${type}Id`;
  const attrValueId = activity.attrValueId || activity.productAttrUnique || activity.unique || firstActivityAttrValueId(activity);
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiPost(
      "/api/front/order/pre/order",
      {
        preOrderType: "buyNow",
        orderDetails: [
          {
            attrValueId: attrValueId ? Number(attrValueId) : 0,
            [activityIdKey]: Number(activity.id),
            productNum: Number(activity.cartNum || activity.onceNum || 1),
            productId: Number(activity.productId),
            ...extra
          }
        ]
      },
      true
    );
    preOrderNo.value = preData.preOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
  } catch (error) {
    showToast(error.message || "活动下单当前无法处理，请稍后重试");
    currentTab.value = type === "seckill" ? "seckillDetail" : type === "combination" ? "combinationDetail" : "bargainDetail";
  } finally {
    checkoutLoading.value = false;
  }
}

function openBanner(item) {
  const url = legacyMenuUrl(item);
  if (openLegacyOrderUrl(url) || openLegacyCoveredUrl(url, currentTab.value)) {
    return;
  }
  if (openExternalUrl(url)) {
    return;
  }
  const id = item.id || String(url || "").match(/id=(\d+)/)?.[1];
  if (id) {
    openDetail({ id });
  }
}

function openHomeDiyLink(payload) {
  const item = typeof payload === "object" && payload !== null ? payload : { link: payload };
  const url = legacyMenuUrl(item);
  const title = String(item.title || item.name || item.componentTitle || "");
  if (openHomeDiyPlaceholderLink(url)) {
    return;
  }
  if (openLegacyOrderUrl(url) || openLegacyCoveredUrl(url, currentTab.value) || openExternalUrl(url)) {
    return;
  }
  if (routeKnownMenu(title || homeDiyTypeTitle(item.componentType), url, currentTab.value)) {
    return;
  }
  if (openHomeDiyLooseLink(item, url)) {
    return;
  }
  if (!url) {
    openHomeDiyFallback(item, title);
    return;
  }
  const id = url.match(/(?:id|productId)=(\d+)/)?.[1] || numericUrlValue(url);
  id ? openDetail({ id }) : openHomeDiyFallback(item, title);
}

function openHomeDiyPlaceholderLink(url) {
  if (isPlaceholderDiyUrl(url)) {
    return true;
  }
  const id = numericUrlValue(url);
  if (id) {
    openDetail({ id });
    return true;
  }
  return false;
}

function openHomeDiyLooseLink(item, url) {
  const type = String(item?.componentType || item?.type || "");
  const normalized = normalizeLegacyUrl(url);
  const parsed = parseLegacyUrl(normalized);
  const id = legacyUrlValue(parsed, "id", "productId", "articleId", "seckillId", "combinationId", "bargainId") || numericIdFromDiyItem(item);
  const cid = legacyUrlValue(parsed, "cid", "cateId", "categoryId") || numericValue(item?.cid || item?.cateId || item?.categoryId);
  const title = String(item?.title || item?.name || item?.componentTitle || "");

  if (
    type === "home_coupon"
    || normalized.includes("coupon")
    || title.includes("优惠券")
    || title.includes("领券")
  ) {
    couponCenterType.value = 1;
    switchTab("couponCenter");
    return true;
  }

  if (cid) {
    activeCid.value = String(cid);
    keyword.value = "";
    activeType.value = 0;
    switchTab("category");
    return true;
  }
  if (!id) {
    return false;
  }
  if (type === "home_article" || normalized.includes("news_details") || normalized.includes("article")) {
    openArticleDetail({ id });
    return true;
  }
  if (type === "home_seckill" || normalized.includes("goods_seckill")) {
    openSeckillDetail({ id });
    return true;
  }
  if (type === "home_group" || normalized.includes("goods_combination")) {
    openCombinationDetail({ id });
    return true;
  }
  if (type === "home_bargain" || normalized.includes("goods_bargain")) {
    openBargainDetail({ id });
    return true;
  }
  if (normalized.includes("small_page")) {
    currentTab.value = "smallPage";
    loadSmallPage(id);
    return true;
  }
  if (title.includes("文章") || title.includes("资讯") || title.includes("公告")) {
    openArticleDetail({ id });
    return true;
  }
  openDetail({ id });
  return true;
}

function numericIdFromDiyItem(item) {
  return numericValue(
    item?.productId
    || item?.product_id
    || item?.goodsId
    || item?.goods_id
    || item?.articleId
    || item?.article_id
    || item?.seckillId
    || item?.seckill_id
    || item?.combinationId
    || item?.combination_id
    || item?.bargainId
    || item?.bargain_id
    || item?.activityId
    || item?.activity_id
    || item?.id
  );
}

function numericValue(value) {
  const text = String(value || "").trim();
  return /^\d+$/.test(text) ? text : "";
}

function numericUrlValue(url) {
  const text = String(url || "").trim();
  return /^[1-9]\d*$/.test(text) ? text : "";
}

function isPlaceholderDiyUrl(url) {
  const text = String(url || "").trim().toLowerCase();
  return text === "#" || text === "0" || text === "javascript:;" || text === "javascript:void(0)" || text === "javascript:void(0);";
}

function homeDiyTypeTitle(type) {
  return {
    home_menu: "导航",
    nav_bar: "商品分类",
    home_tab: "商品列表",
    home_news_roll: "资讯",
    home_goods_list: "商品列表",
    home_coupon: "领券中心",
    home_merchant: "推荐商户",
    home_seckill: "秒杀",
    home_group: "拼团",
    home_bargain: "砍价",
    home_article: "文章资讯",
    home_footer: "首页"
  }[type] || "";
}

function openHomeDiyFallback(item, title = "") {
  const typeTitle = homeDiyTypeTitle(item?.componentType || item?.type);
  const label = title || typeTitle || "";
  if (label && routeKnownMenu(label, "", currentTab.value)) {
    return true;
  }
  if (openDiyImageLanding(item, label)) {
    return true;
  }
  openProductLanding("热卖推荐", 2);
  return true;
}

function openDiyImageLanding(item, label = "") {
  const rawText = String(label || item?.title || item?.name || item?.componentTitle || "").trim();
  const text = isDiyPlaceholderTitle(rawText) ? "" : rawText;
  const componentType = String(item?.componentType || item?.type || "");
  const productType = productTypeFromMenuLabel(text);
  if (productType > 0) {
    openProductLanding(text || "商品推荐", productType, text.includes("排行") || text.includes("榜"));
    return true;
  }
  if (componentType === "home_seckill" || text.includes("秒杀")) {
    switchTab("seckill");
    return true;
  }
  if (componentType === "home_group" || text.includes("拼团")) {
    switchTab("combination");
    return true;
  }
  if (componentType === "home_bargain" || text.includes("砍价")) {
    switchTab("bargain");
    return true;
  }
  if (componentType === "home_coupon" || text.includes("优惠券") || text.includes("领券")) {
    switchTab("couponCenter");
    return true;
  }
  if (text.includes("资讯") || text.includes("文章")) {
    switchTab("articles");
    return true;
  }
  if (text.includes("分类")) {
    switchTab("category");
    return true;
  }
  openProductLanding(text || "热卖推荐", 2);
  return true;
}

function openProductLanding(title = "热卖推荐", type = 2, rank = false) {
  activeCid.value = "";
  keyword.value = "";
  activeType.value = Number(type || 0);
  legacyProductRank.value = Boolean(rank);
  legacyGoodsTitle.value = title;
  switchTab("home");
  scrollToProductSection();
}

function scrollToProductSection() {
  window.setTimeout(() => {
    document.querySelector(".product-section")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, 120);
}

function isDiyPlaceholderTitle(value) {
  const text = String(value || "").trim();
  return !text
    || /^项目\d*$/i.test(text)
    || /^图片\d*$/i.test(text)
    || /^图文\d*$/i.test(text)
    || /^banner\d*$/i.test(text)
    || /^轮播\d*$/i.test(text)
    || /^标题\d*$/i.test(text);
}

async function addDetailToCart() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    detailOpen.value = false;
    return null;
  }
  const productId = detail.value?.productInfo?.id;
  const attrValueId = selectedDetailSku.value?.id;
  if (!productId) {
    showToast("商品数据已失效");
    return null;
  }
  if (!attrValueId) {
    showToast("请选择完整规格");
    return null;
  }
  if (detailPurchaseDisabledReason.value) {
    showToast(detailPurchaseDisabledReason.value);
    return null;
  }
  try {
    const data = await apiPost(
      "/api/front/cart/save",
      {
        productId,
        uniqueId: attrValueId,
        cartNum: detailCartNum.value,
        type: "product"
      },
      true
    );
    showToast("已加入购物车");
    await loadCart();
    return data;
  } catch (error) {
    showToast(error.message);
    return null;
  }
}

async function buyNow() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    detailOpen.value = false;
    return;
  }
  const productId = detail.value?.productInfo?.id;
  const attrValueId = selectedDetailSku.value?.id;
  if (!productId) {
    showToast("商品数据已失效");
    return;
  }
  if (!attrValueId) {
    showToast("请选择完整规格");
    return;
  }
  if (detailPurchaseDisabledReason.value) {
    showToast(detailPurchaseDisabledReason.value);
    return;
  }
  const previousTab = currentTab.value;
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiPost(
      "/api/front/order/pre/order",
      {
        preOrderType: "buyNow",
        orderDetails: [
          {
            productId: Number(productId),
            attrValueId: Number(attrValueId),
            productNum: detailCartNum.value
          }
        ]
      },
      true
    );
    preOrderNo.value = preData.preOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
    detailOpen.value = false;
  } catch (error) {
    showToast(error.message || "立即购买当前无法处理，请稍后重试");
    currentTab.value = previousTab;
  } finally {
    checkoutLoading.value = false;
  }
}

async function toggleDetailCollect() {
  if (detailCollecting.value) {
    return;
  }
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    detailOpen.value = false;
    return;
  }
  const productId = detail.value?.productInfo?.id;
  if (!productId) {
    showToast("商品数据已失效");
    return;
  }
  detailCollecting.value = true;
  try {
    if (detail.value.userCollect) {
      await apiPost(`/api/front/collect/cancel/${productId}`, {}, true);
      detail.value.userCollect = false;
      showToast("取消收藏成功");
    } else {
      await apiPost("/api/front/collect/add", { id: productId, category: "product" }, true);
      detail.value.userCollect = true;
      showToast("收藏成功");
    }
    if (currentTab.value === "collection") {
      await loadCollects();
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    detailCollecting.value = false;
  }
}

async function changeCartNum(item, delta) {
  const nextNum = Math.max(1, Number(item.cartNum) + delta);
  try {
    await apiPost("/api/front/cart/num", { id: item.id, number: nextNum }, true);
    await loadCart();
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteCart(id) {
  try {
    await apiPost("/api/front/cart/delete", { ids: [id] }, true);
    selectedCartIds.value = selectedCartIds.value.filter((item) => Number(item) !== Number(id));
    await loadCart();
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteSelectedCart() {
  if (!selectedCartIds.value.length) {
    showToast("请选择产品");
    return;
  }
  try {
    await apiPost("/api/front/cart/delete", { ids: selectedCartIds.value }, true);
    showToast("删除成功");
    selectedCartIds.value = [];
    await loadCart();
  } catch (error) {
    showToast(error.message);
  }
}

async function collectSelectedCart() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!selectedCartIds.value.length) {
    showToast("请选择产品");
    return;
  }
  const selectedProductIds = selectedCartItems.value.map((item) => Number(item.productId)).filter(Boolean);
  if (!selectedProductIds.length) {
    showToast("请选择产品");
    return;
  }
  try {
    await apiPost("/api/front/collect/all", { id: selectedProductIds, category: "product" }, true);
    showToast("收藏成功");
  } catch (error) {
    showToast(error.message);
  }
}

async function startCheckout(ids) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const cartIds = Array.isArray(ids) && ids.length ? ids : selectedCartIds.value;
  if (!cartIds.length) {
    showToast("请选择产品");
    return;
  }
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiPost(
      "/api/front/order/pre/order",
      { preOrderType: "shoppingCart", cartIds },
      true
    );
    preOrderNo.value = preData.preOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = "cart";
  } finally {
    checkoutLoading.value = false;
  }
}

async function startAgainCheckout(order) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!order?.orderId) {
    showToast("订单数据已失效");
    return;
  }
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiPost(
      "/api/front/order/pre/order",
      { preOrderType: "again", orderDetails: [{ orderNo: order.orderId }] },
      true
    );
    preOrderNo.value = preData.preOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = selectedOrder.value ? "orderDetail" : "profile";
  } finally {
    checkoutLoading.value = false;
  }
}

async function openLegacyCheckout() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const cartId = initialParams.get("cartId") || initialParams.get("cart_id");
  const legacyPreOrderNo = initialParams.get("preOrderNo") || initialParams.get("pre_order_no");
  if (cartId) {
    await startCheckout(String(cartId).split(",").map((item) => Number(item)).filter(Boolean));
    return;
  }
  if (!legacyPreOrderNo) {
    showToast("结算数据已失效，请重新选择商品");
    currentTab.value = "cart";
    await loadCart();
    return;
  }
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiGet(`/api/front/order/load/pre/${legacyPreOrderNo}`, {}, true);
    preOrderNo.value = legacyPreOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = "cart";
  } finally {
    checkoutLoading.value = false;
  }
}

async function openLegacyCheckoutFromParsed(parsed) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const cartId = legacyUrlValue(parsed, "cartId", "cart_id");
  const legacyPreOrderNo = legacyUrlValue(parsed, "preOrderNo", "pre_order_no");
  if (cartId) {
    await startCheckout(String(cartId).split(",").map((item) => Number(item)).filter(Boolean));
    return;
  }
  if (!legacyPreOrderNo) {
    showToast("结算数据已失效，请重新选择商品");
    currentTab.value = "cart";
    await loadCart();
    return;
  }
  checkoutLoading.value = true;
  checkoutItems.value = [];
  checkoutAddress.value = {};
  checkoutPrice.value = {};
  preOrderNo.value = "";
  checkoutMark.value = "";
  checkoutUseIntegral.value = false;
  checkoutShippingType.value = 0;
  storeSelfMention.value = false;
  storeList.value = [];
  selectedStore.value = null;
  pickupContact.value = "";
  pickupPhone.value = "";
  couponList.value = [];
  selectedCoupon.value = null;
  currentTab.value = "checkout";
  try {
    const preData = await apiGet(`/api/front/order/load/pre/${legacyPreOrderNo}`, {}, true);
    preOrderNo.value = legacyPreOrderNo;
    checkoutItems.value = preData.orderInfoVo?.cartInfo || preData.cartInfo || [];
    checkoutAddress.value = preData.addressInfo || {};
    storeSelfMention.value = String(preData.storeSelfMention) === "1" || preData.storeSelfMention === true;
    await recomputeCheckoutPrice();
    if (!checkoutPrice.value?.payFee) {
      checkoutPrice.value = preData.priceGroup || preData.orderInfoVo?.priceGroup || {};
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = "cart";
  } finally {
    checkoutLoading.value = false;
  }
}

async function openCouponPanel() {
  if (!preOrderNo.value) {
    showToast("订单数据已失效，请重新结算");
    return;
  }
  couponOpen.value = true;
  if (couponList.value.length) {
    return;
  }
  couponLoading.value = true;
  try {
    couponList.value = await apiGet(`/api/front/coupons/order/${preOrderNo.value}`, {}, true);
  } catch (error) {
    showToast(error.message);
  } finally {
    couponLoading.value = false;
  }
}

function closeCouponPanel() {
  couponOpen.value = false;
}

async function selectCoupon(coupon) {
  selectedCoupon.value = selectedCoupon.value?.id === coupon.id ? null : coupon;
  couponList.value = couponList.value.map((item) => ({
    ...item,
    isUse: selectedCoupon.value?.id === item.id ? 1 : 0,
    use_title: selectedCoupon.value?.id === item.id ? "不使用" : ""
  }));
  couponOpen.value = false;
  await recomputeCheckoutPrice();
}

async function selectShippingType(type) {
  if (type === 1 && !storeSelfMention.value) {
    showToast("门店自提未开启");
    return;
  }
  checkoutShippingType.value = type;
  if (type === 1) {
    await loadStoreList();
  }
  await recomputeCheckoutPrice();
}

async function loadStoreList() {
  if (storeList.value.length || storeLoading.value) {
    return;
  }
  storeLoading.value = true;
  try {
    const data = await apiPost("/api/front/store/list", { page: 1, limit: 20 });
    storeList.value = data.list || [];
    if (!selectedStore.value && storeList.value.length) {
      selectedStore.value = storeList.value[0];
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    storeLoading.value = false;
  }
}

async function refreshStoreList() {
  storeList.value = [];
  selectedStore.value = null;
  await loadStoreList();
}

function openStoreMap(store) {
  const latitude = Number(store?.latitude || 0);
  const longitude = Number(store?.longitude || 0);
  const query = latitude && longitude
    ? `?api=1&query=${latitude},${longitude}`
    : `?api=1&query=${encodeURIComponent([store?.name, store?.address, store?.detailedAddress].filter(Boolean).join(" "))}`;
  window.open(`https://www.google.com/maps/search/${query}`, "_blank", "noopener");
}

async function openStorePanel() {
  storePanelOpen.value = true;
  await loadStoreList();
}

function closeStorePanel() {
  storePanelOpen.value = false;
}

function chooseStore(store) {
  selectedStore.value = store;
  storePanelOpen.value = false;
}

async function recomputeCheckoutPrice() {
  if (!preOrderNo.value) {
    return;
  }
  try {
    checkoutPrice.value = await apiPost(
      "/api/front/order/computed/price",
      {
        preOrderNo: preOrderNo.value,
        addressId: checkoutAddress.value?.id,
        shippingType: checkoutShippingType.value + 1,
        useIntegral: checkoutUseIntegral.value,
        couponId: selectedCoupon.value?.id || 0
      },
      true
    );
  } catch (error) {
    showToast(error.message);
  }
}

async function createOrder() {
  if (!preOrderNo.value) {
    showToast("订单数据已失效，请重新结算");
    currentTab.value = "cart";
    return;
  }
  if (checkoutShippingType.value === 0 && !checkoutAddress.value?.id) {
    showToast("请先维护默认收货地址");
    return;
  }
  if (checkoutShippingType.value === 1) {
    if (!selectedStore.value?.id) {
      showToast("暂无门店,请选择其他方式");
      return;
    }
    if (!pickupContact.value || !pickupPhone.value) {
      showToast("请填写联系人及联系人电话");
      return;
    }
    if (!/^1(3|4|5|6|7|8|9)\d{9}$/.test(pickupPhone.value)) {
      showToast("请填写正确的手机号");
      return;
    }
    if (!/^[\u4e00-\u9fa5\w]{2,16}$/.test(pickupContact.value)) {
      showToast("请填写您的真实姓名");
      return;
    }
  }
  creatingOrder.value = true;
  try {
    const created = await apiPost(
      "/api/front/order/create",
      {
        preOrderNo: preOrderNo.value,
        addressId: checkoutShippingType.value === 0 ? checkoutAddress.value.id : 0,
        shippingType: checkoutShippingType.value + 1,
        storeId: selectedStore.value?.id || 0,
        realName: pickupContact.value,
        phone: pickupPhone.value,
        useIntegral: checkoutUseIntegral.value,
        couponId: selectedCoupon.value?.id || 0,
        mark: checkoutMark.value
      },
      true
    );
    showToast("订单已创建");
    checkoutItems.value = [];
    preOrderNo.value = "";
    selectedStore.value = null;
    pickupContact.value = "";
    pickupPhone.value = "";
    orderType.value = 0;
    currentTab.value = "profile";
    await Promise.all([loadCart(), loadOrders()]);
    const createdOrder = orders.value.find((item) => item.orderId === created.orderId) || created;
    await payOrder(createdOrder);
  } catch (error) {
    showToast(error.message);
  } finally {
    creatingOrder.value = false;
  }
}

function toggleCartItem(id) {
  const numericId = Number(id);
  if (selectedCartIds.value.includes(numericId)) {
    selectedCartIds.value = selectedCartIds.value.filter((item) => item !== numericId);
  } else {
    selectedCartIds.value = [...selectedCartIds.value, numericId];
  }
}

function toggleAllCart() {
  selectedCartIds.value = isAllCartSelected.value ? [] : cartItems.value.map((item) => Number(item.id));
}

async function loadCollects() {
  if (!authToken.value) {
    return;
  }
  collectLoading.value = true;
  try {
    const data = await apiGet("/api/front/collect/user", { page: 1, limit: 50 }, true);
    collectItems.value = data.list || [];
    collectTotal.value = data.total || collectItems.value.length;
    const currentIds = new Set(collectItems.value.map((item) => Number(item.id)));
    selectedCollectIds.value = selectedCollectIds.value.filter((id) => currentIds.has(Number(id)));
  } catch (error) {
    handleAuthError(error);
  } finally {
    collectLoading.value = false;
  }
}

async function loadUserCoupons() {
  if (!authToken.value) {
    return;
  }
  userCouponLoading.value = true;
  try {
    const data = await apiGet(
      "/api/front/coupons/user",
      { type: userCouponType.value, page: 1, limit: 50 },
      true
    );
    userCouponItems.value = data.list || [];
    if (userCouponType.value === "usable") {
      userCouponUsableCount.value = userCouponItems.value.length;
    }
  } catch (error) {
    handleAuthError(error);
  } finally {
    userCouponLoading.value = false;
  }
}

function selectUserCouponType(type) {
  userCouponType.value = type;
  loadUserCoupons();
}

async function loadCouponCenter() {
  couponCenterLoading.value = true;
  try {
    const data = await apiGet("/api/front/coupons", { type: couponCenterType.value, page: 1, limit: 50 }, Boolean(authToken.value));
    couponCenterItems.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    couponCenterLoading.value = false;
  }
}

function selectCouponCenterType(type) {
  couponCenterType.value = type;
  loadCouponCenter();
}

async function receiveCoupon(coupon) {
  if (!authToken.value) {
    showToast("请先登录后领取优惠券");
    currentTab.value = "profile";
    return;
  }
  if (!coupon?.id || coupon.isUse) {
    return;
  }
  receivingCouponId.value = Number(coupon.id);
  try {
    await apiPost("/api/front/coupon/receive", { couponId: coupon.id }, true);
    couponCenterItems.value = couponCenterItems.value.map((item) =>
      Number(item.id) === Number(coupon.id)
        ? { ...item, isUse: true, use_title: "已领取" }
        : item
    );
    showToast("领取成功");
    await loadUserCoupons();
  } catch (error) {
    showToast(error.message);
  } finally {
    receivingCouponId.value = 0;
  }
}

async function receiveProductCoupon(coupon) {
  if (!authToken.value) {
    detailOpen.value = false;
    currentTab.value = "profile";
    return;
  }
  if (!coupon?.id || coupon.isUse) {
    return;
  }
  receivingCouponId.value = Number(coupon.id);
  try {
    await apiPost("/api/front/coupon/receive", { couponId: coupon.id }, true);
    markDetailCouponReceived(coupon.id);
    showToast("领取成功");
    await loadUserCoupons();
  } catch (error) {
    showToast(error.message);
  } finally {
    receivingCouponId.value = 0;
  }
}

function markDetailCouponReceived(couponId) {
  if (!detail.value) {
    return;
  }
  const updateList = (list) => Array.isArray(list)
    ? list.map((item) => Number(item.id) === Number(couponId)
      ? { ...item, isUse: true, use_title: "已领取" }
      : item)
    : list;
  detail.value = {
    ...detail.value,
    defaultCoupon: updateList(detail.value.defaultCoupon),
    couponList: updateList(detail.value.couponList),
    coupons: updateList(detail.value.coupons)
  };
}

function openDetailCustomerService() {
  detailOpen.value = false;
  openCustomerService("home");
}

function openDetailCart() {
  detailOpen.value = false;
  switchTab("cart");
}

async function loadBalance() {
  if (!authToken.value) {
    return;
  }
  balanceLoading.value = true;
  try {
    balanceInfo.value = await apiGet("/api/front/user/balance", {}, true);
  } catch (error) {
    handleAuthError(error);
  } finally {
    balanceLoading.value = false;
  }
}

async function loadRechargeConfig() {
  if (!authToken.value) {
    return;
  }
  rechargeLoading.value = true;
  try {
    rechargeConfig.value = await apiGet("/api/front/recharge/index", {}, true);
    const quotas = rechargeConfig.value.rechargeQuota || [];
    if (!selectedRechargeId.value && quotas.length) {
      selectRechargeQuota(quotas[0]);
    }
  } catch (error) {
    handleAuthError(error);
  } finally {
    rechargeLoading.value = false;
  }
}

async function openRechargePanel() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  rechargeOpen.value = true;
  if (!rechargeConfig.value.rechargeQuota) {
    await loadRechargeConfig();
  }
}

function closeRechargePanel() {
  rechargeOpen.value = false;
}

function selectRechargeQuota(item) {
  selectedRechargeId.value = item.id;
  customRechargePrice.value = String(item.price || "");
}

async function submitRecharge() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const price = Number(customRechargePrice.value || 0);
  if (!Number.isFinite(price) || price <= 0) {
    showToast("请输入充值金额");
    return;
  }
  submittingRecharge.value = true;
  try {
    const result = await apiPost(
      "/api/front/recharge/wechat",
      {
        price,
        rechar_id: selectedRechargeId.value || 0,
        payType: "weixin",
        from: "public"
      },
      true
    );
    showToast(result.message || "已生成待支付充值单");
    rechargeOpen.value = false;
    await loadBills();
  } catch (error) {
    handleAuthError(error);
  } finally {
    submittingRecharge.value = false;
  }
}

async function loadBills() {
  if (!authToken.value) {
    return;
  }
  billLoading.value = true;
  try {
    const data = await apiGet(
      "/api/front/recharge/bill/record",
      { type: billType.value, page: 1, limit: 50 },
      true
    );
    billGroups.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    billLoading.value = false;
  }
}

async function loadIntegral() {
  if (!authToken.value) {
    return;
  }
  integralLoading.value = true;
  try {
    const [info, list] = await Promise.all([
      apiGet("/api/front/integral/user", {}, true),
      apiGet("/api/front/integral/list", { page: 1, limit: 50 }, true)
    ]);
    integralInfo.value = info || {};
    integralRecords.value = list.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    integralLoading.value = false;
  }
}

async function loadSignCenter() {
  if (!authToken.value) {
    return;
  }
  signLoading.value = true;
  try {
    const [config, info, list] = await Promise.all([
      apiGet("/api/front/user/sign/config", {}, true),
      apiPost("/api/front/user/sign/user", {}, true),
      apiGet("/api/front/user/sign/list", { page: 1, limit: 3 }, true)
    ]);
    signConfig.value = config || [];
    signInfo.value = info || {};
    signList.value = list.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    signLoading.value = false;
  }
}

async function loadMemberLevel() {
  if (!authToken.value) {
    return;
  }
  memberLevelLoading.value = true;
  try {
    const [, levels, records] = await Promise.all([
      loadUserInfo(),
      apiGet("/api/front/user/level/grade", {}, true),
      apiGet("/api/front/user/expList", { page: 1, limit: 20 }, true)
    ]);
    memberLevels.value = levels || [];
    experienceRecords.value = records.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    memberLevelLoading.value = false;
  }
}

async function loadSpreadCenter() {
  if (!authToken.value) {
    return;
  }
  spreadLoading.value = true;
  try {
    const [info, summary] = await Promise.all([
      apiGet("/api/front/commission", {}, true),
      apiGet("/api/front/spread/people/count", {}, true)
    ]);
    spreadInfo.value = info || {};
    spreadPeopleSummary.value = summary || {};
  } catch (error) {
    handleAuthError(error);
  } finally {
    spreadLoading.value = false;
  }
}

async function openBrokerageRecords() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "brokerageRecords";
  if (!Object.keys(spreadInfo.value).length) {
    await loadSpreadCenter();
  }
  await loadBrokerageRecords();
}

async function loadBrokerageRecords() {
  if (!authToken.value) {
    return;
  }
  brokerageRecordLoading.value = true;
  try {
    const data = await apiGet("/api/front/spread/commission/detail", { page: 1, limit: 20 }, true);
    brokerageRecordGroups.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    brokerageRecordLoading.value = false;
  }
}

async function openExtractRecords() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "extractRecords";
  if (!Object.keys(spreadInfo.value).length) {
    await loadSpreadCenter();
  }
  await loadExtractRecords();
}

async function loadExtractRecords() {
  if (!authToken.value) {
    return;
  }
  extractRecordLoading.value = true;
  try {
    const data = await apiGet("/api/front/extract/record", { page: 1, limit: 20 }, true);
    extractRecordGroups.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    extractRecordLoading.value = false;
  }
}

async function openExtractCash() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "extractCash";
  if (!Object.keys(spreadInfo.value).length) {
    await loadSpreadCenter();
  }
  await loadExtractCash();
}

async function loadExtractCash() {
  if (!authToken.value) {
    return;
  }
  extractCashLoading.value = true;
  try {
    const [info, banks] = await Promise.all([
      apiGet("/api/front/extract/user", {}, true),
      apiGet("/api/front/extract/bank", {}, true)
    ]);
    extractCashInfo.value = info || {};
    extractBankList.value = banks || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    extractCashLoading.value = false;
  }
}

async function submitExtractCash() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const money = String(extractCashForm.value.money || "").trim();
  const amount = Number(money);
  if (!extractCashForm.value.name) {
    showToast("请填写真实姓名");
    return;
  }
  if (extractCashType.value === "bank" && !extractCashForm.value.cardum) {
    showToast("请填写银行卡号");
    return;
  }
  if (extractCashType.value === "bank" && !extractCashForm.value.bankName) {
    showToast("请选择银行");
    return;
  }
  if (extractCashType.value === "weixin" && !extractCashForm.value.wechat) {
    showToast("请填写微信账号");
    return;
  }
  if (!/^\d+(\.\d{1,2})?$/.test(money) || amount <= 0) {
    showToast("请输入正确提现金额");
    return;
  }
  const minPrice = Number(extractCashInfo.value.minPrice || 0);
  if (amount < minPrice) {
    showToast(`提现金额不能低于${moneyText(minPrice)}元`);
    return;
  }
  if (amount > Number(extractCashInfo.value.commissionCount || 0)) {
    showToast("提现金额不能超过可提现佣金");
    return;
  }

  const payload =
    extractCashType.value === "bank"
      ? {
          extractType: "bank",
          name: extractCashForm.value.name,
          cardum: extractCashForm.value.cardum,
          bankName: extractCashForm.value.bankName,
          money
        }
      : {
          extractType: "weixin",
          name: extractCashForm.value.name,
          wechat: extractCashForm.value.wechat,
          qrcodeUrl: extractCashForm.value.qrcodeUrl,
          money
        };
  submittingExtractCash.value = true;
  try {
    await apiPost("/api/front/extract/cash", payload, true);
    showToast("提现申请已提交");
    resetExtractCashForm();
    await Promise.all([loadSpreadCenter(), loadExtractCash(), loadExtractRecords()]);
    currentTab.value = "extractRecords";
  } catch (error) {
    handleAuthError(error);
  } finally {
    submittingExtractCash.value = false;
  }
}

function updateExtractCashField(key, value) {
  updateFormField(extractCashForm, key, value);
}

function resetExtractCashForm() {
  extractCashForm.value = emptyExtractCashForm(extractBankList.value[0] || "");
}

async function loadSpreadPeople() {
  if (!authToken.value) {
    return;
  }
  spreadPeopleLoading.value = true;
  try {
    const [summary, data] = await Promise.all([
      apiGet("/api/front/spread/people/count", {}, true),
      apiGet(
        "/api/front/spread/people",
        { grade: spreadGrade.value, sortKey: spreadSortKey.value || undefined, isAsc: "DESC", page: 1, limit: 50 },
        true
      )
    ]);
    spreadPeopleSummary.value = summary || {};
    spreadPeopleItems.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    spreadPeopleLoading.value = false;
  }
}

async function loadSpreadOrders() {
  if (!authToken.value) {
    return;
  }
  spreadOrderLoading.value = true;
  try {
    const data = await apiGet("/api/front/spread/order", { page: 1, limit: 50 }, true);
    spreadOrderCount.value = data.count || 0;
    spreadOrderGroups.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    spreadOrderLoading.value = false;
  }
}

async function loadSpreadPoster() {
  if (!authToken.value) {
    return;
  }
  spreadPosterLoading.value = true;
  try {
    const [banners] = await Promise.all([
      apiGet("/api/front/user/spread/banner", { page: 1, limit: 5 }, true),
      userInfo.value ? Promise.resolve() : loadUserInfo()
    ]);
    spreadPosterList.value = banners || [];
    if (spreadPosterIndex.value >= spreadPosterList.value.length) {
      spreadPosterIndex.value = 0;
    }
  } catch (error) {
    handleAuthError(error);
  } finally {
    spreadPosterLoading.value = false;
  }
}

async function loadSpreadRank() {
  if (!authToken.value) {
    return;
  }
  rankLoading.value = true;
  try {
    const data = await apiGet("/api/front/rank", { type: rankType.value, page: 1, limit: 20 }, true);
    spreadRankList.value = data || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    rankLoading.value = false;
  }
}

async function loadBrokerageRank() {
  if (!authToken.value) {
    return;
  }
  rankLoading.value = true;
  try {
    const data = await apiGet("/api/front/brokerage_rank", { type: rankType.value, page: 1, limit: 20 }, true);
    brokerageRankList.value = data || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    rankLoading.value = false;
  }
}

function selectSpreadGrade(grade) {
  spreadGrade.value = grade;
  loadSpreadPeople();
}

function selectSpreadSort(sortKey) {
  spreadSortKey.value = sortKey;
  loadSpreadPeople();
}

function selectRankType(type) {
  rankType.value = type;
  if (currentTab.value === "spreadRank") {
    loadSpreadRank();
  } else if (currentTab.value === "brokerageRank") {
    loadBrokerageRank();
  }
}

function selectSpreadPoster(index) {
  if (index < 0 || index >= spreadPosterList.value.length) {
    return;
  }
  spreadPosterIndex.value = index;
  spreadManualCopyLink.value = "";
}

async function copySpreadPosterLink() {
  try {
    await navigator.clipboard.writeText(spreadPosterLink.value);
    spreadManualCopyLink.value = "";
    showToast("推广链接已复制");
  } catch {
    spreadManualCopyLink.value = spreadPosterLink.value;
    showToast("已显示推广链接，请长按或全选复制");
  }
}

async function signNow() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (signInfo.value.isDaySign) {
    showToast("您今日已签到!");
    return;
  }
  signing.value = true;
  try {
    const result = await apiGet("/api/front/user/sign/integral", {}, true);
    showToast(`签到成功，获得${result.integral || 0}积分`);
    await Promise.all([loadSignCenter(), loadUserInfo(), loadIntegral()]);
  } catch (error) {
    showToast(error.message);
  } finally {
    signing.value = false;
  }
}

async function openSignRecords() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "signRecords";
  signRecordsLoading.value = true;
  try {
    const data = await apiGet("/api/front/user/sign/month", { page: 1, limit: 50 }, true);
    signRecordGroups.value = data.list || [];
  } catch (error) {
    handleAuthError(error);
  } finally {
    signRecordsLoading.value = false;
  }
}

function openBill(type = "all") {
  billType.value = type;
  currentTab.value = "bill";
  loadBills();
}

function selectBillType(type) {
  billType.value = type;
  loadBills();
}

function toggleCollectItem(id) {
  const numericId = Number(id);
  if (selectedCollectIds.value.includes(numericId)) {
    selectedCollectIds.value = selectedCollectIds.value.filter((item) => item !== numericId);
  } else {
    selectedCollectIds.value = [...selectedCollectIds.value, numericId];
  }
}

function toggleAllCollect() {
  selectedCollectIds.value = isAllCollectSelected.value ? [] : collectItems.value.map((item) => Number(item.id));
}

async function deleteSelectedCollect() {
  if (!selectedCollectIds.value.length) {
    showToast("请选择商品");
    return;
  }
  try {
    await apiPost("/api/front/collect/delete", { ids: selectedCollectIds.value.join(",") }, true);
    showToast("取消收藏成功");
    selectedCollectIds.value = [];
    await loadCollects();
  } catch (error) {
    showToast(error.message);
  }
}

async function openAddressBook(returnTab = "profile") {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  addressReturnTab.value = returnTab;
  currentTab.value = "address";
  addressEditing.value = false;
  await loadAddresses();
}

async function openLegacyAddressBook() {
  await openAddressBook(initialParams.get("preOrderNo") ? "checkout" : "profile");
  const id = Number(initialParams.get("id") || 0);
  if (id > 0) {
    const address = addresses.value.find((item) => Number(item.id) === id);
    if (address) {
      editAddress(address);
    }
  } else if (window.location.pathname.includes("/pages/users/user_address/index")) {
    createAddress();
  }
}

async function openLegacyAddressBookFromParsed(parsed, returnTab = "profile") {
  const preOrderNo = legacyUrlValue(parsed, "preOrderNo", "pre_order_no");
  await openAddressBook(preOrderNo ? "checkout" : returnTab);
  const id = Number(legacyUrlValue(parsed, "id", "addressId") || 0);
  if (id > 0) {
    const address = addresses.value.find((item) => Number(item.id) === id);
    if (address) {
      editAddress(address);
    }
    return;
  }
  if (parsed.pathname.includes("/pages/users/user_address/index")) {
    createAddress();
  }
}

function createAddress() {
  addressForm.value = emptyAddressForm();
  addressEditing.value = true;
}

function editAddress(address) {
  addressForm.value = {
    id: address.id,
    realName: address.realName || "",
    phone: address.phone || "",
    province: address.province || address.address?.province || "",
    city: address.city || address.address?.city || "",
    district: address.district || address.address?.district || "",
    detail: address.detail || "",
    isDefault: Boolean(address.isDefault)
  };
  addressEditing.value = true;
}

function updateAddressField(key, value) {
  updateFormField(addressForm, key, value);
}

async function saveAddress() {
  savingAddress.value = true;
  try {
    const saved = await apiPost("/api/front/address/edit", addressForm.value, true);
    showToast("地址已保存");
    addressEditing.value = false;
    await loadAddresses();
    if (addressReturnTab.value === "checkout" && (!checkoutAddress.value?.id || saved.isDefault)) {
      checkoutAddress.value = saved;
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    savingAddress.value = false;
  }
}

async function deleteAddress(id) {
  const confirmed = await openConfirm({
    title: "删除地址",
    message: "确定删除这个地址吗？",
    confirmText: "删除"
  });
  if (!confirmed) {
    return;
  }
  try {
    await apiPost("/api/front/address/del", { id }, true);
    showToast("地址已删除");
    if (checkoutAddress.value?.id === id) {
      checkoutAddress.value = {};
    }
    await loadAddresses();
  } catch (error) {
    showToast(error.message);
  }
}

async function setDefaultAddress(id) {
  try {
    const address = await apiPost("/api/front/address/default/set", { id }, true);
    showToast("已设为默认");
    await loadAddresses();
    if (addressReturnTab.value === "checkout") {
      checkoutAddress.value = address;
    }
  } catch (error) {
    showToast(error.message);
  }
}

async function chooseAddress(address) {
  if (addressReturnTab.value !== "checkout") {
    return;
  }
  checkoutAddress.value = address;
  currentTab.value = "checkout";
  if (preOrderNo.value) {
    try {
      checkoutPrice.value = await apiPost(
        "/api/front/order/computed/price",
        {
          preOrderNo: preOrderNo.value,
          addressId: address.id,
          shippingType: checkoutShippingType.value + 1,
          useIntegral: checkoutUseIntegral.value,
          couponId: selectedCoupon.value?.id || 0
        },
        true
      );
    } catch (error) {
      showToast(error.message);
    }
  }
}

async function cancelOrder(order) {
  const confirmed = await openConfirm({
    title: "取消订单",
    message: "确定取消这个订单吗？",
    confirmText: "取消订单"
  });
  if (!confirmed) {
    return;
  }
  try {
    await apiPost("/api/front/order/cancel", { id: order.id }, true);
    showToast("订单已取消");
    selectedOrder.value = null;
    currentTab.value = "profile";
    await loadOrders();
  } catch (error) {
    showToast(error.message);
  }
}

async function payOrder(order) {
  if (!order?.orderId || paymentOpen.value) {
    return;
  }
  paymentOrder.value = order;
  paymentOpen.value = true;
  try {
    payConfig.value = await apiGet("/api/front/order/get/pay/config", {}, true);
  } catch (error) {
    showToast(error.message);
  }
}

function paymentMethodEnabled(method) {
  if (isThirdPartyPaymentSafeMode(method)) {
    return false;
  }
  return Number(method?.payStatus ?? method?.status ?? 0) === 1 || method?.value === "yue" && payConfig.value.yuePayStatus !== false;
}

function paymentMethodSubText(method) {
  if (isThirdPartyPaymentSafeMode(method)) {
    return "支付服务需配置后启用，当前不会发起真实支付";
  }
  if (method?.value === "yue") {
    return `可用余额：￥${moneyText(method.userBalance ?? payBalance.value)}`;
  }
  return method?.title || method?.name || "";
}

function isThirdPartyPaymentSafeMode(method) {
  const value = String(method?.value || "").toLowerCase();
  if (value !== "weixin" && value !== "alipay") {
    return false;
  }
  return method?.dryRun === true || payConfig.value.dryRun !== false || payConfig.value.realThirdPartyPay !== true;
}

async function choosePaymentMethod(order, payType) {
  if (!order?.orderId) {
    showToast("缺少订单号无法付款");
    return;
  }
  const method = paymentMethods.value.find((item) => item.value === payType);
  if (!method || !paymentMethodEnabled(method)) {
    showToast(`${paymentMethodText(payType)}当前不可用`);
    return;
  }
  paymentOrder.value = order;
  await executePayment(payType);
}

function closePayment() {
  if (paymentLoading.value) {
    return;
  }
  paymentOpen.value = false;
  paymentOrder.value = null;
}

async function executePayment(payType) {
  const order = paymentOrder.value;
  if (!order?.orderId || paymentLoading.value) {
    return;
  }
  if (payType !== "yue") {
    showToast(`${paymentMethodText(payType)}当前不可用`);
    return;
  }
  if (Number(payBalance.value) < Number(order.payPrice || 0)) {
    showToast("余额不足！");
    return;
  }
  const wasOrderDetail = currentTab.value === "orderDetail";
  paymentLoading.value = true;
  payingOrder.value = order.orderId;
  try {
    const result = await apiPost(
      "/api/front/pay/payment",
      { orderNo: order.orderId, payType, payChannel: "h5", from: "h5" },
      true
    );
    if (result.status) {
      showToast(result.message || "支付成功");
      paymentOpen.value = false;
      paymentOrder.value = null;
      await Promise.all([loadUserInfo(), loadOrders()]);
      if (wasOrderDetail) {
        selectedOrder.value = await apiGet(`/api/front/order/detail/${order.orderId}`, {}, true);
      }
      await loadPayStatus(order.orderId);
    } else {
      showToast(result.message || "支付未完成");
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    paymentLoading.value = false;
    payingOrder.value = "";
  }
}

async function deleteOrder(order) {
  const confirmed = await openConfirm({
    title: "删除订单",
    message: "确定删除这个订单吗？",
    confirmText: "删除"
  });
  if (!confirmed) {
    return;
  }
  try {
    await apiPost("/api/front/order/del", { id: order.id, orderId: order.orderId }, true);
    showToast("删除成功");
    if (selectedOrder.value?.id === order.id) {
      selectedOrder.value = null;
      currentTab.value = "profile";
    }
    await loadOrders();
  } catch (error) {
    showToast(error.message);
  }
}

async function takeOrder(order) {
  const confirmed = await openConfirm({
    title: "确认收货",
    message: "为保障权益，请收到货确认无误后，再确认收货。",
    confirmText: "确认收货"
  });
  if (!confirmed) {
    return;
  }
  try {
    await apiPost("/api/front/order/take", { id: order.id }, true);
    showToast("操作成功");
    await loadOrders();
    if (selectedOrder.value?.id === order.id) {
      selectedOrder.value = await apiGet(`/api/front/order/detail/${order.orderId}`, {}, true);
    }
  } catch (error) {
    showToast(error.message);
  }
}

async function openLogistics(order) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  logisticsLoading.value = true;
  logisticsInfo.value = null;
  deliveryManualCopyId.value = "";
  currentTab.value = "logistics";
  try {
    logisticsInfo.value = await apiGet(`/api/front/order/express/${order.orderId}`, {}, true);
  } catch (error) {
    showToast(error.message);
    currentTab.value = selectedOrder.value ? "orderDetail" : "profile";
  } finally {
    logisticsLoading.value = false;
  }
}

async function copyDeliveryId() {
  const deliveryId = logisticsInfo.value?.order?.deliveryId;
  if (!deliveryId) {
    showToast("暂无快递单号");
    return;
  }
  try {
    await navigator.clipboard.writeText(String(deliveryId));
    deliveryManualCopyId.value = "";
    showToast("复制成功");
  } catch {
    deliveryManualCopyId.value = String(deliveryId);
    showToast("已显示快递单号，请长按或全选复制");
  }
}

async function openRefund(order) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "refund";
  refundLoading.value = true;
  refundOrder.value = null;
  refundPics.value = [];
  refundForm.value = emptyRefundForm(order.orderId);
  try {
    const [orderInfo, reasons] = await Promise.all([
      apiGet(`/api/front/order/apply/refund/${order.orderId}`, {}, true),
      apiGet("/api/front/order/refund/reason", {}, true)
    ]);
    refundOrder.value = orderInfo;
    refundReasons.value = reasons || [];
    refundForm.value.uni = orderInfo.orderId;
    if (!refundForm.value.text && refundReasons.value.length) {
      refundForm.value.text = refundReasons.value[0];
    }
  } catch (error) {
    showToast(error.message);
    currentTab.value = "profile";
  } finally {
    refundLoading.value = false;
  }
}

function returnFromRefund() {
  currentTab.value = selectedOrder.value ? "orderDetail" : "profile";
}

async function uploadRefundImage(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) {
    return;
  }
  if (refundPics.value.length >= 3) {
    showToast("最多可上传3张");
    return;
  }
  refundUploading.value = true;
  try {
    const formData = new FormData();
    formData.append("multipart", file);
    formData.append("model", "product");
    formData.append("pid", "1");
    const uploaded = await apiUpload("/api/front/upload/image", formData, true);
    refundPics.value = [...refundPics.value, { url: uploaded.url, localPath: uploaded.url }];
  } catch (error) {
    showToast(error.message);
  } finally {
    refundUploading.value = false;
  }
}

function removeRefundPic(index) {
  refundPics.value = refundPics.value.filter((_, currentIndex) => currentIndex !== index);
}

async function openFirstComment(order) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  const detailOrder = order.cartInfo?.length ? order : await apiGet(`/api/front/order/detail/${order.orderId}`, {}, true);
  const goods = (detailOrder.cartInfo || []).find((item) => Number(item.isReply || 0) === 0);
  if (!goods) {
    showToast("该订单商品已评价");
    return;
  }
  await openComment(detailOrder, goods);
}

async function openComment(order, goods) {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  commentLoading.value = true;
  currentTab.value = "comment";
  commentOrder.value = order;
  commentGoods.value = null;
  commentPics.value = [];
  commentForm.value = emptyCommentForm();
  try {
    commentGoods.value = await apiPost(
      "/api/front/order/product",
      {
        orderNo: order.orderId,
        orderId: order.id,
        uni: goods.unique || goods.attrId || goods.attrValueId
      },
      true
    );
  } catch (error) {
    showToast(error.message);
    currentTab.value = "orderDetail";
  } finally {
    commentLoading.value = false;
  }
}

async function openLegacyComment() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  if (!initialOrderId || !initialCommentUnique) {
    showToast("缺少评价参数");
    currentTab.value = "profile";
    return;
  }
  commentLoading.value = true;
  currentTab.value = "comment";
  commentOrder.value = { orderId: initialOrderId, id: initialCommentInfoId };
  commentGoods.value = null;
  commentPics.value = [];
  commentForm.value = emptyCommentForm();
  try {
    commentGoods.value = await apiPost(
      "/api/front/order/product",
      {
        orderNo: initialOrderId,
        orderId: initialCommentInfoId,
        uni: initialCommentUnique
      },
      true
    );
  } catch (error) {
    showToast(error.message);
    currentTab.value = "profile";
  } finally {
    commentLoading.value = false;
  }
}

function returnFromComment() {
  currentTab.value = selectedOrder.value ? "orderDetail" : "profile";
}

async function uploadCommentImage(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) {
    return;
  }
  if (commentPics.value.length >= 8) {
    showToast("最多上传8张图片");
    return;
  }
  commentUploading.value = true;
  try {
    const formData = new FormData();
    formData.append("multipart", file);
    formData.append("model", "product");
    formData.append("pid", "1");
    const uploaded = await apiUpload("/api/front/upload/image", formData, true);
    commentPics.value = [...commentPics.value, { url: uploaded.url, localPath: uploaded.url }];
  } catch (error) {
    showToast(error.message);
  } finally {
    commentUploading.value = false;
  }
}

function removeCommentPic(index) {
  commentPics.value = commentPics.value.filter((_, currentIndex) => currentIndex !== index);
}

async function submitComment() {
  if (!commentGoods.value || !commentOrder.value) {
    showToast("评价数据已失效");
    return;
  }
  if (!commentForm.value.comment) {
    showToast("请填写你对宝贝的心得！");
    return;
  }
  if (!commentForm.value.productScore || !commentForm.value.serviceScore) {
    showToast("请选择评分");
    return;
  }
  submittingComment.value = true;
  try {
    await apiPost(
      "/api/front/order/comment",
      {
        orderNo: commentOrder.value.orderId,
        productId: commentGoods.value.productId,
        unique: commentGoods.value.unique || commentGoods.value.attrId || commentGoods.value.attrValueId,
        sku: commentGoods.value.sku,
        productScore: commentForm.value.productScore,
        serviceScore: commentForm.value.serviceScore,
        comment: commentForm.value.comment,
        pics: commentPics.value.length ? JSON.stringify(commentPics.value.map((item) => item.url)) : ""
      },
      true
    );
    showToast("感谢您的评价!");
    await Promise.all([loadOrders(), loadUserInfo()]);
    if (commentOrder.value?.orderId) {
      selectedOrder.value = await apiGet(`/api/front/order/detail/${commentOrder.value.orderId}`, {}, true);
      currentTab.value = "orderDetail";
    } else {
      currentTab.value = "profile";
    }
    commentOrder.value = null;
    commentGoods.value = null;
    commentPics.value = [];
  } catch (error) {
    showToast(error.message);
  } finally {
    submittingComment.value = false;
  }
}

async function submitRefund() {
  if (!refundForm.value.text) {
    showToast("请选择退款原因");
    return;
  }
  submittingRefund.value = true;
  try {
    await apiPost(
      "/api/front/order/refund",
      {
        uni: refundForm.value.uni,
        text: refundForm.value.text,
        refund_reason_wap_explain: refundForm.value.explain,
        refund_reason_wap_img: refundPics.value.map((item) => item.url).join(",")
      },
      true
    );
    showToast("申请成功");
    orderType.value = -3;
    selectedOrder.value = null;
    refundOrder.value = null;
    refundPics.value = [];
    currentTab.value = "profile";
    await Promise.all([loadOrders(), loadRefundOrders()]);
  } catch (error) {
    showToast(error.message);
  } finally {
    submittingRefund.value = false;
  }
}

async function login() {
  loginLoading.value = true;
  authMessage.value = "";
  try {
    const data = await apiPost("/api/front/login", loginForm.value);
    authToken.value = data.token;
    localStorage.setItem("crmeb-front-token", data.token);
    await Promise.all([loadUserInfo(), loadCart(), loadOrders(), loadAddresses(), loadUserCoupons(), loadBalance(), loadIntegral()]);
    await bindCachedSpread();
    showToast("登录成功");
  } catch (error) {
    authMessage.value = error.message;
  } finally {
    loginLoading.value = false;
  }
}

function logout() {
  if (authToken.value) {
    apiGet("/api/front/logout", {}, true).catch(() => {});
  }
  authToken.value = "";
  userInfo.value = null;
  cartItems.value = [];
  orders.value = [];
  addresses.value = [];
  userCouponItems.value = [];
  userCouponUsableCount.value = 0;
  balanceInfo.value = {};
  billGroups.value = [];
  integralInfo.value = {};
  integralRecords.value = [];
  signInfo.value = {};
  signList.value = [];
  signRecordGroups.value = [];
  memberLevels.value = [];
  experienceRecords.value = [];
  spreadInfo.value = {};
  spreadPeopleSummary.value = {};
  brokerageRecordGroups.value = [];
  extractRecordGroups.value = [];
  spreadPeopleItems.value = [];
  spreadOrderGroups.value = [];
  spreadOrderCount.value = 0;
  spreadPosterList.value = [];
  spreadManualCopyLink.value = "";
  pinkManualCopyLink.value = "";
  spreadPosterIndex.value = 0;
  bargainManualCopyLink.value = "";
  selectedOrder.value = null;
  refundOrder.value = null;
  localStorage.removeItem("crmeb-front-token");
  showToast("已退出");
}

function normalizeSpreadPid(value) {
  const number = Number.parseInt(String(value || ""), 10);
  return Number.isFinite(number) && number > 0 ? number : 0;
}

async function bindCachedSpread() {
  if (!authToken.value) {
    return;
  }
  const spreadPid = normalizeSpreadPid(localStorage.getItem(spreadStorageKey));
  if (!spreadPid) {
    localStorage.removeItem(spreadStorageKey);
    return;
  }
  try {
    const bound = await apiGet("/api/front/user/bindSpread", { spreadPid }, true);
    if (bound !== false) {
      localStorage.removeItem(spreadStorageKey);
    }
  } catch (error) {
    const message = String(error.message || "");
    if (!message.includes("登录")) {
      localStorage.removeItem(spreadStorageKey);
    }
  }
}

function recordVisit(visitType) {
  apiPost("/api/front/user/set_visit", { visitType }, Boolean(authToken.value)).catch(() => {});
}

function handleAuthError(error) {
  showToast(error.message);
  if (String(error.message).includes("登录")) {
    logout();
  }
}

function handleMenu(item) {
  const name = String(item?.name || "");
  const url = legacyMenuUrl(item);
  if (openLegacyOrderUrl(url)) {
    return;
  } else if (openLegacyCoveredUrl(url, currentTab.value)) {
    return;
  } else if (openExternalUrl(url)) {
    return;
  } else if (routeKnownMenu(name, url, currentTab.value)) {
    return;
  } else if (name.includes("秒杀") || url.includes("goods_seckill") || url.includes("seckill")) {
    switchTab("seckill");
  } else if (name.includes("拼团") || url.includes("goods_combination") || url.includes("combination")) {
    switchTab("combination");
  } else if (name.includes("砍价") || url.includes("goods_bargain") || url.includes("bargain")) {
    switchTab("bargain");
  } else if (name.includes("商品分类")) {
    switchTab("category");
  } else if (url.includes("user_get_coupon")) {
    switchTab("couponCenter");
  } else if (name.includes("优惠")) {
    switchTab("userCoupons");
  } else if (name.includes("资讯") || name.includes("文章") || name.includes("新闻") || name.includes("公告") || name.includes("消息") || name.includes("通知") || url.includes("/pages/news/")) {
    switchTab("articles");
  } else if (name.includes("客服") || name.includes("联系") || url.includes("/pages/users/kefu") || url.includes("/pages/users/web_page") || url.includes("/pages/service")) {
    openCustomerService(currentTab.value);
  } else if (name.includes("售后") || name.includes("退款") || url.includes("user_return_list")) {
    switchTab("refundList");
  } else if (name.includes("我的订单")) {
    openOrders(null);
  } else if (name.includes("会员") || url.includes("user_vip")) {
    switchTab("memberLevel");
  } else if (name.includes("账单") || url.includes("user_bill")) {
    switchTab("bill");
  } else if (name.includes("充值") || url.includes("user_payment")) {
    switchTab("balance");
    openRechargePanel();
  } else if (name.includes("签到记录") || url.includes("user_sgin_list")) {
    switchTab("signRecords");
  } else if (name.includes("收藏")) {
    switchTab("collection");
  } else if (url.includes("user_cash")) {
    openExtractCash();
  } else if (url.includes("user_spread_money")) {
    initialParams.get("type") === "1" || url.includes("type=1") ? openExtractRecords() : openBrokerageRecords();
  } else if (url.includes("promoter-list")) {
    switchTab("spreadPeople");
  } else if (url.includes("promoter-order")) {
    openSpreadOrders();
  } else if (url.includes("promoter_rank")) {
    openSpreadRank();
  } else if (url.includes("commission_rank")) {
    openBrokerageRank();
  } else if (url.includes("user_spread_code")) {
    openSpreadPoster();
  } else if (name.includes("推广") || url.includes("user_spread")) {
    switchTab("spread");
  } else if (url) {
    showToast(`${name || "该服务"}当前无法打开，请稍后重试`);
  }
}

function handleProfileMenu(item) {
  const name = String(item?.name || "");
  const url = legacyMenuUrl(item);
  if (openLegacyOrderUrl(url)) {
    return;
  } else if (openLegacyCoveredUrl(url, "profile")) {
    return;
  } else if (routeKnownMenu(name, url, "profile")) {
    return;
  } else if ((name.includes("砍价") && name.includes("记录")) || url.includes("/pages/activity/bargain") || url.includes("bargain_record")) {
    switchTab("bargainRecords");
  } else if ((name.includes("拼团") && (name.includes("记录") || name.includes("订单"))) || url.includes("goods_combination_status") || url.includes("combination_record")) {
    openActivityOrders("combination");
  } else if ((name.includes("秒杀") && name.includes("订单")) || url.includes("seckill_order")) {
    openActivityOrders("seckill");
  } else if (url.includes("user_spread_money") && (url.includes("type=1") || name.includes("提现"))) {
    openExtractRecords();
  } else if (name.includes("佣金") || url.includes("user_spread_money")) {
    openBrokerageRecords();
  } else if (url.includes("user_cash")) {
    openExtractCash();
  } else if (url.includes("promoter-list")) {
    switchTab("spreadPeople");
  } else if (url.includes("promoter-order")) {
    openSpreadOrders();
  } else if (url.includes("promoter_rank")) {
    openSpreadRank();
  } else if (url.includes("commission_rank")) {
    openBrokerageRank();
  } else if (url.includes("user_spread_code")) {
    openSpreadPoster();
  } else if (name.includes("推广") || url.includes("user_spread_user")) {
    switchTab("spread");
  } else if (name.includes("会员") || name.includes("等级") || url.includes("user_vip") || url.includes("level")) {
    switchTab("memberLevel");
  } else if (name.includes("账单") || url.includes("user_bill")) {
    switchTab("bill");
  } else if (name.includes("充值") || url.includes("user_payment")) {
    switchTab("balance");
    openRechargePanel();
  } else if (name.includes("余额") || url.includes("user_money")) {
    switchTab("balance");
  } else if (name.includes("积分") || url.includes("user_integral")) {
    switchTab("integral");
  } else if (name.includes("收藏") || url.includes("user_goods_collection")) {
    switchTab("collection");
  } else if (url.includes("user_get_coupon")) {
    switchTab("couponCenter");
  } else if (name.includes("优惠") || url.includes("coupon")) {
    switchTab("userCoupons");
  } else if (name.includes("地址") || url.includes("user_address")) {
    openAddressBook("profile");
  } else if (name.includes("签到记录") || url.includes("user_sgin_list")) {
    switchTab("signRecords");
  } else if (name.includes("签到") || url.includes("user_sgin")) {
    switchTab("sign");
  } else if (name.includes("售后") || name.includes("退款") || url.includes("user_return_list")) {
    switchTab("refundList");
  } else if (name.includes("资讯") || name.includes("文章") || name.includes("新闻") || name.includes("公告") || name.includes("消息") || name.includes("通知") || url.includes("/pages/news/")) {
    switchTab("articles");
  } else if (name.includes("客服") || name.includes("联系") || url.includes("/pages/users/kefu") || url.includes("/pages/users/web_page") || url.includes("/pages/service")) {
    openCustomerService("profile");
  } else if (name.includes("个人资料") || name.includes("账户") || name.includes("安全") || name.includes("设置") || url.includes("user_info") || url.includes("user_phone") || url.includes("user_pwd")) {
    openUserInfo();
  } else if (name.includes("订单") || url.includes("order_list")) {
    openOrders(null);
  } else if (openExternalUrl(url)) {
    return;
  } else {
    showToast(`${name || "该服务"}当前无法打开，请稍后重试`);
  }
}

function routeKnownMenu(name, url, returnTab = "profile") {
  const label = String(name || "");
  const link = normalizeLegacyUrl(url);
  if (label.includes("待付款")) {
    openOrders(0);
    return true;
  }
  if (label.includes("待发货")) {
    openOrders(1);
    return true;
  }
  if (label.includes("待收货")) {
    openOrders(2);
    return true;
  }
  if (label.includes("待评价")) {
    openOrders(3);
    return true;
  }
  if (label.includes("全部订单") || label.includes("我的订单") || link.includes("order_list")) {
    openOrders(null);
    return true;
  }
  const orderId = menuOrderIdFromLink(link);
  if (label.includes("订单详情") || label.includes("订单明细") || link.includes("order_details")) {
    orderId ? openOrderDetail({ orderId }) : openOrders(null);
    return true;
  }
  if (label.includes("去付款") || label.includes("继续支付") || label.includes("立即支付") || label.includes("订单支付") || link.includes("order_payment")) {
    orderId ? openPaymentPage(orderId) : openOrders(0);
    return true;
  }
  if (label.includes("支付状态") || label.includes("支付结果") || label.includes("付款结果") || link.includes("order_pay_status")) {
    orderId ? loadPayStatus(orderId) : openOrders(0);
    return true;
  }
  if (label.includes("查看物流") || label.includes("物流信息") || label.includes("物流详情") || label.includes("订单物流") || link.includes("goods_logistics")) {
    orderId ? openLogistics({ orderId }) : openOrders(2);
    return true;
  }
  if (label.includes("商品评价") || label.includes("评价列表") || label.includes("用户评价") || label.includes("评价中心") || link.includes("goods_comment_list")) {
    const productId = menuProductIdFromLink(link);
    productId ? openProductReplies(productId) : openOrders(3);
    return true;
  }
  if (label.includes("发表评价") || label.includes("去评价") || label.includes("订单评价") || link.includes("goods_comment_con")) {
    openOrders(3);
    return true;
  }
  if (label === "首页" || label.includes("商城首页") || link.includes("/pages/index/index")) {
    switchTab("home");
    return true;
  }
  if (label === "我的" || label.includes("个人中心") || link.includes("/pages/user/user") || link.includes("/pages/user/index")) {
    switchTab("profile");
    return true;
  }
  if (label.includes("购物车") || label.includes("购物袋") || link.includes("order_addcart")) {
    switchTab("cart");
    return true;
  }
  if (label.includes("领券") || label.includes("领优惠券") || label.includes("优惠券中心") || link.includes("user_get_coupon")) {
    switchTab("couponCenter");
    return true;
  }
  if (label.includes("商品搜索") || label === "搜索" || link.includes("goods_search")) {
    openSearch();
    return true;
  }
  const menuProductType = productTypeFromMenuLabel(label);
  if (
    label.includes("商品列表")
    || label === "商品"
    || label.includes("全部商品")
    || label.includes("商品中心")
    || label.includes("商品频道")
    || label.includes("好物")
    || menuProductType > 0
    || link.includes("goods_list")
  ) {
    keyword.value = "";
    activeCid.value = "";
    activeType.value = menuProductType;
    switchTab("home");
    return true;
  }
  if (label.includes("砍价") && label.includes("记录")) {
    switchTab("bargainRecords");
    return true;
  }
  if (label.includes("拼团") && (label.includes("记录") || label.includes("订单"))) {
    openActivityOrders("combination");
    return true;
  }
  if (label.includes("秒杀") && label.includes("订单")) {
    openActivityOrders("seckill");
    return true;
  }
  if (label.includes("秒杀") || link.includes("goods_seckill") || link.includes("seckill")) {
    switchTab("seckill");
    return true;
  }
  if (label.includes("拼团") || link.includes("goods_combination") || link.includes("combination")) {
    switchTab("combination");
    return true;
  }
  if (label.includes("砍价") || link.includes("goods_bargain") || link.includes("bargain")) {
    switchTab("bargain");
    return true;
  }
  if (label.includes("商品分类") || label === "分类" || label.includes("分类导航") || link.includes("goods_cate")) {
    switchTab("category");
    return true;
  }
  if (label.includes("手机号") || link.includes("user_phone")) {
    openPhoneChange();
    return true;
  }
  if (label.includes("密码") || label.includes("账户安全") || link.includes("user_pwd")) {
    openPasswordChange();
    return true;
  }
  if (
    label.includes("个人资料")
    || label.includes("我的资料")
    || label.includes("资料")
    || label.includes("账号管理")
    || label.includes("帐号管理")
    || label.includes("账户管理")
    || label.includes("设置")
    || label.includes("权限设置")
    || link.includes("user_info")
  ) {
    openUserInfo();
    return true;
  }
  if (label.includes("充值") || link.includes("user_payment")) {
    switchTab("balance");
    openRechargePanel();
    return true;
  }
  if (label.includes("余额") || label.includes("钱包") || label.includes("账户余额") || label.includes("我的账户") || label.includes("我的钱包") || link.includes("user_money")) {
    switchTab("balance");
    return true;
  }
  if (
    label.includes("账单")
    || (label.includes("明细") && label.includes("账户"))
    || label.includes("消费记录")
    || label.includes("充值记录")
    || label.includes("余额记录")
    || label.includes("资金记录")
    || link.includes("user_bill")
    || link.includes("recharge_record")
    || link.includes("consume_record")
  ) {
    openBill();
    return true;
  }
  if (label.includes("积分") || label.includes("经验值") || link.includes("user_integral")) {
    switchTab("integral");
    return true;
  }
  if (label.includes("签到记录") || link.includes("user_sgin_list")) {
    openSignRecords();
    return true;
  }
  if (label.includes("签到") || link.includes("user_sgin")) {
    switchTab("sign");
    return true;
  }
  if (label.includes("收藏") || link.includes("user_goods_collection")) {
    switchTab("collection");
    return true;
  }
  if (label.includes("优惠") || label.includes("券") || label.includes("卡包") || label.includes("卡券") || label.includes("我的卡券") || link.includes("coupon")) {
    switchTab("userCoupons");
    return true;
  }
  if (label.includes("地址") || link.includes("user_address")) {
    openAddressBook(returnTab || "profile");
    return true;
  }
  if (label.includes("申请售后") || label.includes("申请退款") || link.includes("goods_return")) {
    orderId ? openRefund({ orderId }) : switchTab("refundList");
    return true;
  }
  if (label.includes("售后") || label.includes("退款") || label.includes("退货") || link.includes("user_return_list")) {
    switchTab("refundList");
    return true;
  }
  if (
    label.includes("客服")
    || label.includes("联系")
    || label.includes("帮助")
    || label.includes("服务中心")
    || label.includes("常见问题")
    || label.includes("售后政策")
    || link.includes("/pages/users/kefu")
    || link.includes("/pages/users/web_page")
    || link.includes("/pages/service")
  ) {
    openCustomerService(returnTab || "profile");
    return true;
  }
  if (
    label.includes("资讯")
    || label.includes("文章")
    || label.includes("新闻")
    || label.includes("公告")
    || label.includes("消息")
    || label.includes("通知")
    || label.includes("关于我们")
    || label.includes("平台介绍")
    || link.includes("/pages/news/")
  ) {
    switchTab("articles");
    return true;
  }
  if (label.includes("会员") || label.includes("等级") || label.includes("成长值") || link.includes("user_vip") || link.includes("level")) {
    switchTab("memberLevel");
    return true;
  }
  if (
    label.includes("提现记录")
    || (link.includes("user_spread_money") && link.includes("type=1"))
    || link.includes("extract_record")
    || link.includes("extract-record")
    || link.includes("cash_record")
  ) {
    openExtractRecords();
    return true;
  }
  if (label.includes("提现") || link.includes("user_cash") || link.includes("extract_cash") || link.includes("extract-cash")) {
    openExtractCash();
    return true;
  }
  if (label.includes("佣金排行") || label.includes("收益排行") || link.includes("commission_rank") || link.includes("commission-rank") || link.includes("brokerage_rank")) {
    openBrokerageRank();
    return true;
  }
  if (label.includes("推广排行") || label.includes("分销排行") || link.includes("promoter_rank") || link.includes("promoter-rank") || link.includes("spread_rank")) {
    openSpreadRank();
    return true;
  }
  if (label.includes("佣金") || label.includes("收益") || link.includes("user_spread_money") || link.includes("commission_record") || link.includes("brokerage_record")) {
    openBrokerageRecords();
    return true;
  }
  if ((label.includes("推广") && label.includes("订单")) || link.includes("promoter-order") || link.includes("promoter_order") || link.includes("spread_order")) {
    openSpreadOrders();
    return true;
  }
  if (
    label.includes("推广人")
    || label.includes("推广用户")
    || label.includes("我的团队")
    || label.includes("团队")
    || label.includes("粉丝")
    || label.includes("下级")
    || link.includes("promoter-list")
    || link.includes("promoter_list")
    || link.includes("spread_people")
    || link.includes("spread-list")
  ) {
    switchTab("spreadPeople");
    return true;
  }
  if (
    label.includes("推广名片")
    || label.includes("推广海报")
    || label.includes("分享海报")
    || label.includes("二维码")
    || link.includes("user_spread_code")
    || link.includes("spread_code")
    || link.includes("spread_poster")
    || link.includes("promotion-card")
  ) {
    openSpreadPoster();
    return true;
  }
  if (label.includes("推广") || label.includes("分销") || label.includes("邀请好友") || link.includes("/pages/promoter/index") || link.includes("/pages/spread/index") || link.includes("user_spread")) {
    switchTab("spread");
    return true;
  }
  return false;
}

function menuOrderIdFromLink(link) {
  if (!link) {
    return "";
  }
  const parsed = parseLegacyUrl(link);
  return legacyUrlValue(parsed, "order_id", "orderId", "orderNo", "id");
}

function menuProductIdFromLink(link) {
  if (!link) {
    return "";
  }
  const parsed = parseLegacyUrl(link);
  return legacyUrlValue(parsed, "productId", "product_id", "id");
}

function openLegacyOrderUrl(url) {
  if (!url) {
    return false;
  }
  const parsed = parseLegacyUrl(url);
  const pathname = parsed.pathname;
  const id = legacyUrlValue(parsed, "order_id", "orderId", "orderNo", "id");
  if (pathname.includes("/pages/order/order_payment/index")) {
    if (id) {
      openPaymentPage(id);
    } else {
      showToast("缺少订单号无法付款");
    }
    return true;
  }
  if (pathname.includes("/pages/order/order_pay_status/index")) {
    if (id) {
      loadPayStatus(id);
    } else {
      showToast("缺少订单号无法查看支付状态");
    }
    return true;
  }
  if (pathname.includes("/pages/order/order_details/index")) {
    if (id) {
      openOrderDetail({ orderId: id }, parsed.params.get("isReturen") === "1" || parsed.params.get("isReturn") === "1");
    } else {
      switchTab("profile");
    }
    return true;
  }
  if (pathname.includes("/pages/goods/goods_return/index")) {
    if (id) {
      openRefund({ orderId: id });
    } else {
      switchTab("refundList");
    }
    return true;
  }
  return false;
}

function legacyMenuUrl(item) {
  if (!item) {
    return "";
  }
  const direct = item.url
    || item.wap_url
    || item.wapUrl
    || item.link
    || item.linkUrl
    || item.link_url
    || item.wap_link
    || item.wapLink
    || item.uniapp_url
    || item.uniappUrl
    || item.path
    || item.href
    || item.fallbackLink
    || item.value;
  if (direct) {
    return String(direct);
  }
  if (Array.isArray(item.info)) {
    const linkField = item.info.find((field) => {
      const label = String(field?.title || field?.name || field?.key || "").toLowerCase();
      return label.includes("link") || label.includes("url") || label.includes("链接");
    }) || item.info[1];
    if (linkField?.value) {
      return String(linkField.value);
    }
  }
  return "";
}

function openExternalUrl(url) {
  const externalUrl = normalizeExternalUrl(url);
  if (!externalUrl) {
    return false;
  }
  window.open(externalUrl, "_blank", "noopener");
  return true;
}

function normalizeExternalUrl(url) {
  const raw = String(url || "").trim();
  if (/^https?:\/\//i.test(raw)) {
    return raw;
  }
  let candidate = raw;
  for (let index = 0; index < 2; index += 1) {
    const decoded = safeDecodeLegacyUrl(candidate);
    if (!decoded || decoded === candidate) {
      break;
    }
    if (/^https?:\/\//i.test(decoded)) {
      return decoded;
    }
    candidate = decoded;
  }
  return "";
}

function openAdminMobileRoute(path) {
  const base = (import.meta.env.VITE_ADMIN_WEB_BASE || "http://127.0.0.1:19527").replace(/\/+$/, "");
  const route = String(path || "").startsWith("/") ? path : `/${path || ""}`;
  window.open(`${base}${route}`, "_blank", "noopener");
}

function openLegacyCoveredUrl(url, returnTab = currentTab.value) {
  if (!url) {
    return false;
  }
  const parsed = parseLegacyUrl(url);
  const tab = matchedLegacyTab(parsed.pathname, parsed.params);
  if (!tab) {
    return false;
  }
  openLegacyTab(tab, parsed, returnTab);
  return true;
}

function parseLegacyUrl(url) {
  const normalized = normalizeLegacyUrl(url);
  try {
    const parsed = new URL(normalized, window.location.origin);
    const hashRoute = parseLegacyHashRoute(parsed.hash);
    if (hashRoute) {
      return hashRoute;
    }
    return { pathname: parsed.pathname, params: parsed.searchParams };
  } catch {
    const [pathname = "", search = ""] = normalized.split("?");
    return { pathname, params: new URLSearchParams(search) };
  }
}

function normalizeLegacyUrl(url) {
  const raw = String(url || "").trim();
  if (!raw) {
    return "";
  }
  if (/^\/?pages\//.test(raw)) {
    return raw.startsWith("pages/") ? `/${raw}` : raw;
  }
  let candidate = raw;
  for (let index = 0; index < 3; index += 1) {
    const decoded = safeDecodeLegacyUrl(candidate);
    if (!decoded || decoded === candidate) {
      break;
    }
    if (/^https?:\/\//i.test(decoded) && !/^https?:\/\//i.test(raw)) {
      break;
    }
    candidate = decoded;
    if (isLegacyInternalUrl(candidate)) {
      return candidate.startsWith("pages/") ? `/${candidate}` : candidate;
    }
  }
  return isLegacyInternalUrl(candidate) && candidate.startsWith("pages/") ? `/${candidate}` : candidate;
}

function safeDecodeLegacyUrl(value) {
  try {
    return decodeURIComponent(String(value).replace(/\+/g, "%20"));
  } catch {
    return "";
  }
}

function isLegacyInternalUrl(value) {
  const text = String(value || "").trim();
  return /^\/?pages\//.test(text) || text.includes("#/pages/");
}

function parseLegacyHashRoute(hash) {
  const rawHash = String(hash || "").replace(/^#/, "");
  if (!/^\/?pages\//.test(rawHash)) {
    return null;
  }
  const hashRoute = rawHash.startsWith("pages/") ? `/${rawHash}` : rawHash;
  const [pathname = "", search = ""] = hashRoute.split("?");
  return { pathname, params: new URLSearchParams(search) };
}

function legacyUrlValue(parsed, ...names) {
  for (const name of names) {
    const value = parsed.params.get(name);
    if (value) {
      return value;
    }
  }
  return "";
}

function openLegacyTab(tab, parsed, returnTab) {
  const legacyStatus = legacyOrderStatus(parsed.pathname, parsed.params);
  if (tab === "profile" && legacyStatus !== undefined) {
    orderType.value = legacyStatus;
  }
  if (tab === "orders") {
    openOrders(legacyStatus === undefined ? null : legacyStatus);
    return;
  }
  if (tab === "adminMobileOrder") {
    openAdminMobileRoute("/javaMobile/orderList");
    return;
  }
  if (tab === "adminMobileCancellation") {
    openAdminMobileRoute("/javaMobile/orderCancellation");
    return;
  }
  if (tab === "productReplies") {
    openProductReplies(legacyUrlValue(parsed, "productId", "id"));
    return;
  }
  if (tab === "comment") {
    openCommentFromLegacyUrl(parsed);
    return;
  }
  if (tab === "logistics") {
    const orderId = legacyUrlValue(parsed, "orderId", "order_id", "id");
    orderId ? openLogistics({ orderId }) : switchTab("profile");
    return;
  }
  if (tab === "seckillDetail") {
    const id = legacyUrlValue(parsed, "id", "seckillId");
    id ? openSeckillDetail({ id }) : switchTab("seckill");
    return;
  }
  if (tab === "combinationDetail") {
    const id = legacyUrlValue(parsed, "id", "combinationId");
    id ? openCombinationDetail({ id, pinkId: legacyUrlValue(parsed, "pinkId") }) : switchTab("combination");
    return;
  }
  if (tab === "combinationStatus") {
    openLegacyCombinationStatus(parsed);
    return;
  }
  if (tab === "bargainDetail") {
    const id = legacyUrlValue(parsed, "id", "bargainId");
    id ? openBargainDetail({ id, bargainUserId: legacyUrlValue(parsed, "storeBargainId", "bargainUserId") }) : switchTab("bargain");
    return;
  }
  if (tab === "activityPoster") {
    openLegacyActivityPoster(parsed);
    return;
  }
  if (tab === "activityOrders") {
    activityOrderType.value = legacyActivityOrderType(parsed.pathname, parsed.params);
    openActivityOrders(activityOrderType.value);
    return;
  }
  if (tab === "checkout") {
    openLegacyCheckoutFromParsed(parsed);
    return;
  }
  if (tab === "refund") {
    const orderId = legacyUrlValue(parsed, "orderId", "order_id", "id");
    orderId ? openRefund({ orderId }) : switchTab("refundList");
    return;
  }
  if (tab === "paymentPage") {
    const orderNo = legacyUrlValue(parsed, "orderNo", "order_id", "orderId");
    orderNo ? openPaymentPage(orderNo) : switchTab("profile");
    return;
  }
  if (tab === "payStatus") {
    const orderNo = legacyUrlValue(parsed, "orderNo", "order_id", "orderId");
    orderNo ? loadPayStatus(orderNo) : switchTab("profile");
    return;
  }
  if (tab === "orderDetail") {
    const orderId = legacyUrlValue(parsed, "orderId", "order_id", "id");
    orderId ? openOrderDetail({ orderId }, parsed.params.get("isReturen") === "1" || parsed.params.get("isReturn") === "1") : switchTab("profile");
    return;
  }
  if (tab === "articleDetail") {
    const id = legacyUrlValue(parsed, "id", "articleId");
    id ? openArticleDetail({ id }) : switchTab("articles");
    return;
  }
  if (tab === "customerService") {
    customerReturnTab.value = returnTab || "profile";
    const webUrl = legacyUrlValue(parsed, "webUel", "webUrl", "url");
    const title = legacyUrlValue(parsed, "title") || (webUrl ? "网页内容" : "联系客服");
    openCustomerService(customerReturnTab.value, webUrl, title);
    return;
  }
  if (tab === "legacySafe") {
    legacySafeContext.value = {
      pathname: parsed.pathname,
      params: Object.fromEntries(parsed.params.entries()),
      returnTab: returnTab || "profile"
    };
    currentTab.value = "legacySafe";
    return;
  }
  if (tab === "storeList") {
    currentTab.value = "storeList";
    loadStoreList();
    return;
  }
  if (tab === "address") {
    openLegacyAddressBookFromParsed(parsed, returnTab || "profile");
    return;
  }
  if (tab === "extractRecords") {
    openExtractRecords();
    return;
  }
  if (tab === "brokerageRecords") {
    openBrokerageRecords();
    return;
  }
  if (tab === "extractCash") {
    openExtractCash();
    return;
  }
  if (tab === "spreadOrders") {
    openSpreadOrders();
    return;
  }
  if (tab === "spreadPoster") {
    openSpreadPoster();
    return;
  }
  if (tab === "spreadRank") {
    openSpreadRank();
    return;
  }
  if (tab === "brokerageRank") {
    openBrokerageRank();
    return;
  }
  if (tab === "bill") {
    const type = parsed.params.get("type");
    if (["all", "expenditure", "income"].includes(type || "")) {
      billType.value = type;
    }
  }
  if (tab === "balance" && parsed.pathname.includes("/pages/users/user_payment/index")) {
    currentTab.value = "balance";
    openRechargePanel();
    return;
  }
  if (tab === "category" || tab === "home") {
    if (isLegacyProductDetailPath(parsed.pathname)) {
      const id = legacyUrlValue(parsed, "productId", "id");
      id ? openDetail({ id }) : switchTab("home");
      return;
    }
    if (parsed.pathname.includes("/pages/activity/promotionList/index")) {
      applyLegacyPromotionFilters(parsed);
      switchTab("home");
      return;
    }
    const searchValue = legacyUrlValue(parsed, "searchValue", "keyword");
    const cid = legacyUrlValue(parsed, "cid", "cateId");
    const title = legacyUrlValue(parsed, "title", "name");
    if (searchValue) keyword.value = searchValue;
    if (cid) activeCid.value = cid;
    legacyProductRank.value = false;
    activeType.value = Number(parsed.params.get("type") || 0);
    legacyGoodsTitle.value = title || (searchValue ? `搜索「${searchValue}」` : "");
  }
  switchTab(tab);
}

function applyLegacyPromotionFilters(parsed) {
  const title = legacyUrlValue(parsed, "title", "name");
  keyword.value = "";
  activeCid.value = "";
  if (legacyPromotionRank(parsed.pathname, parsed.params)) {
    activeType.value = 0;
    legacyProductRank.value = true;
    legacyGoodsTitle.value = title || "商品排行";
    return;
  }
  activeType.value = legacyPromotionType(parsed.pathname, parsed.params);
  legacyProductRank.value = false;
  legacyGoodsTitle.value = title;
}

function isLegacyProductDetailPath(pathname) {
  return pathname.includes("/pages/goods/goods_details/index")
    || pathname.includes("/pages/goods_details/index");
}

async function openLegacyCombinationStatus(parsed) {
  const pinkId = legacyUrlValue(parsed, "pinkId", "id");
  const combinationId = legacyUrlValue(parsed, "combinationId", "combination_id", "cid");
  if (pinkId) {
    const detail = await openCombinationPinkDetail(pinkId);
    if (detail) {
      pinkManualCopyLink.value = combinationShareUrl(pinkId, detail.id || detail.combinationId || combinationId);
    }
    return;
  }
  if (combinationId) {
    await openCombinationDetail({ id: combinationId, pinkId });
    return;
  }
  currentTab.value = "combination";
  showToast("缺少拼团编号");
}

async function openLegacyActivityPoster(parsed) {
  const type = Number(legacyUrlValue(parsed, "type") || 0);
  const id = legacyUrlValue(parsed, "id", "activityId", "pinkId", "bargainUserId");
  if (type === 1) {
    await openBargainDetail({ id, bargainUserId: id });
    await shareBargainLink();
    return;
  }
  if (id) {
    await openLegacyCombinationStatus({ pathname: "/pages/activity/goods_combination_status/index", params: new URLSearchParams({ id }) });
    const link = pinkManualCopyLink.value || combinationShareUrl(id, selectedCombination.value?.id);
    if (link) {
      try {
        await navigator.clipboard.writeText(link);
        pinkManualCopyLink.value = "";
        showToast("拼团邀请链接已复制");
      } catch {
        pinkManualCopyLink.value = link;
        showToast("已显示拼团邀请链接，请长按或全选复制");
      }
    }
    return;
  }
  switchTab("combination");
  showToast("缺少活动海报参数");
}

function legacyOrderStatus(pathname, params) {
  if (!pathname.includes("/pages/users/order_list/index")) {
    return undefined;
  }
  const value = params.get("status") || params.get("type") || "";
  if (value === "") {
    return null;
  }
  const status = Number(value);
  return [0, 1, 2, 3, -3].includes(status) ? status : null;
}

function openCommentFromLegacyUrl(parsed) {
  commentOrder.value = {
    orderId: legacyUrlValue(parsed, "orderId", "order_id"),
    unique: legacyUrlValue(parsed, "unique", "uni")
  };
  commentGoods.value = {
    unique: legacyUrlValue(parsed, "unique", "uni"),
    infoId: legacyUrlValue(parsed, "id", "infoId")
  };
  currentTab.value = "comment";
}

async function openSpreadOrders() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "spreadOrders";
  await loadSpreadOrders();
}

async function openSpreadPoster() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "spreadPoster";
  await loadSpreadPoster();
}

async function openSpreadRank() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "spreadRank";
  await loadSpreadRank();
}

async function openBrokerageRank() {
  if (!authToken.value) {
    showToast("请先登录");
    currentTab.value = "profile";
    return;
  }
  currentTab.value = "brokerageRank";
  await loadBrokerageRank();
}

async function openOrders(type = orderType.value) {
  orderType.value = type === undefined ? null : type;
  if (!authToken.value) {
    currentTab.value = "orders";
    showToast("请先登录");
    return;
  }
  currentTab.value = "orders";
  await loadOrders();
}

function switchTab(tab) {
  currentTab.value = tab;
  if (tab === "home") {
    activeCid.value = "";
    loadProducts();
    return;
  }
  if (tab === "search") {
    loadSearchHotKeywords();
    loadProducts();
    return;
  }
  if (tab === "category" && flatCategories.value.length && !activeCid.value) {
    selectCategory(flatCategories.value[0].id);
    return;
  }
  if (tab === "cart") {
    loadCart();
  }
  if (tab === "profile" && authToken.value) {
    loadUserInfo();
    loadOrders();
    loadAddresses();
  }
  if (tab === "orders") {
    loadOrders();
  }
  if (tab === "userInfo" && authToken.value) {
    loadUserInfo();
  }
  if (tab === "collection") {
    collectManage.value = false;
    loadCollects();
  }
  if (tab === "userCoupons") {
    userCouponType.value = "usable";
    loadUserCoupons();
  }
  if (tab === "couponCenter") {
    couponCenterType.value = 1;
    loadCouponCenter();
  }
  if (tab === "articles") {
    loadArticles();
  }
  if (tab === "customerService") {
    openCustomerService(customerReturnTab.value);
  }
  if (tab === "seckill") {
    loadSeckill();
  }
  if (tab === "combination") {
    loadCombination();
  }
  if (tab === "bargain") {
    loadBargain();
  }
  if (tab === "bargainRecords") {
    loadBargainRecords();
  }
  if (tab === "activityOrders") {
    loadActivityOrders();
  }
  if (tab === "refundList") {
    loadRefundOrders();
  }
  if (tab === "balance") {
    loadBalance();
  }
  if (tab === "bill") {
    loadBills();
  }
  if (tab === "integral") {
    loadIntegral();
  }
  if (tab === "sign") {
    loadSignCenter();
  }
  if (tab === "signRecords") {
    openSignRecords();
  }
  if (tab === "memberLevel") {
    loadMemberLevel();
  }
  if (tab === "spread") {
    loadSpreadCenter();
  }
  if (tab === "brokerageRecords") {
    loadBrokerageRecords();
  }
  if (tab === "extractRecords") {
    loadExtractRecords();
  }
  if (tab === "extractCash") {
    loadExtractCash();
  }
  if (tab === "spreadPeople") {
    loadSpreadPeople();
  }
  if (tab === "spreadOrders") {
    loadSpreadOrders();
  }
  if (tab === "spreadPoster") {
    loadSpreadPoster();
  }
  if (tab === "spreadRank") {
    loadSpreadRank();
  }
  if (tab === "brokerageRank") {
    loadBrokerageRank();
  }
  if (tab === "profile") {
    recordVisit(4);
  }
}

function selectCategory(id) {
  currentTab.value = "category";
  activeCid.value = String(id);
  activeType.value = 0;
  legacyProductRank.value = false;
  legacyGoodsTitle.value = "";
  loadProducts();
}

function selectType(type) {
  currentTab.value = "home";
  activeType.value = type;
  activeCid.value = "";
  legacyProductRank.value = false;
  legacyGoodsTitle.value = "";
  const layoutTab = activeLayoutTab(type);
  if (layoutTab?.url) {
    openHomeDiyLink(layoutTab);
    return;
  }
  loadProducts();
}

function selectOrderType(type) {
  orderType.value = type;
  loadOrders();
}

function selectRefundStatus(status) {
  refundStatus.value = status;
  loadRefundOrders();
}

function searchProducts() {
  openSearch();
}

async function openSearch() {
  currentTab.value = "search";
  activeCid.value = "";
  activeType.value = 0;
  legacyProductRank.value = false;
  legacyGoodsTitle.value = "";
  await Promise.all([loadSearchHotKeywords(), loadProducts()]);
}

async function submitSearch() {
  activeCid.value = "";
  activeType.value = 0;
  legacyProductRank.value = false;
  legacyGoodsTitle.value = "";
  await loadProducts();
}

async function searchByHotKeyword(value) {
  keyword.value = String(value || "").trim();
  await submitSearch();
}

function showToast(message) {
  toast.value = message;
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    toast.value = "";
  }, 1800);
}

function openConfirm(options = {}) {
  if (confirmDialog.value.resolver) {
    confirmDialog.value.resolver(false);
  }
  return new Promise((resolve) => {
    confirmDialog.value = {
      open: true,
      title: options.title || "确认操作",
      message: options.message || "确定继续吗？",
      confirmText: options.confirmText || "确定",
      cancelText: options.cancelText || "取消",
      resolver: resolve
    };
  });
}

function resolveConfirm(value) {
  const resolver = confirmDialog.value.resolver;
  confirmDialog.value = {
    open: false,
    title: "",
    message: "",
    confirmText: "确定",
    cancelText: "取消",
    resolver: null
  };
  if (resolver) {
    resolver(Boolean(value));
  }
}

onMounted(async () => {
  window.addEventListener("error", hideBrokenLegacyImage, true);
  await Promise.all([loadHome(), loadProfileMenus()]);
  applyLegacyGoodsFilters();
  await loadProducts();
  if (isLegacyProductDetailPath(window.location.pathname) && initialProductId) {
    await openDetail({ id: initialProductId });
  }
  if (authToken.value) {
    await Promise.all([loadUserInfo(), loadCart(), loadOrders(), loadAddresses(), loadUserCoupons(), loadBalance(), loadIntegral()]);
    await bindCachedSpread();
  }
  if (currentTab.value === "profile") {
    recordVisit(4);
  }
  if (currentTab.value === "search") {
    await loadSearchHotKeywords();
  }
  if (currentTab.value === "seckillDetail" && initialSeckillId) {
    await openSeckillDetail({ id: initialSeckillId });
  } else if (currentTab.value === "seckill") {
    await loadSeckill();
  } else if (currentTab.value === "combinationDetail" && initialCombinationId) {
    await openCombinationDetail({ id: initialCombinationId, pinkId: initialCombinationPinkId });
  } else if (currentTab.value === "combination") {
    await loadCombination();
  } else if (currentTab.value === "bargainDetail" && initialBargainId) {
    await openBargainDetail({ id: initialBargainId, bargainUserId: initialBargainUserId });
  } else if (currentTab.value === "bargain") {
    await loadBargain();
  } else if (currentTab.value === "bargainRecords") {
    await loadBargainRecords();
  } else if (currentTab.value === "activityOrders") {
    await loadActivityOrders();
  } else if (currentTab.value === "checkout") {
    await openLegacyCheckout();
  } else if (currentTab.value === "refundList") {
    await loadRefundOrders();
  } else if (currentTab.value === "refund" && initialOrderId) {
    await openRefund({ orderId: initialOrderId });
  } else if (currentTab.value === "paymentPage" && initialPaymentOrderNo) {
    await openPaymentPage(initialPaymentOrderNo);
  } else if (currentTab.value === "payStatus" && initialPaymentOrderNo) {
    await loadPayStatus(initialPaymentOrderNo);
  } else if (currentTab.value === "orderDetail" && initialOrderId) {
    await openOrderDetail({ orderId: initialOrderId }, initialOrderIsReturn);
  } else if (currentTab.value === "logistics" && initialOrderId) {
    await openLogistics({ orderId: initialOrderId });
  } else if (currentTab.value === "comment") {
    await openLegacyComment();
  } else if (currentTab.value === "productReplies" && initialProductId) {
    await openProductReplies(initialProductId);
  } else if (currentTab.value === "articleDetail" && initialArticleId) {
    await Promise.all([loadArticles(), openArticleDetail({ id: initialArticleId })]);
  } else if (currentTab.value === "articles") {
    await loadArticles();
  } else if (currentTab.value === "smallPage") {
    await loadSmallPage();
  } else if (currentTab.value === "storeList") {
    await loadStoreList();
  } else if (currentTab.value === "combinationStatus") {
    await openLegacyCombinationStatus(parseLegacyUrl(window.location.pathname + window.location.search));
  } else if (currentTab.value === "activityPoster") {
    await openLegacyActivityPoster(parseLegacyUrl(window.location.pathname + window.location.search));
  } else if (currentTab.value === "customerService") {
    await openCustomerService("profile");
  } else if (currentTab.value === "address") {
    await openLegacyAddressBook();
  } else if (currentTab.value === "userInfo") {
    await loadUserInfo();
  } else if (currentTab.value === "userPhone") {
    await loadUserInfo();
    openPhoneChange();
  } else if (currentTab.value === "userPassword") {
    await loadUserInfo();
    openPasswordChange();
  } else if (currentTab.value === "collection") {
    await loadCollects();
  } else if (currentTab.value === "userCoupons") {
    await loadUserCoupons();
  } else if (currentTab.value === "integral") {
    await loadIntegral();
  } else if (currentTab.value === "bill") {
    await loadBills();
  } else if (currentTab.value === "balance") {
    await loadBalance();
    if (window.location.pathname.includes("/pages/users/user_payment/index")) {
      await openRechargePanel();
    }
  } else if (currentTab.value === "sign") {
    await loadSignCenter();
  } else if (currentTab.value === "signRecords") {
    await openSignRecords();
  } else if (currentTab.value === "memberLevel") {
    await loadMemberLevel();
  } else if (currentTab.value === "spread") {
    await loadSpreadCenter();
  } else if (currentTab.value === "brokerageRecords") {
    await openBrokerageRecords();
  } else if (currentTab.value === "extractRecords") {
    await openExtractRecords();
  } else if (currentTab.value === "extractCash") {
    await openExtractCash();
  } else if (currentTab.value === "spreadPeople") {
    await loadSpreadPeople();
  } else if (currentTab.value === "spreadOrders") {
    await openSpreadOrders();
  } else if (currentTab.value === "spreadPoster") {
    await openSpreadPoster();
  } else if (currentTab.value === "spreadRank") {
    await openSpreadRank();
  } else if (currentTab.value === "brokerageRank") {
    await openBrokerageRank();
  }
});

function hideBrokenLegacyImage(event) {
  const target = event.target;
  if (!(target instanceof HTMLImageElement)) {
    return;
  }
  target.dataset.broken = "true";
  target.alt = "";
}
</script>
