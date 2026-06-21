// 移动端商城 API
// H5 走当前域名下的 /api 代理，避免旧包继续请求历史内网 IP。
// APP/小程序本地调试时可通过运行时配置或部署环境替换为正式域名。
let domain = typeof window !== 'undefined' && window.location ? window.location.origin : ''

module.exports = {
	// 请求域名 格式： https://您的域名
	// #ifdef MP || APP-PLUS
	// HTTP_REQUEST_URL:'',
	HTTP_REQUEST_URL: domain,
	// H5商城地址
	HTTP_H5_URL: typeof window !== 'undefined' && window.location ? window.location.origin : '',
	// #endif
	// #ifdef H5
	HTTP_REQUEST_URL: domain,
	// #endif
	HEADER: {
		'content-type': 'application/json'
	},
	HEADERPARAMS: {
		'content-type': 'application/x-www-form-urlencoded'
	},
	// 回话密钥名称 请勿修改此配置
	TOKENNAME: 'Authori-zation',
	// 缓存时间 0 永久
	EXPIRE: 0,
	//分页最多显示条数
	LIMIT: 10
};
