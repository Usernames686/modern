package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;

public class StoreOrderInfoResponse {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private OrderInfoDetail info;
    private String unique;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public OrderInfoDetail getInfo() {
        return info;
    }

    public void setInfo(OrderInfoDetail info) {
        this.info = info;
    }

    public String getUnique() {
        return unique;
    }

    public void setUnique(String unique) {
        this.unique = unique;
    }

    public static class OrderInfoDetail {
        private String image;
        private Integer productId;
        private Integer attrValueId;
        private String productName;
        private String sku;
        private BigDecimal price;
        private Integer payNum;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public Integer getAttrValueId() {
            return attrValueId;
        }

        public void setAttrValueId(Integer attrValueId) {
            this.attrValueId = attrValueId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getPayNum() {
            return payNum;
        }

        public void setPayNum(Integer payNum) {
            this.payNum = payNum;
        }
    }
}
