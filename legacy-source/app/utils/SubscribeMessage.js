const arrTemp = ["beforePay", "afterPay", "createBargain", "pink"];

/**
 * 支付成功后订阅消息id
 * 订阅  确认收货通知 订单支付成功  新订单管理员提醒
 */
export function openPaySubscribe() {
	let tmplIds = uni.getStorageSync('tempID' + arrTemp[0]);
	return subscribe(tmplIds);
}

/**
 * 订单相关订阅消息
 * 送货 发货 取消订单
 */
export function openOrderSubscribe() {
	let tmplIds = uni.getStorageSync('tempID' + arrTemp[1]);
	return subscribe(tmplIds);
}

/**
 * 砍价成功
 */
export function openBargainSubscribe() {
	let tmplIds = uni.getStorageSync('tempID' + arrTemp[2]);
	return subscribe(tmplIds);
}


/**
 * 拼团成功
 */
export function openPinkSubscribe() {
	let tmplIds = uni.getStorageSync('tempID' + arrTemp[3]);
	return subscribe(tmplIds);
}


/**
 * 调起订阅界面
 * array tmplIds 模板id
 */
export function subscribe(tmplIds) {
	let wecaht = wx;
	return new Promise((reslove, reject) => {
		wecaht.requestSubscribeMessage({
			tmplIds: tmplIds,
			success(res) {
				return reslove(res);
			},
			fail(res) {
				return reslove(res);
			}
		})
	});
}
