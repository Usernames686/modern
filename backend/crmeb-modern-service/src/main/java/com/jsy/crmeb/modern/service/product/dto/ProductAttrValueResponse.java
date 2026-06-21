package com.jsy.crmeb.modern.service.product.dto;

import java.math.BigDecimal;

public class ProductAttrValueResponse {
    private Integer id;
    private Integer productId;
    private String suk;
    private Integer stock;
    private Integer sales;
    private BigDecimal price;
    private String image;
    private BigDecimal cost;
    private BigDecimal otPrice;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal brokerage;
    private BigDecimal brokerageTwo;
    private Integer type;
    private Integer quota;
    private Integer quotaShow;
    private String attrValue;
    private String barCode;
    private Integer addStock = 0;
    private Integer version;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSuk() {
        return suk;
    }

    public void setSuk(String suk) {
        this.suk = suk;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getOtPrice() {
        return otPrice;
    }

    public void setOtPrice(BigDecimal otPrice) {
        this.otPrice = otPrice;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(BigDecimal brokerage) {
        this.brokerage = brokerage;
    }

    public BigDecimal getBrokerageTwo() {
        return brokerageTwo;
    }

    public void setBrokerageTwo(BigDecimal brokerageTwo) {
        this.brokerageTwo = brokerageTwo;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Integer getQuotaShow() {
        return quotaShow;
    }

    public void setQuotaShow(Integer quotaShow) {
        this.quotaShow = quotaShow;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public Integer getAddStock() {
        return addStock;
    }

    public void setAddStock(Integer addStock) {
        this.addStock = addStock;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
