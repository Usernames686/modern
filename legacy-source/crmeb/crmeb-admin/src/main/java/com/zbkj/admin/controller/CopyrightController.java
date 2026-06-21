package com.zbkj.admin.controller;

import com.zbkj.admin.copyright.CopyrightInfoResponse;
import com.zbkj.admin.copyright.CopyrightUpdateInfoRequest;
import com.zbkj.admin.service.CopyrightService;
import com.zbkj.common.result.CommonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * 版权控制器
 */
@Slf4j
@RestController
@RequestMapping("api/admin/copyright")
@Api(tags = "版权控制器")
public class CopyrightController {

    @Autowired
    private CopyrightService copyrightService;

    @PreAuthorize("hasAuthority('admin:copyright:get:info')")
    @ApiOperation(value = "获取版权信息")
    @RequestMapping(value = "/get/info", method = RequestMethod.GET)
    public CommonResult<CopyrightInfoResponse> getInfo() {
        return CommonResult.success(copyrightService.getInfo());
    }

}
