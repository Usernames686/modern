package com.jsy.crmeb.modern.service.marketing.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VideoChannelAdminMapper {
    @Select("""
            <script>
            select count(1)
            from (
              select oi.product_id
              from eb_store_order_info oi
              left join eb_store_product p on p.id = oi.product_id
              where oi.product_type = 4
              <if test="keywords != null and keywords != ''">
                and (oi.product_id like concat('%', #{keywords}, '%')
                  or oi.product_name like concat('%', #{keywords}, '%')
                  or p.store_name like concat('%', #{keywords}, '%')
                  or p.keyword like concat('%', #{keywords}, '%'))
              </if>
              group by oi.product_id
            ) vc
            </script>
            """)
    long countVideoProducts(@Param("keywords") String keywords);

    @Select("""
            <script>
            select
              oi.product_id as id,
              max(coalesce(nullif(p.image, ''), oi.image)) as image,
              max(coalesce(nullif(p.store_name, ''), oi.product_name)) as storeName,
              max(p.keyword) as keyword,
              max(p.cate_id) as cateId,
              max(p.price) as price,
              max(p.vip_price) as vipPrice,
              max(p.ot_price) as otPrice,
              max(p.sales) as sales,
              max(p.stock) as stock,
              max(p.is_show) as isShow,
              max(p.is_del) as isDel,
              max(p.is_recycle) as isRecycle,
              max(p.video_link) as videoLink,
              max(p.add_time) as addTime,
              count(distinct oi.order_id) as orderCount,
              coalesce(sum(oi.pay_num), 0) as payNum,
              coalesce(sum(case when o.paid = 1 then oi.vip_price * oi.pay_num else 0 end), 0) as payAmount,
              max(oi.create_time) as lastOrderTime
            from eb_store_order_info oi
            left join eb_store_order o on o.id = oi.order_id
            left join eb_store_product p on p.id = oi.product_id
            where oi.product_type = 4
            <if test="keywords != null and keywords != ''">
              and (oi.product_id like concat('%', #{keywords}, '%')
                or oi.product_name like concat('%', #{keywords}, '%')
                or p.store_name like concat('%', #{keywords}, '%')
                or p.keyword like concat('%', #{keywords}, '%'))
            </if>
            group by oi.product_id
            order by lastOrderTime desc, oi.product_id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectVideoProducts(
            @Param("keywords") String keywords,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            <script>
            select count(1)
            from eb_store_product p
            where p.is_del = 0
              and p.is_recycle = 0
              <choose>
                <when test="status == 'unshelved'">
                  and p.is_show = 0
                </when>
                <when test="status == 'withVideo'">
                  and p.video_link is not null and p.video_link != ''
                </when>
                <otherwise>
                  and (p.is_show = 0 or (p.video_link is not null and p.video_link != ''))
                </otherwise>
              </choose>
              <if test="keywords != null and keywords != ''">
                and (p.id like concat('%', #{keywords}, '%')
                  or p.store_name like concat('%', #{keywords}, '%')
                  or p.keyword like concat('%', #{keywords}, '%'))
              </if>
            </script>
            """)
    long countDraftProducts(@Param("status") String status, @Param("keywords") String keywords);

    @Select("""
            <script>
            select
              p.id,
              p.image,
              p.store_name as storeName,
              p.keyword,
              p.cate_id as cateId,
              p.price,
              p.vip_price as vipPrice,
              p.ot_price as otPrice,
              p.sales,
              p.stock,
              p.is_show as isShow,
              p.is_del as isDel,
              p.is_recycle as isRecycle,
              p.video_link as videoLink,
              p.add_time as addTime,
              0 as orderCount,
              0 as payNum,
              0 as payAmount,
              null as lastOrderTime
            from eb_store_product p
            where p.is_del = 0
              and p.is_recycle = 0
              <choose>
                <when test="status == 'unshelved'">
                  and p.is_show = 0
                </when>
                <when test="status == 'withVideo'">
                  and p.video_link is not null and p.video_link != ''
                </when>
                <otherwise>
                  and (p.is_show = 0 or (p.video_link is not null and p.video_link != ''))
                </otherwise>
              </choose>
              <if test="keywords != null and keywords != ''">
                and (p.id like concat('%', #{keywords}, '%')
                  or p.store_name like concat('%', #{keywords}, '%')
                  or p.keyword like concat('%', #{keywords}, '%'))
              </if>
            order by p.add_time desc, p.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectDraftProducts(
            @Param("status") String status,
            @Param("keywords") String keywords,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
