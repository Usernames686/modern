package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 商品添加库存对象

 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="ProductAddStockRequest对象", description="商品添加库存对象")
public class ProductAddStockRequest implements Serializable {

    private static final long serialVersionUID = -259657957101524526L;

    @ApiModelProperty(value = "商品ID", required = true)
    @NotNull(message = "商品ID不能为空")
    private Integer id;

    @Valid
    @ApiModelProperty(value = "商品规格属性列表", required = true)
    @NotEmpty(message = "商品规格属性列表不能为空")
    private List<ProductAttrValueAddStockRequest> attrValueList;

}
