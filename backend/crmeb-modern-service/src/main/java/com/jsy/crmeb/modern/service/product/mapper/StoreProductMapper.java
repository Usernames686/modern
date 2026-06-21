package com.jsy.crmeb.modern.service.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreProductMapper extends BaseMapper<StoreProduct> {
    @Update("update eb_store_product set stock = stock + #{stock}, version = version + 1 where id = #{id}")
    int incrementStock(@Param("id") Integer id, @Param("stock") Integer stock);

    @Update("update eb_store_product set browse = coalesce(browse, 0) + 1 where id = #{id}")
    int incrementBrowse(@Param("id") Integer id);
}
