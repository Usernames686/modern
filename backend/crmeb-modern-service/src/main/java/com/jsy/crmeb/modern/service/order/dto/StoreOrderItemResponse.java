package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoreOrderItemResponse {
    private Integer id;
    private String orderId;
    private Integer uid;
    private String realName;
    private String userPhone;
    private BigDecimal totalPrice;
    private BigDecimal payPrice;
    private Boolean paid;
    private LocalDateTime payTime;
    private String payType;
    private LocalDateTime createTime;
    private Integer status;
    private String storeName;
    private String clerkName;
    private List<StoreOrderInfoResponse> productList = new ArrayList<>();
    private Map<String, String> statusStr;
    private String payTypeStr;
    private BigDecimal totalPostage;
    private BigDecimal payPostage;
    private Boolean isDel;
    private Boolean isSystemDel;
    private String remark;
    private BigDecimal refundPrice;
    private Integer refundStatus;
    private Integer totalNum;
    private Integer shippingType;
    private String verifyCode;
    private StoreOrderSpreadInfoResponse spreadInfo = new StoreOrderSpreadInfoResponse();
    private String orderType;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public BigDecimal getPayPrice() { return payPrice; }
    public void setPayPrice(BigDecimal payPrice) { this.payPrice = payPrice; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getClerkName() { return clerkName; }
    public void setClerkName(String clerkName) { this.clerkName = clerkName; }
    public List<StoreOrderInfoResponse> getProductList() { return productList; }
    public void setProductList(List<StoreOrderInfoResponse> productList) { this.productList = productList; }
    public Map<String, String> getStatusStr() { return statusStr; }
    public void setStatusStr(Map<String, String> statusStr) { this.statusStr = statusStr; }
    public String getPayTypeStr() { return payTypeStr; }
    public void setPayTypeStr(String payTypeStr) { this.payTypeStr = payTypeStr; }
    public BigDecimal getTotalPostage() { return totalPostage; }
    public void setTotalPostage(BigDecimal totalPostage) { this.totalPostage = totalPostage; }
    public BigDecimal getPayPostage() { return payPostage; }
    public void setPayPostage(BigDecimal payPostage) { this.payPostage = payPostage; }
    public Boolean getIsDel() { return isDel; }
    public void setIsDel(Boolean isDel) { this.isDel = isDel; }
    public Boolean getIsSystemDel() { return isSystemDel; }
    public void setIsSystemDel(Boolean isSystemDel) { this.isSystemDel = isSystemDel; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getRefundPrice() { return refundPrice; }
    public void setRefundPrice(BigDecimal refundPrice) { this.refundPrice = refundPrice; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public Integer getTotalNum() { return totalNum; }
    public void setTotalNum(Integer totalNum) { this.totalNum = totalNum; }
    public Integer getShippingType() { return shippingType; }
    public void setShippingType(Integer shippingType) { this.shippingType = shippingType; }
    public String getVerifyCode() { return verifyCode; }
    public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }
    public StoreOrderSpreadInfoResponse getSpreadInfo() { return spreadInfo; }
    public void setSpreadInfo(StoreOrderSpreadInfoResponse spreadInfo) { this.spreadInfo = spreadInfo; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
}
