package com.jsy.crmeb.modern.service.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StoreProductRelationMapper {
    @Select("select count(*) from eb_store_product_relation where product_id = #{productId} and type = #{type}")
    Integer countByProductIdAndType(@Param("productId") Integer productId, @Param("type") String type);

    @Delete("delete from eb_store_product_relation where product_id = #{productId}")
    int deleteByProductId(@Param("productId") Integer productId);

    @Select("""
            select count(*)
            from eb_store_product_relation
            where uid = #{uid}
              and product_id = #{productId}
              and type = #{type}
              and category = #{category}
            """)
    Integer countByUidAndProductIdAndType(
            @Param("uid") Integer uid,
            @Param("productId") Integer productId,
            @Param("type") String type,
            @Param("category") String category);

    @Insert("""
            insert into eb_store_product_relation(uid, product_id, type, category, create_time, update_time)
            values(#{uid}, #{productId}, #{type}, #{category}, now(), now())
            """)
    int insertRelation(
            @Param("uid") Integer uid,
            @Param("productId") Integer productId,
            @Param("type") String type,
            @Param("category") String category);

    @Delete("""
            <script>
            delete from eb_store_product_relation
            where uid = #{uid}
              and product_id in
              <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>#{productId}</foreach>
              and type = #{type}
              and category = #{category}
            </script>
            """)
    int deleteByUidAndProductIdsAndType(
            @Param("uid") Integer uid,
            @Param("productIds") List<Integer> productIds,
            @Param("type") String type,
            @Param("category") String category);

    @Delete("""
            <script>
            delete from eb_store_product_relation
            where uid = #{uid}
              and id in
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    int deleteByUidAndIds(@Param("uid") Integer uid, @Param("ids") List<Integer> ids);

    @Delete("delete from eb_store_product_relation where uid = #{uid} and product_id = #{productId} and type = #{type}")
    int deleteByUidAndProductIdAndType(
            @Param("uid") Integer uid,
            @Param("productId") Integer productId,
            @Param("type") String type);

    @Select("""
            select count(*)
            from eb_store_product_relation r
            inner join eb_store_product p on p.id = r.product_id
            where r.uid = #{uid}
              and r.type = 'collect'
              and p.is_show = 1
              and p.is_del = 0
              and p.is_recycle = 0
            """)
    long countUserCollectList(@Param("uid") Integer uid);

    @Select("""
            select r.id,
                   r.uid,
                   r.product_id as productId,
                   r.category,
                   r.type,
                   r.create_time as createTime,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.sales,
                   p.ficti,
                   p.unit_name as unitName,
                   p.stock
            from eb_store_product_relation r
            inner join eb_store_product p on p.id = r.product_id
            where r.uid = #{uid}
              and r.type = 'collect'
              and p.is_show = 1
              and p.is_del = 0
              and p.is_recycle = 0
            order by r.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectUserCollectList(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
