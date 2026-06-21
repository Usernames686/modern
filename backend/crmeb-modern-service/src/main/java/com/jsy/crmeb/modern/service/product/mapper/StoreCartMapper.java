package com.jsy.crmeb.modern.service.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreCartMapper {
    @Update("update eb_store_cart set status = 0, update_time = now() where product_id = #{productId}")
    int disableByProductId(@Param("productId") Integer productId);

    @Update({
            "<script>",
            "update eb_store_cart set status = 1, update_time = now()",
            "where is_new = 0 and product_attr_unique in",
            "<foreach collection='skuIds' item='skuId' open='(' separator=',' close=')'>#{skuId}</foreach>",
            "</script>"
    })
    int enableBySkuIds(@Param("skuIds") List<Integer> skuIds);

    @Delete("delete from eb_store_cart where product_id = #{productId}")
    int deleteByProductId(@Param("productId") Integer productId);
}
