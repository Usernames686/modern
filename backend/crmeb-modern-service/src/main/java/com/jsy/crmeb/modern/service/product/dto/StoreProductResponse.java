package com.jsy.crmeb.modern.service.product.dto;

import java.math.BigDecimal;

public class StoreProductResponse {
    private Integer id;
    private String image;
    private String storeName;
    private String keyword;
    private String barCode;
    private String cateId;
    private String cateValues;
    private BigDecimal price;
    private BigDecimal vipPrice;
    private BigDecimal otPrice;
    private Integer sort;
    private Integer sales;
    private Integer stock;
    private Boolean isShow;
    private Boolean isHot;
    private Boolean isBenefit;
    private Boolean isBest;
    private Boolean isNew;
    private Integer addTime;
    private Boolean isPostage;
    private Boolean isDel;
    private BigDecimal cost;
    private Integer ficti;
    private Integer browse;
    private Integer collectCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getCateId() {
        return cateId;
    }

    public void setCateId(String cateId) {
        this.cateId = cateId;
    }

    public String getCateValues() {
        return cateValues;
    }

    public void setCateValues(String cateValues) {
        this.cateValues = cateValues;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(BigDecimal vipPrice) {
        this.vipPrice = vipPrice;
    }

    public BigDecimal getOtPrice() {
        return otPrice;
    }

    public void setOtPrice(BigDecimal otPrice) {
        this.otPrice = otPrice;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getIsShow() {
        return isShow;
    }

    public void setIsShow(Boolean isShow) {
        this.isShow = isShow;
    }

    public Boolean getIsHot() {
        return isHot;
    }

    public void setIsHot(Boolean isHot) {
        this.isHot = isHot;
    }

    public Boolean getIsBenefit() {
        return isBenefit;
    }

    public void setIsBenefit(Boolean isBenefit) {
        this.isBenefit = isBenefit;
    }

    public Boolean getIsBest() {
        return isBest;
    }

    public void setIsBest(Boolean isBest) {
        this.isBest = isBest;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Integer getAddTime() {
        return addTime;
    }

    public void setAddTime(Integer addTime) {
        this.addTime = addTime;
    }

    public Boolean getIsPostage() {
        return isPostage;
    }

    public void setIsPostage(Boolean isPostage) {
        this.isPostage = isPostage;
    }

    public Boolean getIsDel() {
        return isDel;
    }

    public void setIsDel(Boolean isDel) {
        this.isDel = isDel;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Integer getFicti() {
        return ficti;
    }

    public void setFicti(Integer ficti) {
        this.ficti = ficti;
    }

    public Integer getBrowse() {
        return browse;
    }

    public void setBrowse(Integer browse) {
        this.browse = browse;
    }

    public Integer getCollectCount() {
        return collectCount;
    }

    public void setCollectCount(Integer collectCount) {
        this.collectCount = collectCount;
    }
}
