package com.jsy.crmeb.modern.service.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ProductAddStockRequest {
    @NotNull(message = "商品ID不能为空")
    private Integer id;
    @Valid
    @NotEmpty(message = "商品规格属性列表不能为空")
    private List<ProductAttrValueAddStockRequest> attrValueList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<ProductAttrValueAddStockRequest> getAttrValueList() {
        return attrValueList;
    }

    public void setAttrValueList(List<ProductAttrValueAddStockRequest> attrValueList) {
        this.attrValueList = attrValueList;
    }
}
