package com.jsy.crmeb.modern.service.front.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FrontProductReplyMapper {
    @Select("""
            <script>
            select id,
                   uid,
                   oid,
                   `unique`,
                   product_id as productId,
                   reply_type as replyType,
                   product_score as productScore,
                   service_score as serviceScore,
                   comment,
                   pics,
                   nickname,
                   avatar,
                   sku,
                   create_time as createTime
            from eb_store_product_reply
            where product_id = #{productId}
              and is_del = 0
            <choose>
              <when test='type == 1'>and product_score &gt;= 4</when>
              <when test='type == 2'>and product_score = 3</when>
              <when test='type == 3'>and product_score &lt;= 2</when>
            </choose>
            order by id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectReplyList(
            @Param("productId") Integer productId,
            @Param("type") Integer type,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            <script>
            select count(1)
            from eb_store_product_reply
            where product_id = #{productId}
              and is_del = 0
            <choose>
              <when test='type == 1'>and product_score &gt;= 4</when>
              <when test='type == 2'>and product_score = 3</when>
              <when test='type == 3'>and product_score &lt;= 2</when>
            </choose>
            </script>
            """)
    long countReplyList(@Param("productId") Integer productId, @Param("type") Integer type);

    @Select("""
            select
              count(1) as sumCount,
              coalesce(sum(case when product_score >= 4 then 1 else 0 end), 0) as goodCount,
              coalesce(sum(case when product_score = 3 then 1 else 0 end), 0) as inCount,
              coalesce(sum(case when product_score <= 2 then 1 else 0 end), 0) as poorCount,
              coalesce(sum(product_score + service_score), 0) as scoreSum
            from eb_store_product_reply
            where product_id = #{productId}
              and is_del = 0
            """)
    Map<String, Object> countReplyConfig(@Param("productId") Integer productId);
}
