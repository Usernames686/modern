export function maskPhone(phone) {
  const value = String(phone || "");
  if (value.length !== 11) {
    return value || "未绑定手机号";
  }
  return `${value.slice(0, 3)}****${value.slice(7)}`;
}

export function couponTypeText(coupon) {
  const useType = Number(coupon?.useType || 1);
  if (useType === 2) return "商品";
  if (useType === 3) return "品类";
  return "通用";
}

export function couponStatusText(status) {
  const map = {
    usable: "可用",
    unusable: "已用",
    overdue: "过期",
    notStart: "未开始"
  };
  return map[status] || "可用";
}

export function extractStatusText(status) {
  const map = {
    "-1": "未通过",
    0: "审核中",
    1: "已提现"
  };
  return map[status] || "审核中";
}

export function paymentIconClass(value) {
  if (value === "weixin") return "wechat";
  if (value === "alipay") return "alipay";
  return "balance";
}

export function paymentIconText(value) {
  if (value === "weixin") return "微";
  if (value === "alipay") return "支";
  return "余";
}

export function menuIconClass(item) {
  if (item.name.includes("分类")) return "menu-icon cate";
  if (item.name.includes("优惠")) return "menu-icon coupon";
  if (item.name.includes("订单")) return "menu-icon order";
  if (item.name.includes("会员")) return "menu-icon vip";
  if (item.name.includes("积分")) return "menu-icon point";
  if (item.name.includes("客服")) return "menu-icon service";
  if (item.name.includes("收藏")) return "menu-icon collect";
  return "menu-icon spread";
}
