package com.zbkj.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zbkj.admin.copyright.CopyrightInfoResponse;
import com.zbkj.admin.service.CopyrightService;
import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 版权服务实现类
 */
@Service
public class CopyrightServiceImpl implements CopyrightService {

    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private CrmebConfig crmebConfig;

    /**
     * 获取版权信息
     */
    @Override
    public CopyrightInfoResponse getInfo() {
        CopyrightInfoResponse response = new CopyrightInfoResponse();
        String domainName = systemConfigService.getValueByKey(Constants.CONFIG_KEY_API_URL);
        if (StrUtil.isBlank(domainName)) {
            response.setStatus(-2);
            return response;
        }
        String label = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_COPYRIGHT_LABEL);
        String version = crmebConfig.getVersion();
        response.setDomainUrl(domainName);
        response.setLabel(Integer.parseInt(label));
        response.setVersion(version);

        response.setStatus(-1);
        return response;
    }

}
