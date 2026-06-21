package com.jsy.crmeb.modern.service.front.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FrontArticleMapper {
    @Select("""
            select id, name
            from eb_category
            where type = 3
              and status = 1
            order by sort desc, id asc
            """)
    List<Map<String, Object>> selectCategories();

    @Select("""
            select count(1)
            from eb_article
            where cid = #{cid}
              and hide = 0
              and status = 0
            """)
    long countByCategory(@Param("cid") String cid);

    @Select("""
            select id,
                   cid,
                   title,
                   author,
                   image_input as imageInput,
                   synopsis,
                   share_title as shareTitle,
                   share_synopsis as shareSynopsis,
                   visit,
                   product_id as productId,
                   is_hot as isHot,
                   is_banner as isBanner,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime
            from eb_article
            where cid = #{cid}
              and hide = 0
              and status = 0
            order by sort desc, cast(coalesce(nullif(visit, ''), '0') as unsigned) desc, create_time desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectByCategory(@Param("cid") String cid, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            select id,
                   cid,
                   title,
                   author,
                   image_input as imageInput,
                   synopsis,
                   visit,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime
            from eb_article
            where is_hot = 1
              and hide = 0
              and status = 0
            order by sort desc, id desc
            limit 20
            """)
    List<Map<String, Object>> selectHot();

    @Select("""
            select id,
                   image_input as imageInput
            from eb_article
            where is_banner = 1
              and hide = 0
              and status = 0
            order by sort desc, id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectBanner(@Param("limit") int limit);

    @Select("""
            select id,
                   cid,
                   title,
                   author,
                   image_input as imageInput,
                   synopsis,
                   share_title as shareTitle,
                   share_synopsis as shareSynopsis,
                   visit,
                   product_id as productId,
                   is_hot as isHot,
                   is_banner as isBanner,
                   content,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime
            from eb_article
            where id = #{id}
              and hide = 0
              and status = 0
            limit 1
            """)
    Map<String, Object> selectInfo(@Param("id") Integer id);

    @Update("""
            update eb_article
            set visit = cast(coalesce(nullif(visit, ''), '0') as unsigned) + 1,
                update_time = now()
            where id = #{id}
            """)
    int increaseVisit(@Param("id") Integer id);

    @Select("select value from eb_system_config where name = #{name} limit 1")
    String configValue(@Param("name") String name);
}
