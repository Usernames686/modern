package com.zbkj.service.service;

import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderPayRequest;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.PayConfigResponse;

/**
 * 订单支付
 */
public interface OrderPayService {


    /**
     * 获取支付配置
     */
    PayConfigResponse getPayConfig();

    /**
     * 支付成功处理
     *
     * @param storeOrder 订单
     */
    Boolean paySuccess(StoreOrder storeOrder);

    /**
     * 订单支付
     *
     * @param orderPayRequest 支付参数
     * @param ip              ip
     * @return OrderPayResultResponse
     */
    OrderPayResultResponse payment(OrderPayRequest orderPayRequest, String ip);
}
