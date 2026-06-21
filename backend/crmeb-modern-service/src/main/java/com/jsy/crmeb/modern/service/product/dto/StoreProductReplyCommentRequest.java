package com.jsy.crmeb.modern.service.product.dto;

public class StoreProductReplyCommentRequest {
    private Integer ids;
    private String merchantReplyContent;

    public Integer getIds() {
        return ids;
    }

    public void setIds(Integer ids) {
        this.ids = ids;
    }

    public String getMerchantReplyContent() {
        return merchantReplyContent;
    }

    public void setMerchantReplyContent(String merchantReplyContent) {
        this.merchantReplyContent = merchantReplyContent;
    }
}
