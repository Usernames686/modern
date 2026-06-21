package com.jsy.crmeb.modern.service.front.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FrontCombinationMapper {
    @Select("""
            select count(1)
            from eb_store_combination
            where is_del = 0
              and is_show = 1
              and stock >= 0
              and start_time <= #{nowMillis}
              and stop_time >= #{nowMillis}
            """)
    long countVisible(@Param("nowMillis") long nowMillis);

    @Select("""
            <script>
            select
              c.id,
              c.product_id as productId,
              c.image,
              c.images,
              c.title,
              c.people,
              c.info,
              c.price,
              c.sort,
              c.sales,
              c.stock,
              c.add_time as addTime,
              c.is_host as isHost,
              c.is_show as isShow,
              c.is_del as isDel,
              c.is_postage as isPostage,
              c.postage,
              c.start_time as startTime,
              c.stop_time as stopTime,
              c.effective_time as effectiveTime,
              c.cost,
              c.browse,
              c.unit_name as unitName,
              c.temp_id as tempId,
              c.weight,
              c.volume,
              c.num,
              c.quota,
              c.quota_show as quotaShow,
              c.ot_price as otPrice,
              c.once_num as onceNum,
              c.virtual_ration as virtualRation,
              coalesce(pink_count.countPeople, 0) as countPeople,
              coalesce(pink_count.countPeopleAll, 0) as countPeopleAll,
              coalesce(pink_count.countPeoplePink, 0) as countPeoplePink
            from eb_store_combination c
            left join (
              select cid,
                     sum(case when k_id = 0 then 1 else 0 end) as countPeople,
                     count(1) as countPeopleAll,
                     sum(case when k_id = 0 and status = 2 then 1 else 0 end) as countPeoplePink
              from eb_store_pink
              group by cid
            ) pink_count on pink_count.cid = c.id
            where c.is_del = 0
              and c.is_show = 1
              and c.stock >= 0
              and c.start_time &lt;= #{nowMillis}
              and c.stop_time >= #{nowMillis}
            <if test="excludeId != null and excludeId &gt; 0">
              and c.id != #{excludeId}
            </if>
            order by c.sort desc, c.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectVisible(
            @Param("nowMillis") long nowMillis,
            @Param("excludeId") Integer excludeId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              c.id,
              c.product_id as productId,
              c.image,
              c.images,
              c.title,
              c.people,
              c.info,
              c.price,
              c.sort,
              c.sales,
              c.stock,
              c.add_time as addTime,
              c.is_host as isHost,
              c.is_show as isShow,
              c.is_del as isDel,
              c.is_postage as isPostage,
              c.postage,
              c.start_time as startTime,
              c.stop_time as stopTime,
              c.effective_time as effectiveTime,
              c.cost,
              c.browse,
              c.unit_name as unitName,
              c.temp_id as tempId,
              c.weight,
              c.volume,
              c.num,
              c.quota,
              c.quota_show as quotaShow,
              c.ot_price as otPrice,
              c.once_num as onceNum,
              c.virtual_ration as virtualRation,
              coalesce(pink_count.countPeople, 0) as countPeople,
              coalesce(pink_count.countPeopleAll, 0) as countPeopleAll,
              coalesce(pink_count.countPeoplePink, 0) as countPeoplePink
            from eb_store_combination c
            left join (
              select cid,
                     sum(case when k_id = 0 then 1 else 0 end) as countPeople,
                     count(1) as countPeopleAll,
                     sum(case when k_id = 0 and status = 2 then 1 else 0 end) as countPeoplePink
              from eb_store_pink
              group by cid
            ) pink_count on pink_count.cid = c.id
            where c.id = #{id}
              and c.is_del = 0
              and c.is_show = 1
            limit 1
            """)
    Map<String, Object> selectVisibleById(@Param("id") Integer id);

    @Select("""
            select attr_name as attrName,
                   attr_values as attrValues,
                   product_id as productId,
                   type
            from eb_store_product_attr
            where product_id = #{combinationId}
              and type = 3
              and is_del = 0
            order by id asc
            """)
    List<Map<String, Object>> selectAttrs(@Param("combinationId") Integer combinationId);

    @Select("""
            select p.id,
                   p.uid,
                   p.cid,
                   p.k_id as kId,
                   p.people,
                   p.price,
                   p.total_num as totalNum,
                   p.stop_time as stopTime,
                   p.add_time as addTime,
                   p.status,
                   p.is_refund as isRefund,
                   p.order_id as orderId,
                   u.nickname,
                   u.avatar,
                   coalesce(member_count.countPeople, 1) as countPeople
            from eb_store_pink p
            left join eb_user u on u.uid = p.uid
            left join (
              select k_id, count(1) + 1 as countPeople
              from eb_store_pink
              where k_id > 0
              group by k_id
            ) member_count on member_count.k_id = p.id
            where p.cid = #{combinationId}
              and p.k_id = 0
              and p.is_refund = 0
            order by p.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectPinkHeads(@Param("combinationId") Integer combinationId, @Param("limit") int limit);

    @Select("""
            select p.id,
                   p.uid,
                   p.cid,
                   p.k_id as kId,
                   p.people,
                   p.price,
                   p.total_num as totalNum,
                   p.stop_time as stopTime,
                   p.add_time as addTime,
                   p.status,
                   p.is_refund as isRefund,
                   p.order_id as orderId,
                   u.nickname,
                   u.avatar
            from eb_store_pink p
            left join eb_user u on u.uid = p.uid
            where p.id = #{pinkId}
               or p.k_id = #{pinkId}
            order by p.k_id asc, p.id asc
            """)
    List<Map<String, Object>> selectPinkMembers(@Param("pinkId") Integer pinkId);

    @Select("""
            select p.id,
                   p.uid,
                   p.cid,
                   p.k_id as kId,
                   p.people,
                   p.price,
                   p.total_num as totalNum,
                   p.stop_time as stopTime,
                   p.add_time as addTime,
                   p.status,
                   p.is_refund as isRefund,
                   p.order_id as orderId,
                   u.nickname,
                   u.avatar
            from eb_store_pink p
            left join eb_user u on u.uid = p.uid
            where p.id = #{pinkId}
            limit 1
            """)
    Map<String, Object> selectPinkById(@Param("pinkId") Integer pinkId);

    @Select("select coalesce(sum(total_num), 0) from eb_store_pink where is_refund = 0")
    Integer countTotalPeople();

    @Update("""
            update eb_store_combination
            set browse = coalesce(browse, 0) + 1
            where id = #{id}
            """)
    int incrementBrowse(@Param("id") Integer id);

    @Select("""
            select avatar
            from eb_store_pink
            where is_refund = 0
              and avatar is not null
              and avatar != ''
            order by id desc
            limit #{limit}
            """)
    List<String> selectRecentPinkAvatars(@Param("limit") int limit);
}
