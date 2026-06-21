package com.zbkj.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *


 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WechatVideoUploadImageInfoVo extends BaseResultResponseVo {

    private String media_id;
    private String temp_img_url;
    private String pay_media_id;
}
