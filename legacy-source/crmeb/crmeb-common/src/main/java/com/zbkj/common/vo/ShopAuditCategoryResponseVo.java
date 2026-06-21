package com.zbkj.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上传类目资质 response


 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ShopAuditCategoryResponseVo extends BaseResultResponseVo {
    private String audit_id;
}
