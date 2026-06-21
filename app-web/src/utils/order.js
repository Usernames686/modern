export function isUnpaid(order) {
  return Number(order?.paid || 0) === 0 && Number(order?.refundStatus || 0) === 0;
}

export function paymentMethodText(payType) {
  const map = {
    weixin: "微信支付",
    yue: "余额支付",
    offline: "线下支付",
    alipay: "支付宝支付"
  };
  return map[String(payType || "").toLowerCase()] || payType || "-";
}

export function canApplyRefund(order) {
  return Number(order?.paid || 0) === 1 && Number(order?.refundStatus || 0) === 0 && Number(order?.type || 0) !== 1;
}

export function orderGoodsList(order) {
  return order?.cartInfo || order?.orderInfoList || order?.info || [];
}

export function goodsName(goods) {
  return goods?.productName || goods?.storeName || goods?.productInfo?.storeName || "";
}

export function goodsImage(goods) {
  return goods?.image || goods?.productImg || goods?.productInfo?.image || "";
}

export function goodsSku(goods) {
  return goods?.sku || goods?.suk || goods?.attrInfo?.suk || "";
}

export function goodsPrice(goods) {
  return goods?.price || goods?.truePrice || goods?.productInfo?.price || "0.00";
}

export function goodsNum(goods) {
  return goods?.payNum || goods?.cartNum || goods?.num || 1;
}

export function orderGoodsTotal(order) {
  return orderGoodsList(order).reduce((sum, goods) => sum + Number(goodsNum(goods) || 0), 0);
}

export function refundStatusText(order) {
  if (Number(order?.refundStatus || 0) === 0 && order?.refundReason) {
    return "已拒绝";
  }
  const map = {
    1: "申请中",
    2: "已退款",
    3: "退款中"
  };
  return map[Number(order?.refundStatus || 0)] || order?.refundStatusText || order?.orderStatusMsg || order?.statusText || "售后";
}

export function canTakeOrder(order) {
  return Number(order?.paid || 0) === 1 && Number(order?.status || 0) === 1 && Number(order?.refundStatus || 0) === 0;
}

export function canViewLogistics(order) {
  return String(order?.deliveryType || "") === "express" && Number(order?.status || 0) > 0 && Boolean(order?.deliveryId);
}

export function canCommentOrder(order) {
  return Number(order?.paid || 0) === 1 && Number(order?.status || 0) === 2 && Number(order?.refundStatus || 0) === 0;
}

export function canCommentGoods(order, goods) {
  return canCommentOrder(order) && Number(goods?.isReply || 0) === 0;
}

export function canDeleteOrder(order) {
  const paid = Number(order?.paid || 0) === 1;
  const status = Number(order?.status || 0);
  const refundStatus = Number(order?.refundStatus || 0);
  return paid && (refundStatus === 2 || (refundStatus === 0 && status === 3));
}

export function canOrderAgain(order) {
  return Number(order?.paid || 0) === 1
    && Number(order?.status || 0) === 3
    && Number(order?.refundStatus || 0) === 0
    && Number(order?.type || 0) !== 1
    && Number(order?.seckillId || 0) === 0
    && Number(order?.bargainId || 0) === 0
    && Number(order?.combinationId || 0) === 0;
}
