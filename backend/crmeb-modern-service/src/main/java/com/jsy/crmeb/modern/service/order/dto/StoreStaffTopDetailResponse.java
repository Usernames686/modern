package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;

public class StoreStaffTopDetailResponse {
    private long completeCount;
    private long evaluatedCount;
    private long monthCount;
    private BigDecimal monthPrice = BigDecimal.ZERO;
    private long orderCount;
    private long proCount;
    private BigDecimal proPrice = BigDecimal.ZERO;
    private long receivedCount;
    private long refundCount;
    private BigDecimal sumPrice = BigDecimal.ZERO;
    private long todayCount;
    private BigDecimal todayPrice = BigDecimal.ZERO;
    private long unpaidCount;
    private long unshippedCount;
    private long verificationCount;

    public long getCompleteCount() { return completeCount; }
    public void setCompleteCount(long completeCount) { this.completeCount = completeCount; }
    public long getEvaluatedCount() { return evaluatedCount; }
    public void setEvaluatedCount(long evaluatedCount) { this.evaluatedCount = evaluatedCount; }
    public long getMonthCount() { return monthCount; }
    public void setMonthCount(long monthCount) { this.monthCount = monthCount; }
    public BigDecimal getMonthPrice() { return monthPrice; }
    public void setMonthPrice(BigDecimal monthPrice) { this.monthPrice = monthPrice; }
    public long getOrderCount() { return orderCount; }
    public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    public long getProCount() { return proCount; }
    public void setProCount(long proCount) { this.proCount = proCount; }
    public BigDecimal getProPrice() { return proPrice; }
    public void setProPrice(BigDecimal proPrice) { this.proPrice = proPrice; }
    public long getReceivedCount() { return receivedCount; }
    public void setReceivedCount(long receivedCount) { this.receivedCount = receivedCount; }
    public long getRefundCount() { return refundCount; }
    public void setRefundCount(long refundCount) { this.refundCount = refundCount; }
    public BigDecimal getSumPrice() { return sumPrice; }
    public void setSumPrice(BigDecimal sumPrice) { this.sumPrice = sumPrice; }
    public long getTodayCount() { return todayCount; }
    public void setTodayCount(long todayCount) { this.todayCount = todayCount; }
    public BigDecimal getTodayPrice() { return todayPrice; }
    public void setTodayPrice(BigDecimal todayPrice) { this.todayPrice = todayPrice; }
    public long getUnpaidCount() { return unpaidCount; }
    public void setUnpaidCount(long unpaidCount) { this.unpaidCount = unpaidCount; }
    public long getUnshippedCount() { return unshippedCount; }
    public void setUnshippedCount(long unshippedCount) { this.unshippedCount = unshippedCount; }
    public long getVerificationCount() { return verificationCount; }
    public void setVerificationCount(long verificationCount) { this.verificationCount = verificationCount; }
}
