package com.jsy.crmeb.modern.service.order.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderCancelTaskMapper {
    @Select("""
            select id,
                   order_id as orderNo,
                   coupon_id as couponId,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId
            from eb_store_order
            where id = #{id}
            limit 1
            """)
    Map<String, Object> selectOrder(@Param("id") Integer id);

    @Select("""
            select product_id as productId,
                   attr_value_id as attrValueId,
                   sku,
                   pay_num as payNum
            from eb_store_order_info
            where order_id = #{orderId}
            order by id asc
            """)
    List<Map<String, Object>> selectOrderInfo(@Param("orderId") Integer orderId);

    @Update("""
            update eb_store_coupon_user
            set status = 0,
                update_time = current_timestamp
            where id = #{couponId}
            """)
    int restoreCoupon(@Param("couponId") Integer couponId);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderId}, 'cancel_order', '取消订单', now())
            """)
    int insertCancelLog(@Param("orderId") Integer orderId);

    @Update("""
            update eb_store_product
            set stock = stock + #{num},
                sales = sales - #{num},
                version = version + 1
            where id = #{productId}
            """)
    int restoreProductStock(@Param("productId") Integer productId, @Param("num") Integer num);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock + #{num},
                sales = sales - #{num},
                version = version + 1
            where id = #{attrId}
              and type = #{type}
              and is_del = 0
            """)
    int restoreAttrStock(
            @Param("attrId") Integer attrId,
            @Param("type") Integer type,
            @Param("num") Integer num);

    @Update("""
            update eb_store_seckill
            set stock = stock + #{num},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int restoreSeckillStock(@Param("id") Integer id, @Param("num") Integer num);

    @Select("select product_id from eb_store_seckill where id = #{id} limit 1")
    Integer selectSeckillProductId(@Param("id") Integer id);

    @Update("""
            update eb_store_bargain
            set stock = stock + #{num},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int restoreBargainStock(@Param("id") Integer id, @Param("num") Integer num);

    @Select("select product_id from eb_store_bargain where id = #{id} limit 1")
    Integer selectBargainProductId(@Param("id") Integer id);

    @Update("""
            update eb_store_combination
            set stock = stock + #{num},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int restoreCombinationStock(@Param("id") Integer id, @Param("num") Integer num);

    @Select("select product_id from eb_store_combination where id = #{id} limit 1")
    Integer selectCombinationProductId(@Param("id") Integer id);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock + #{num},
                sales = sales - #{num},
                quota = quota + #{num},
                version = version + 1
            where id = #{attrId}
              and product_id = #{activityId}
              and type = #{type}
              and is_del = 0
            """)
    int restoreActivityAttrStock(
            @Param("activityId") Integer activityId,
            @Param("attrId") Integer attrId,
            @Param("type") Integer type,
            @Param("num") Integer num);

    @Select("""
            select id
            from eb_store_product_attr_value
            where product_id = #{productId}
              and suk = #{sku}
              and type = 0
              and is_del = 0
            order by id asc
            """)
    List<Integer> selectNormalAttrIdsBySku(@Param("productId") Integer productId, @Param("sku") String sku);
}
