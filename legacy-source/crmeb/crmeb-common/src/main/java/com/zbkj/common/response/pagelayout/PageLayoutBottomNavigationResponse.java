package com.zbkj.common.response.pagelayout;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * 页面设计底部导航响应对象


 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "PageLayoutBottomNavigationResponse对象", description = "页面设计底部导航响应对象")
public class PageLayoutBottomNavigationResponse implements Serializable {

    private static final long serialVersionUID = 8350218800271787826L;

    @ApiModelProperty(value = "底部导航")
    private List<HashMap<String, Object>> bottomNavigationList;

    @ApiModelProperty(value = "是否自定义")
    private String isCustom;

}
