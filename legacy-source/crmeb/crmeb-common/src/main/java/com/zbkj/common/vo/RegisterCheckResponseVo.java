package com.zbkj.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 获取接入状态 response


 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RegisterCheckResponseVo  extends BaseResultResponseVo {

    private RegisterCheckDataItemnVo data;
}
