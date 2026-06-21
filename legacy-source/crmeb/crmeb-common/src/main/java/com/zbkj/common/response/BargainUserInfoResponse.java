package com.zbkj.common.response;

import com.zbkj.common.model.bargain.StoreBargainUserHelp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 砍价用户详情响应对象


 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="BargainUserInfoResponse对象", description="砍价用户详情响应对象")
public class BargainUserInfoResponse implements Serializable {

    private static final long serialVersionUID = 4177599369617161973L;

    @ApiModelProperty(value = "当前用户砍价状态：1-可以参与砍价,2-参与次数已满，3-砍价中,4-已完成，5-可以帮砍，6-已帮砍,7-帮砍次数已满，8-已生成订单未支付，9-已支付，10-未支付，已取消")
    private Integer bargainStatus;

    @ApiModelProperty(value = "已砍金额")
    private BigDecimal alreadyPrice;

    @ApiModelProperty(value = "剩余金额")
    private BigDecimal surplusPrice;

    @ApiModelProperty(value = "砍价百分比")
    private Integer bargainPercent;

    @ApiModelProperty(value = "用户帮砍列表")
    private List<StoreBargainUserHelp> userHelpList;

    @ApiModelProperty(value = "用户砍价活动id")
    private Integer storeBargainUserId;

    @ApiModelProperty(value = "用户砍价活动昵称")
    private String storeBargainUserName;

    @ApiModelProperty(value = "用户砍价活动头像")
    private String storeBargainUserAvatar;
}
