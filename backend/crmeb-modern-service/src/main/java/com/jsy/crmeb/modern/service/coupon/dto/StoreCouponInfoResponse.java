package com.jsy.crmeb.modern.service.coupon.dto;

import com.jsy.crmeb.modern.service.product.entity.Category;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import java.util.List;

public class StoreCouponInfoResponse {
    private StoreCouponSaveRequest coupon;
    private List<StoreProduct> product;
    private List<Category> category;

    public StoreCouponInfoResponse(StoreCouponSaveRequest coupon, List<StoreProduct> product, List<Category> category) {
        this.coupon = coupon;
        this.product = product;
        this.category = category;
    }

    public StoreCouponSaveRequest getCoupon() { return coupon; }
    public void setCoupon(StoreCouponSaveRequest coupon) { this.coupon = coupon; }
    public List<StoreProduct> getProduct() { return product; }
    public void setProduct(List<StoreProduct> product) { this.product = product; }
    public List<Category> getCategory() { return category; }
    public void setCategory(List<Category> category) { this.category = category; }
}
