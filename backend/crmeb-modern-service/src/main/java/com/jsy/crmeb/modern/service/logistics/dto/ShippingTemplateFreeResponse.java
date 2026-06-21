package com.jsy.crmeb.modern.service.logistics.dto;

import java.math.BigDecimal;

public class ShippingTemplateFreeResponse {
    private String title;
    private BigDecimal number;
    private BigDecimal price;
    private String uniqid;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getNumber() {
        return number;
    }

    public void setNumber(BigDecimal number) {
        this.number = number;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUniqid() {
        return uniqid;
    }

    public void setUniqid(String uniqid) {
        this.uniqid = uniqid;
    }
}
