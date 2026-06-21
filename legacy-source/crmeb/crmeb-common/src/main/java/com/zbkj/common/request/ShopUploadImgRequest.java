package com.zbkj.common.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 自定义组件上传图片请求对象


 */
@Data
public class ShopUploadImgRequest {

    @ApiModelProperty(value = "0:media_id(目前只用于品牌申请品牌和类目)，1：返回链接, 2：返回微信支付media_id图片要求")
    private Integer respType;

    @ApiModelProperty(value = "0:图片流(resp_type=2需指定filename)，1:图片url")
    private Integer uploadType;

    @ApiModelProperty(value = "upload_type=1时必传")
    private String imgUrl;
}
