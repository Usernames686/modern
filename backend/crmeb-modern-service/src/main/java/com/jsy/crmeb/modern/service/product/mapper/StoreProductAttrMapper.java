package com.jsy.crmeb.modern.service.product.mapper;

import com.jsy.crmeb.modern.service.product.dto.ProductAttrResponse;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreProductAttrMapper {
    @Select("""
            select id,
                   product_id as productId,
                   attr_name as attrName,
                   attr_values as attrValues,
                   type,
                   is_del as isDel
            from eb_store_product_attr
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
            order by id asc
            """)
    List<ProductAttrResponse> selectByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);

    @Select("""
            select count(1)
            from eb_store_product_attr
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
            """)
    int countByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);

    @Insert("""
            insert into eb_store_product_attr(
                product_id, attr_name, attr_values, type, is_del
            )
            values(
                #{productId}, #{attrName}, #{attrValues}, #{type}, 0
            )
            """)
    int insertAttr(
            @Param("productId") Integer productId,
            @Param("attrName") String attrName,
            @Param("attrValues") String attrValues,
            @Param("type") Integer type);

    @Update("""
            update eb_store_product_attr
            set is_del = 1
            where product_id = #{productId}
              and type = #{type}
              and is_del = 0
            """)
    int softDeleteByProductIdAndType(@Param("productId") Integer productId, @Param("type") Integer type);
}
