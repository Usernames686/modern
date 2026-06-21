package com.zbkj.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询审核结果 Response


 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ShopAuditResultResponseVo extends BaseResultResponseVo {

    private ItemData data;

    @Data
    class ItemData{
        private Integer status;
        private Integer brand_id;
        private String reject_reason;
    }
}
