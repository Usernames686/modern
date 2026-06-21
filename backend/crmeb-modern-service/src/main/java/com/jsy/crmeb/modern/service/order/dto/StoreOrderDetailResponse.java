package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoreOrderDetailResponse {
    private Integer id;
    private String orderId;
    private Integer uid;
    private BigDecimal totalPrice;
    private Integer totalNum;
    private BigDecimal totalPostage;
    private BigDecimal payPrice;
    private BigDecimal payPostage;
    private String payType;
    private LocalDateTime createTime;
    private Integer status;
    private List<StoreOrderInfoResponse> productList = new ArrayList<>();
    private Map<String, String> statusStr;
    private String payTypeStr;
    private Boolean isDel;
    private Integer refundStatus;
    private BigDecimal refundPrice;
    private String refundReasonWapImg;
    private String refundReasonWapExplain;
    private LocalDateTime refundReasonTime;
    private String refundReasonWap;
    private String refundReason;
    private String orderType;
    private String remark;
    private String realName;
    private BigDecimal proTotalPrice;
    private BigDecimal couponPrice;
    private Boolean paid;
    private Integer type;
    private Boolean isAlterPrice;
    private String verifyCode;
    private Integer shippingType;
    private Integer storeId;
    private String userPhone;
    private String userAddress;
    private String mark;
    private String deliveryName;
    private String deliveryType;
    private String deliveryId;
    private String deliveryCode;
    private Integer expressRecordType;
    private LocalDateTime deliveryTime;

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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
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

    public List<StoreOrderInfoResponse> getProductList() {
        return productList;
    }

    public void setProductList(List<StoreOrderInfoResponse> productList) {
        this.productList = productList;
    }

    public Map<String, String> getStatusStr() {
        return statusStr;
    }

    public void setStatusStr(Map<String, String> statusStr) {
        this.statusStr = statusStr;
    }

    public String getPayTypeStr() {
        return payTypeStr;
    }

    public void setPayTypeStr(String payTypeStr) {
        this.payTypeStr = payTypeStr;
    }

    public Boolean getIsDel() {
        return isDel;
    }

    public void setIsDel(Boolean isDel) {
        this.isDel = isDel;
    }

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
    }

    public BigDecimal getRefundPrice() {
        return refundPrice;
    }

    public void setRefundPrice(BigDecimal refundPrice) {
        this.refundPrice = refundPrice;
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

    public LocalDateTime getRefundReasonTime() {
        return refundReasonTime;
    }

    public void setRefundReasonTime(LocalDateTime refundReasonTime) {
        this.refundReasonTime = refundReasonTime;
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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
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

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Boolean getIsAlterPrice() {
        return isAlterPrice;
    }

    public void setIsAlterPrice(Boolean isAlterPrice) {
        this.isAlterPrice = isAlterPrice;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
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

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
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

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }
}
