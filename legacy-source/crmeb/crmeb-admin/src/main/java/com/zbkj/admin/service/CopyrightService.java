package com.zbkj.admin.service;

import com.zbkj.admin.copyright.CopyrightInfoResponse;
import com.zbkj.admin.copyright.CopyrightUpdateInfoRequest;

/**
 * 版权服务
 */
public interface CopyrightService {

    /**
     * 获取版权信息
     */
    CopyrightInfoResponse getInfo();

}
