package com.jsy.crmeb.modern.service.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductStockTaskMapper {
    @Update("""
            update eb_store_product
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                version = version + 1
            where id = #{productId}
            """)
    int updateProductStock(
            @Param("productId") Integer productId,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                version = version + 1
            where product_id = #{productId}
              and id = #{attrId}
              and type = #{type}
              and is_del = 0
            """)
    int updateProductAttrStock(
            @Param("productId") Integer productId,
            @Param("attrId") Integer attrId,
            @Param("type") Integer type,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Update("""
            update eb_store_seckill
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int updateSeckillStock(
            @Param("id") Integer id,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Update("""
            update eb_store_bargain
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int updateBargainStock(
            @Param("id") Integer id,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Update("""
            update eb_store_combination
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                quota = quota + #{num}
            where id = #{id}
            """)
    int updateCombinationStock(
            @Param("id") Integer id,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock + #{stockDelta},
                sales = sales - #{num},
                quota = quota + #{num},
                version = version + 1
            where product_id = #{activityId}
              and id = #{attrId}
              and type = #{type}
              and is_del = 0
            """)
    int updateActivityAttrStock(
            @Param("activityId") Integer activityId,
            @Param("attrId") Integer attrId,
            @Param("type") Integer type,
            @Param("stockDelta") Integer stockDelta,
            @Param("num") Integer num);

    @Select("""
            select id
            from eb_store_product_attr_value
            where product_id = #{productId}
              and suk = #{suk}
              and type = 0
              and is_del = 0
            order by id asc
            """)
    List<Integer> selectNormalAttrIdsBySuk(@Param("productId") Integer productId, @Param("suk") String suk);
}
