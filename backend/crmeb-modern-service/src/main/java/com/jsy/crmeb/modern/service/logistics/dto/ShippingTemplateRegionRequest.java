package com.jsy.crmeb.modern.service.logistics.dto;

import java.math.BigDecimal;

public class ShippingTemplateRegionRequest {
    private String cityId;
    private String title;
    private BigDecimal first;
    private BigDecimal firstPrice;
    private BigDecimal renewal;
    private BigDecimal renewalPrice;
    private String uniqid;

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getFirst() {
        return first;
    }

    public void setFirst(BigDecimal first) {
        this.first = first;
    }

    public BigDecimal getFirstPrice() {
        return firstPrice;
    }

    public void setFirstPrice(BigDecimal firstPrice) {
        this.firstPrice = firstPrice;
    }

    public BigDecimal getRenewal() {
        return renewal;
    }

    public void setRenewal(BigDecimal renewal) {
        this.renewal = renewal;
    }

    public BigDecimal getRenewalPrice() {
        return renewalPrice;
    }

    public void setRenewalPrice(BigDecimal renewalPrice) {
        this.renewalPrice = renewalPrice;
    }

    public String getUniqid() {
        return uniqid;
    }

    public void setUniqid(String uniqid) {
        this.uniqid = uniqid;
    }
}
