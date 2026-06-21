package com.zbkj.common.response;

import lombok.Data;

/**
 * 易联云 获取AccessToken response body 数据
 * @program: crmeb
 **/
@Data
public class YlyAccessTokenBodyResponse {
    private String access_token;
    private String refresh_token;
    private String machine_code;
    private Integer expires_in;
    private String scope;
}
