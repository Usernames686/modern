package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 二维码Vo对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "QrCodeVo对象", description = "二维码Vo对象")
public class QrCodeVo {

    @ApiModelProperty(value = "二维码（base64已处理）")
    private String code;
}
