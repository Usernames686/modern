package com.zbkj.common.response;

import lombok.Data;

/** 易联云 获取AccessToken response
 * @program: crmeb
 **/
@Data
public class YlyAccessTokenResponse {
    private String error;
    private String error_description;
    private YlyAccessTokenBodyResponse body;
}
