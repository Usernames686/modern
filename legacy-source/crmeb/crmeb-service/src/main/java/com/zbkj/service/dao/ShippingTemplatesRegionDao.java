package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.express.ShippingTemplatesRegion;
import com.zbkj.common.response.ShippingTemplatesRegionResponse;

import java.util.List;

/**
 *  运费模板区域 Mapper 接口
 */
public interface ShippingTemplatesRegionDao extends BaseMapper<ShippingTemplatesRegion> {

    List<ShippingTemplatesRegionResponse> getListGroup(Integer tempId);
}
