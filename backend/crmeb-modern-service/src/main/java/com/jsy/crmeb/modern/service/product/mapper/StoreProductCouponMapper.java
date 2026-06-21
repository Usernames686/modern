package com.jsy.crmeb.modern.service.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StoreProductCouponMapper {
    @Select("select issue_coupon_id from eb_store_product_coupon where product_id = #{productId} order by id asc")
    List<Integer> selectCouponIds(@Param("productId") Integer productId);

    @Delete("delete from eb_store_product_coupon where product_id = #{productId}")
    int deleteByProductId(@Param("productId") Integer productId);

    @Insert("insert into eb_store_product_coupon(product_id, issue_coupon_id, add_time) values(#{productId}, #{couponId}, #{addTime})")
    int insertProductCoupon(
            @Param("productId") Integer productId,
            @Param("couponId") Integer couponId,
            @Param("addTime") Integer addTime);
}
