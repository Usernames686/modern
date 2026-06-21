package com.jsy.crmeb.modern.service.product.dto;

import java.util.List;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;

public class StoreProductInfoResponse extends StoreProductResponse {
    private String sliderImage;
    private String storeInfo;
    private String videoLink;
    private String unitName;
    private Integer giveIntegral;
    private Integer tempId;
    private Boolean specType;
    private Boolean isSub;
    private Boolean isGood;
    private List<String> activity;
    private List<ProductAttrResponse> attr;
    private List<ProductAttrValueResponse> attrValue;
    private String content;
    private List<Integer> couponIds;
    private List<StoreCoupon> coupons;

    public String getSliderImage() {
        return sliderImage;
    }

    public void setSliderImage(String sliderImage) {
        this.sliderImage = sliderImage;
    }

    public String getStoreInfo() {
        return storeInfo;
    }

    public void setStoreInfo(String storeInfo) {
        this.storeInfo = storeInfo;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Integer getGiveIntegral() {
        return giveIntegral;
    }

    public void setGiveIntegral(Integer giveIntegral) {
        this.giveIntegral = giveIntegral;
    }

    public Integer getTempId() {
        return tempId;
    }

    public void setTempId(Integer tempId) {
        this.tempId = tempId;
    }

    public Boolean getSpecType() {
        return specType;
    }

    public void setSpecType(Boolean specType) {
        this.specType = specType;
    }

    public Boolean getIsSub() {
        return isSub;
    }

    public void setIsSub(Boolean isSub) {
        this.isSub = isSub;
    }

    public Boolean getIsGood() {
        return isGood;
    }

    public void setIsGood(Boolean isGood) {
        this.isGood = isGood;
    }

    public List<String> getActivity() {
        return activity;
    }

    public void setActivity(List<String> activity) {
        this.activity = activity;
    }

    public List<ProductAttrResponse> getAttr() {
        return attr;
    }

    public void setAttr(List<ProductAttrResponse> attr) {
        this.attr = attr;
    }

    public List<ProductAttrValueResponse> getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(List<ProductAttrValueResponse> attrValue) {
        this.attrValue = attrValue;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Integer> getCouponIds() {
        return couponIds;
    }

    public void setCouponIds(List<Integer> couponIds) {
        this.couponIds = couponIds;
    }

    public List<StoreCoupon> getCoupons() {
        return coupons;
    }

    public void setCoupons(List<StoreCoupon> coupons) {
        this.coupons = coupons;
    }
}
