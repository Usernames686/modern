package com.jsy.crmeb.modern.service.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProductAttrValueAddStockRequest {
    @NotNull(message = "商品规格属性ID不能为空")
    private Integer id;
    @NotNull(message = "添加库存不能为空")
    @Min(value = 0, message = "添加库存不能小于0")
    private Integer addStock;
    private Integer version;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
