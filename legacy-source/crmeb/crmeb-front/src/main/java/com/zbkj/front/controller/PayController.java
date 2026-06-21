package com.zbkj.front.controller;

import com.zbkj.common.request.OrderPayRequest;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.PayConfigResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.service.service.OrderPayService;
import com.zbkj.service.service.WeChatPayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 微信缓存表 前端控制器


 */
@Slf4j
@RestController
@RequestMapping("api/front/pay")
@Api(tags = "支付管理")
public class PayController {

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private OrderPayService orderPayService;



    @ApiOperation(value = "获取支付配置")
    @RequestMapping(value = "/get/config", method = RequestMethod.GET)
    public CommonResult<PayConfigResponse> getPayConfig() {
        return CommonResult.success(orderPayService.getPayConfig());
    }

    /**
     * 订单支付
     */
    @ApiOperation(value = "订单支付")
    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public CommonResult<OrderPayResultResponse> payment(@RequestBody @Validated OrderPayRequest orderPayRequest, HttpServletRequest request) {
        String ip = CrmebUtil.getClientIp(request);
        return CommonResult.success(orderPayService.payment(orderPayRequest, ip));
    }

    /**
     * 查询支付结果
     *
     * @param orderNo |订单编号|String|必填
     */
    @ApiOperation(value = "查询支付结果")
    @RequestMapping(value = "/queryPayResult", method = RequestMethod.GET)
    public CommonResult<Boolean> queryPayResult(@RequestParam String orderNo) {
        return CommonResult.success(weChatPayService.queryPayResult(orderNo));
    }
}
