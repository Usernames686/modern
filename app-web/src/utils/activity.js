import { dateTimeText, timestampMs } from "./format";

export function activityOrderMatches(order, type) {
  const activityText = [
    order?.activityType,
    order?.type,
    order?.productType,
    order?.orderType,
    order?.cartInfo?.[0]?.productType,
    order?.cartInfo?.[0]?.activityType
  ].map((value) => String(value ?? "").toLowerCase());
  if (type === "seckill") {
    return Number(order?.seckillId || 0) > 0
      || activityText.some((value) => value.includes("seckill") || value === "1" || value.includes("秒杀"));
  }
  if (type === "bargain") {
    return Number(order?.bargainId || 0) > 0
      || activityText.some((value) => value.includes("bargain") || value === "2" || value.includes("砍价"));
  }
  return Number(order?.combinationId || 0) > 0
    || Number(order?.pinkId || 0) > 0
    || activityText.some((value) => value.includes("combination") || value.includes("pink") || value === "3" || value.includes("拼团"));
}

export function activityOrderTypeText(order) {
  if (activityOrderMatches(order, "seckill")) {
    return "秒杀";
  }
  if (activityOrderMatches(order, "bargain")) {
    return "砍价";
  }
  if (activityOrderMatches(order, "combination")) {
    return "拼团";
  }
  return "活动";
}

export function activityOrderHint(order) {
  if (activityOrderMatches(order, "seckill")) {
    return "秒杀商品请尽快完成支付，超时会释放库存";
  }
  if (activityOrderMatches(order, "bargain")) {
    return "砍价成功后生成的活动订单，支付后进入订单履约";
  }
  if (activityOrderMatches(order, "combination")) {
    return "拼团订单支付后可邀请好友参团，未成团按售后规则处理";
  }
  return "";
}

export function activityOrderActionText(order) {
  if (activityOrderMatches(order, "seckill")) {
    return "查看秒杀";
  }
  if (activityOrderMatches(order, "bargain")) {
    return "查看砍价";
  }
  if (activityOrderMatches(order, "combination")) {
    return "查看拼团";
  }
  return "查看活动";
}

export function bargainRecordStatusText(item) {
  const status = Number(item?.status || 0);
  if (item?.isDel && status === 3 && !item?.isPay) return "砍价失败";
  if (item?.isDel) return "已取消";
  if (status === 1) return "砍价中";
  if (status === 2) return "砍价失败";
  if (status === 3 && item?.isPay) return "已支付";
  if (status === 3 && item?.isOrder) return "待支付";
  if (status === 3) return "砍价成功";
  return "未知";
}

export function bargainRecordMainStatusText(item) {
  const status = Number(item?.status || 0);
  if (status === 3 && !item?.isDel) {
    return "砍价成功";
  }
  if (status === 3 && item?.isDel && !item?.isPay) {
    return "砍价失败";
  }
  return "活动已结束";
}

export function bargainRecordMainStatusClass(item) {
  const status = Number(item?.status || 0);
  return {
    success: status === 3 && !item?.isDel,
    fail: status === 2 || (status === 3 && item?.isDel && !item?.isPay) || Boolean(item?.isDel),
    active: status === 1
  };
}

export function canPayBargainRecord(item) {
  return Number(item?.status) === 3 && Boolean(item?.isOrder) && !item?.isPay && Boolean(item?.orderNo);
}

export function canCreateBargainOrder(item) {
  return Number(item?.status) === 3 && !item?.isDel && !item?.isOrder;
}

export function firstActivityAttrValueId(activity) {
  const values = Object.values(activity?.productValue || activity?.attrValue || {});
  return values[0]?.id || values[0]?.unique || "";
}

export function combinationItemEnded(item) {
  if (!item) {
    return true;
  }
  const stop = timestampMs(item.stopTime);
  return Boolean(item.isDel) || Boolean(item.isSoldOut) || (stop > 0 && stop < Date.now());
}

export function combinationPinkText(pink) {
  const count = Number(pink?.count ?? pink?.countPeople ?? pink?.surplusPeople ?? 0);
  const stop = timestampMs(pink?.stopTime);
  const timeText = stop ? `${dateTimeText(stop)} 截止` : "";
  const needText = count > 0 ? `还差 ${count} 人成团` : "等待成团";
  return timeText ? `${needText} · ${timeText}` : needText;
}
