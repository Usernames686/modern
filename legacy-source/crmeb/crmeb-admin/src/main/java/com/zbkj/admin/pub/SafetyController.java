package com.zbkj.admin.pub;

import com.anji.captcha.model.common.ResponseModel;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SafetyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 安全验证控制器
 */
@Slf4j
@RestController
@RequestMapping("api/public/safety")
@Api(tags = "安全验证控制器")
public class SafetyController {

    @Autowired
    private SafetyService safetyService;

    @ApiOperation(value = "获取行为验证码")
    @RequestMapping(value = "/get", method = RequestMethod.POST)
    public CommonResult<ResponseModel> getSafetyCode(@RequestBody com.anji.captcha.model.vo.CaptchaVO data, HttpServletRequest request) {
        return CommonResult.success(safetyService.getSafetyCode(data, request));
    }

    @ApiOperation(value = "验证行为验证码")
    @RequestMapping(value = "/check", method = RequestMethod.POST)
    public CommonResult<ResponseModel> checkSafetyCode(@RequestBody com.anji.captcha.model.vo.CaptchaVO data, HttpServletRequest request) {
        return CommonResult.success(safetyService.checkSafetyCode(data, request));
    }

    @ApiOperation(value = "行为验证码二次校验")
    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public CommonResult<ResponseModel> verifySafetyCode(@RequestBody com.anji.captcha.model.vo.CaptchaVO data, HttpServletRequest request) {
        return CommonResult.success(safetyService.verifySafetyCode(data));
    }
}
