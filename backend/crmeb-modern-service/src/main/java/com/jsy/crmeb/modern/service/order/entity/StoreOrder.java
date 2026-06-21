package com.jsy.crmeb.modern.service.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eb_store_order")
public class StoreOrder {
    @TableId
    private Integer id;
    @TableField("order_id")
    private String orderId;
    private Integer uid;
    @TableField("real_name")
    private String realName;
    @TableField("user_phone")
    private String userPhone;
    @TableField("user_address")
    private String userAddress;
    @TableField("total_num")
    private Integer totalNum;
    @TableField("total_price")
    private BigDecimal totalPrice;
    @TableField("total_postage")
    private BigDecimal totalPostage;
    @TableField("pay_price")
    private BigDecimal payPrice;
    @TableField("pay_postage")
    private BigDecimal payPostage;
    @TableField("pay_type")
    private String payType;
    @TableField("create_time")
    private LocalDateTime createTime;
    private Integer status;
    @TableField("pay_time")
    private LocalDateTime payTime;
    @TableField("refund_status")
    private Integer refundStatus;
    @TableField("refund_reason_wap_img")
    private String refundReasonWapImg;
    @TableField("refund_reason_wap_explain")
    private String refundReasonWapExplain;
    @TableField("refund_reason_wap")
    private String refundReasonWap;
    @TableField("refund_reason")
    private String refundReason;
    @TableField("refund_reason_time")
    private LocalDateTime refundReasonTime;
    @TableField("refund_price")
    private BigDecimal refundPrice;
    @TableField("is_del")
    private Integer isDel;
    @TableField("is_system_del")
    private Integer isSystemDel;
    @TableField("combination_id")
    private Integer combinationId;
    @TableField("seckill_id")
    private Integer seckillId;
    @TableField("bargain_id")
    private Integer bargainId;
    @TableField("verify_code")
    private String verifyCode;
    @TableField("clerk_id")
    private Integer clerkId;
    private String remark;
    private String mark;
    private Integer paid;
    @TableField("delivery_name")
    private String deliveryName;
    @TableField("delivery_type")
    private String deliveryType;
    @TableField("delivery_id")
    private String deliveryId;
    @TableField("delivery_code")
    private String deliveryCode;
    @TableField("express_record_type")
    private Integer expressRecordType;
    @TableField("shipping_type")
    private Integer shippingType;
    @TableField("store_id")
    private Integer storeId;
    private Integer type;
    @TableField("is_alter_price")
    private Integer isAlterPrice;
    @TableField("pro_total_price")
    private BigDecimal proTotalPrice;
    @TableField("coupon_price")
    private BigDecimal couponPrice;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public Integer getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getTotalPostage() {
        return totalPostage;
    }

    public void setTotalPostage(BigDecimal totalPostage) {
        this.totalPostage = totalPostage;
    }

    public BigDecimal getPayPrice() {
        return payPrice;
    }

    public void setPayPrice(BigDecimal payPrice) {
        this.payPrice = payPrice;
    }

    public BigDecimal getPayPostage() {
        return payPostage;
    }

    public void setPayPostage(BigDecimal payPostage) {
        this.payPostage = payPostage;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getRefundReasonWapImg() {
        return refundReasonWapImg;
    }

    public void setRefundReasonWapImg(String refundReasonWapImg) {
        this.refundReasonWapImg = refundReasonWapImg;
    }

    public String getRefundReasonWapExplain() {
        return refundReasonWapExplain;
    }

    public void setRefundReasonWapExplain(String refundReasonWapExplain) {
        this.refundReasonWapExplain = refundReasonWapExplain;
    }

    public String getRefundReasonWap() {
        return refundReasonWap;
    }

    public void setRefundReasonWap(String refundReasonWap) {
        this.refundReasonWap = refundReasonWap;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getRefundReasonTime() {
        return refundReasonTime;
    }

    public void setRefundReasonTime(LocalDateTime refundReasonTime) {
        this.refundReasonTime = refundReasonTime;
    }

    public BigDecimal getRefundPrice() {
        return refundPrice;
    }

    public void setRefundPrice(BigDecimal refundPrice) {
        this.refundPrice = refundPrice;
    }

    public Integer getIsDel() {
        return isDel;
    }

    public void setIsDel(Integer isDel) {
        this.isDel = isDel;
    }

    public Integer getIsSystemDel() {
        return isSystemDel;
    }

    public void setIsSystemDel(Integer isSystemDel) {
        this.isSystemDel = isSystemDel;
    }

    public Integer getCombinationId() {
        return combinationId;
    }

    public void setCombinationId(Integer combinationId) {
        this.combinationId = combinationId;
    }

    public Integer getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(Integer seckillId) {
        this.seckillId = seckillId;
    }

    public Integer getBargainId() {
        return bargainId;
    }

    public void setBargainId(Integer bargainId) {
        this.bargainId = bargainId;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public Integer getClerkId() {
        return clerkId;
    }

    public void setClerkId(Integer clerkId) {
        this.clerkId = clerkId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Integer getPaid() {
        return paid;
    }

    public void setPaid(Integer paid) {
        this.paid = paid;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getDeliveryCode() {
        return deliveryCode;
    }

    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }

    public Integer getExpressRecordType() {
        return expressRecordType;
    }

    public void setExpressRecordType(Integer expressRecordType) {
        this.expressRecordType = expressRecordType;
    }

    public Integer getShippingType() {
        return shippingType;
    }

    public void setShippingType(Integer shippingType) {
        this.shippingType = shippingType;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getIsAlterPrice() {
        return isAlterPrice;
    }

    public void setIsAlterPrice(Integer isAlterPrice) {
        this.isAlterPrice = isAlterPrice;
    }

    public BigDecimal getProTotalPrice() {
        return proTotalPrice;
    }

    public void setProTotalPrice(BigDecimal proTotalPrice) {
        this.proTotalPrice = proTotalPrice;
    }

    public BigDecimal getCouponPrice() {
        return couponPrice;
    }

    public void setCouponPrice(BigDecimal couponPrice) {
        this.couponPrice = couponPrice;
    }
}
