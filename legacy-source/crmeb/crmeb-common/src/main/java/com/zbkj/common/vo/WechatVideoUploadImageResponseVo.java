package com.zbkj.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *


 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WechatVideoUploadImageResponseVo extends BaseResultResponseVo {

    private WechatVideoUploadImageInfoVo img_info;
}
