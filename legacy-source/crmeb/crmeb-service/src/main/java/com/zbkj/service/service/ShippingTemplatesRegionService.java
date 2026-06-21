package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.express.ShippingTemplatesRegion;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.ShippingTemplatesRegionRequest;
import com.zbkj.common.response.ShippingTemplatesRegionResponse;

import java.util.List;

/**
* ShippingTemplatesRegionService 接口
*/
public interface ShippingTemplatesRegionService extends IService<ShippingTemplatesRegion> {

    Boolean saveAll(List<ShippingTemplatesRegionRequest> shippingTemplatesRegionRequestList, Integer type, Integer id);

    List<ShippingTemplatesRegionResponse> getListGroup(Integer tempId);

    Boolean deleteByTempId(Integer tempId);

    /**
     * 删除
     * @param tempId 运费模板id
     * @return Boolean
     */
    Boolean delete(Integer tempId);

    /**
     * 根据模板编号、城市ID查询
     * @param tempId 模板编号
     * @param cityId 城市ID
     * @return 运费模板
     */
    ShippingTemplatesRegion getByTempIdAndCityId(Integer tempId, Integer cityId);

    /**
     * 获取运费模板区域列表
     * @param tempId 模板标号
     * @return List
     */
    List<ShippingTemplatesRegion> findListById(Integer tempId);
}
