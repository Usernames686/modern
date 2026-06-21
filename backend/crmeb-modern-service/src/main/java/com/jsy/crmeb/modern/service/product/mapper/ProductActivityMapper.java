package com.jsy.crmeb.modern.service.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductActivityMapper {
    @Select("select count(*) from eb_store_seckill where product_id = #{productId} and is_del = 0 and status = 1")
    int countActiveSeckill(@Param("productId") Integer productId);

    @Select("select count(*) from eb_store_bargain where product_id = #{productId} and status = 1")
    int countActiveBargain(@Param("productId") Integer productId);

    @Select("select count(*) from eb_store_combination where product_id = #{productId} and is_show = 1")
    int countActiveCombination(@Param("productId") Integer productId);
}
