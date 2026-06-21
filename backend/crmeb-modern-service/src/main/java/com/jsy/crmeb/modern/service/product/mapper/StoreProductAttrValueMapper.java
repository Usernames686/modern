package com.jsy.crmeb.modern.service.product.mapper;

import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreProductAttrValueMapper {
    @Select("select id from eb_store_product_attr_value where product_id = #{productId} and type = #{type} and is_del = 0")
    List<Integer> selectIdsByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);

    @Select("""
            select id,
                   product_id as productId,
                   suk,
                   stock,
                   sales,
                   price,
                   image,
                   cost,
                   ot_price as otPrice,
                   weight,
                   volume,
                   brokerage,
                   brokerage_two as brokerageTwo,
                   type,
                   quota,
                   quota_show as quotaShow,
                   attr_value as attrValue,
                   bar_code as barCode,
                   version
            from eb_store_product_attr_value
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
            order by id asc
            """)
    List<ProductAttrValueResponse> selectByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);

    @Select("""
            <script>
            select id,
                   product_id as productId,
                   suk,
                   stock,
                   sales,
                   price,
                   image,
                   cost,
                   ot_price as otPrice,
                   weight,
                   volume,
                   brokerage,
                   brokerage_two as brokerageTwo,
                   type,
                   quota,
                   quota_show as quotaShow,
                   attr_value as attrValue,
                   bar_code as barCode,
                   version
            from eb_store_product_attr_value
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
              and id in
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<ProductAttrValueResponse> selectByProductIdAndTypeAndIds(
            @Param("productId") Integer productId,
            @Param("type") Integer type,
            @Param("ids") List<Integer> ids);

    @Insert("""
            insert into eb_store_product_attr_value(
                product_id, suk, stock, sales, price, image, `unique`, cost, bar_code,
                ot_price, weight, volume, brokerage, brokerage_two, type, quota,
                quota_show, attr_value, is_del, version
            )
            values(
                #{productId}, #{suk}, #{stock}, 0, #{price}, #{image}, #{uniqueValue}, #{cost}, #{barCode},
                #{otPrice}, 0, 0, 0, 0, #{type}, 0,
                0, #{attrValue}, 0, 0
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
            @Param("type") Integer type,
            @Param("attrValue") String attrValue);

    @Update("""
            update eb_store_product_attr_value
            set price = #{price},
                image = #{image},
                cost = #{cost},
                ot_price = #{otPrice},
                bar_code = #{barCode}
            where id = #{id}
              and product_id = #{productId}
              and type = #{type}
              and is_del = 0
            """)
    int updateBasicFields(
            @Param("id") Integer id,
            @Param("productId") Integer productId,
            @Param("type") Integer type,
            @Param("price") BigDecimal price,
            @Param("image") String image,
            @Param("cost") BigDecimal cost,
            @Param("otPrice") BigDecimal otPrice,
            @Param("barCode") String barCode);

    @Update("""
            update eb_store_product_attr_value
            set is_del = 1
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
            """)
    int softDeleteByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock + #{stock},
                version = version + 1
            where id = #{id}
              and type = #{type}
              and version = #{version}
            """)
    int incrementStock(
            @Param("id") Integer id,
            @Param("type") Integer type,
            @Param("stock") Integer stock,
            @Param("version") Integer version);
}
