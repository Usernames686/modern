<template>
	<!-- 辅助空白-->
	<view>
		<view :style="[boxStyle]"></view>
	</view>
</template>
<script>
	export default {
		name: 'blankPage',
		props: {
			dataConfig: {
				type: Object,
				default: () => {}
			}
		},
		computed: {
			//最外层盒子的样式
			boxStyle() {
				return {
					height: this.dataConfig.heightConfig.val + 'px',
					background: `linear-gradient(${this.dataConfig.bgColor.color[0].item}, ${this.dataConfig.bgColor.color[1].item})`,
					margin: this.dataConfig.mbConfig.val * 2 + 'rpx' + ' ' + this.dataConfig.lrConfig.val * 2 + 'rpx' +
						' ' + 0,
				}
			},
		},
	}
</script>

<style lang="scss" scoped>
	.blankPage {
		.bankCon {
			width: 100%;
		}
	}
</style>