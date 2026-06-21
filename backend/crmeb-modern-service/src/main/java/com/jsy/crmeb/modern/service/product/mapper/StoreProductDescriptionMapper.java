package com.jsy.crmeb.modern.service.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreProductDescriptionMapper {
    @Select("select description from eb_store_product_description where product_id = #{productId} and type = #{type} limit 1")
    String selectDescription(@Param("productId") Integer productId, @Param("type") Integer type);

    @Select("select count(1) from eb_store_product_description where product_id = #{productId} and type = #{type}")
    int countDescription(@Param("productId") Integer productId, @Param("type") Integer type);

    @Update("update eb_store_product_description set description = #{description} where product_id = #{productId} and type = #{type}")
    int updateDescription(@Param("productId") Integer productId, @Param("type") Integer type, @Param("description") String description);

    @Insert("insert into eb_store_product_description(product_id, description, type) values(#{productId}, #{description}, #{type})")
    int insertDescription(@Param("productId") Integer productId, @Param("type") Integer type, @Param("description") String description);
}
