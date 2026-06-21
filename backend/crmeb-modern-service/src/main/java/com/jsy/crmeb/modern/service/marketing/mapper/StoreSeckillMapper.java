package com.jsy.crmeb.modern.service.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.marketing.entity.StoreSeckill;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreSeckillMapper extends BaseMapper<StoreSeckill> {
    @Update("""
            update eb_store_product_attr_value
            set is_del = 1
            where product_id = #{productId}
              and type = 1
            """)
    int softDeleteAttrValues(@Param("productId") Integer productId);

    @Update("""
            update eb_store_product_attr
            set is_del = 1
            where product_id = #{productId}
              and type = 1
            """)
    int softDeleteAttrs(@Param("productId") Integer productId);

    @Insert("""
            insert into eb_store_product_attr(
                product_id, attr_name, attr_values, type, is_del
            )
            values(
                #{productId}, #{attrName}, #{attrValues}, 1, 0
            )
            """)
    int insertAttr(
            @Param("productId") Integer productId,
            @Param("attrName") String attrName,
            @Param("attrValues") String attrValues);

    @Insert("""
            insert into eb_store_product_attr_value(
                product_id, suk, stock, sales, price, image, `unique`, cost, bar_code,
                ot_price, weight, volume, brokerage, brokerage_two, type, quota,
                quota_show, attr_value, is_del, version
            )
            values(
                #{productId}, #{suk}, #{stock}, 0, #{price}, #{image}, #{uniqueValue}, #{cost}, #{barCode},
                #{otPrice}, #{weight}, #{volume}, 0, 0, 1, #{quota},
                #{quotaShow}, #{attrValue}, 0, 0
            )
            """)
    int insertAttrValue(
            @Param("productId") Integer productId,
            @Param("suk") String suk,
            @Param("stock") Integer stock,
            @Param("price") BigDecimal price,
            @Param("image") String image,
            @Param("uniqueValue") String uniqueValue,
            @Param("cost") BigDecimal cost,
            @Param("barCode") String barCode,
            @Param("otPrice") BigDecimal otPrice,
            @Param("weight") BigDecimal weight,
            @Param("volume") BigDecimal volume,
            @Param("quota") Integer quota,
            @Param("quotaShow") Integer quotaShow,
            @Param("attrValue") String attrValue);
}
