package com.jsy.crmeb.modern.service.front.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FrontBargainMapper {
    @Select("""
            select count(1)
            from eb_store_bargain
            where status = 1
              and is_del = 0
              and stock > 0
              and start_time <= #{nowMillis}
              and stop_time >= #{nowMillis}
            """)
    long countVisible(@Param("nowMillis") long nowMillis);

    @Select("""
            select
              id,
              product_id as productId,
              title,
              image,
              unit_name as unitName,
              stock,
              sales,
              images,
              start_time as startTime,
              stop_time as stopTime,
              store_name as storeName,
              price,
              min_price as minPrice,
              num,
              bargain_max_price as bargainMaxPrice,
              bargain_min_price as bargainMinPrice,
              bargain_num as bargainNum,
              status,
              give_integral as giveIntegral,
              info,
              cost,
              sort,
              is_hot as isHot,
              is_del as isDel,
              add_time as addTime,
              is_postage as isPostage,
              postage,
              rule,
              look,
              share,
              temp_id as tempId,
              weight,
              volume,
              quota,
              quota_show as quotaShow,
              people_num as peopleNum
            from eb_store_bargain
            where status = 1
              and is_del = 0
              and stock > 0
              and start_time <= #{nowMillis}
              and stop_time >= #{nowMillis}
            order by sort desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectVisible(
            @Param("nowMillis") long nowMillis,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              id,
              product_id as productId,
              title,
              image,
              unit_name as unitName,
              stock,
              sales,
              images,
              start_time as startTime,
              stop_time as stopTime,
              store_name as storeName,
              price,
              min_price as minPrice,
              num,
              bargain_max_price as bargainMaxPrice,
              bargain_min_price as bargainMinPrice,
              bargain_num as bargainNum,
              status,
              give_integral as giveIntegral,
              info,
              cost,
              sort,
              is_hot as isHot,
              is_del as isDel,
              add_time as addTime,
              is_postage as isPostage,
              postage,
              rule,
              look,
              share,
              temp_id as tempId,
              weight,
              volume,
              quota,
              quota_show as quotaShow,
              people_num as peopleNum
            from eb_store_bargain
            where id = #{id}
            limit 1
            """)
    Map<String, Object> selectById(@Param("id") Integer id);

    @Select("""
            select count(1)
            from eb_store_bargain_user_help
            """)
    int countHelps();

    @Select("""
            select
              u.nickname as nickName,
              u.avatar,
              bu.bargain_price_min as price,
              b.title
            from eb_store_bargain_user bu
            left join eb_user u on u.uid = bu.uid
            left join eb_store_bargain b on b.id = bu.bargain_id
            where bu.is_del = 0
              and bu.status = 3
            order by bu.id desc
            limit 7
            """)
    List<Map<String, Object>> selectSuccessList();

    @Select("""
            select
              p.is_del as isDel,
              p.is_show as isShow,
              p.stock
            from eb_store_product p
            where p.id = #{productId}
            limit 1
            """)
    Map<String, Object> selectMasterProduct(@Param("productId") Integer productId);

    @Select("""
            select
              bu.id,
              bu.uid,
              bu.bargain_id as bargainId,
              bu.bargain_price_min as bargainPriceMin,
              bu.bargain_price as bargainPrice,
              bu.price,
              bu.status,
              bu.add_time as addTime,
              bu.is_del as isDel
            from eb_store_bargain_user bu
            where bu.bargain_id = #{bargainId}
              and bu.uid = #{uid}
            order by bu.id desc
            limit 1
            """)
    Map<String, Object> selectLastUserBargain(
            @Param("bargainId") Integer bargainId,
            @Param("uid") Integer uid);

    @Select("""
            select
              bu.id,
              bu.uid,
              bu.bargain_id as bargainId,
              bu.bargain_price_min as bargainPriceMin,
              bu.bargain_price as bargainPrice,
              bu.price,
              bu.status,
              bu.add_time as addTime,
              bu.is_del as isDel
            from eb_store_bargain_user bu
            where bu.id = #{bargainUserId}
            limit 1
            """)
    Map<String, Object> selectUserBargainById(@Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select count(1)
            from eb_store_bargain_user
            where bargain_id = #{bargainId}
              and uid = #{uid}
            """)
    int countBargainUserTimes(
            @Param("bargainId") Integer bargainId,
            @Param("uid") Integer uid);

    @Select("""
            select count(1)
            from eb_store_bargain_user
            where bargain_id = #{bargainId}
              and uid = #{uid}
              and is_del = 0
            """)
    int countActiveBargainUserTimes(
            @Param("bargainId") Integer bargainId,
            @Param("uid") Integer uid);

    @Insert("""
            insert into eb_store_bargain_user (
              uid, bargain_id, bargain_price_min, bargain_price, price, status, add_time, is_del
            ) values (
              #{row.uid}, #{row.bargainId}, #{row.bargainPriceMin}, #{row.bargainPrice}, #{row.price},
              #{row.status}, #{row.addTime}, #{row.isDel}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insertBargainUser(@Param("row") Map<String, Object> row);

    @Select("""
            select
              order_id as orderNo,
              paid,
              is_del as isDel,
              is_system_del as isSystemDel
            from eb_store_order
            where bargain_id = #{bargainId}
              and bargain_user_id = #{bargainUserId}
            order by id desc
            limit 1
            """)
    Map<String, Object> selectBargainOrder(
            @Param("bargainId") Integer bargainId,
            @Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select
              uid,
              nickname,
              avatar
            from eb_user
            where uid = #{uid}
            limit 1
            """)
    Map<String, Object> selectUserBrief(@Param("uid") Integer uid);

    @Select("""
            select
              h.id,
              h.uid,
              h.bargain_id as bargainId,
              h.bargain_user_id as bargainUserId,
              h.price,
              h.add_time as addTime,
              u.nickname,
              u.avatar
            from eb_store_bargain_user_help h
            left join eb_user u on u.uid = h.uid
            where h.bargain_user_id = #{bargainUserId}
            order by h.id desc
            """)
    List<Map<String, Object>> selectHelpList(@Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select count(1)
            from eb_store_bargain_user_help
            where bargain_user_id = #{bargainUserId}
              and uid = #{uid}
            """)
    int countUserHelp(
            @Param("bargainUserId") Integer bargainUserId,
            @Param("uid") Integer uid);

    @Select("""
            select count(1)
            from eb_store_bargain_user_help
            where bargain_id = #{bargainId}
              and bargain_user_id = #{bargainUserId}
            """)
    long countHelpByBargainUser(
            @Param("bargainId") Integer bargainId,
            @Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select count(1)
            from eb_store_bargain_user_help h
            where h.bargain_id = #{bargainId}
              and h.uid = #{uid}
              and not exists (
                select 1
                from eb_store_bargain_user bu
                where bu.id = h.bargain_user_id
                  and bu.uid = #{uid}
              )
            """)
    int countOtherHelpByUser(
            @Param("bargainId") Integer bargainId,
            @Param("uid") Integer uid);

    @Update("""
            update eb_store_bargain_user
            set price = #{newPrice},
                status = #{status}
            where id = #{bargainUserId}
              and price = #{oldPrice}
            """)
    int updateBargainUserPrice(
            @Param("bargainUserId") Integer bargainUserId,
            @Param("oldPrice") BigDecimal oldPrice,
            @Param("newPrice") BigDecimal newPrice,
            @Param("status") Integer status);

    @Insert("""
            insert into eb_store_bargain_user_help (
              uid, bargain_id, bargain_user_id, price, add_time
            ) values (
              #{uid}, #{bargainId}, #{bargainUserId}, #{price}, #{addTime}
            )
            """)
    int insertBargainUserHelp(
            @Param("uid") Integer uid,
            @Param("bargainId") Integer bargainId,
            @Param("bargainUserId") Integer bargainUserId,
            @Param("price") BigDecimal price,
            @Param("addTime") Long addTime);

    @Update("""
            update eb_store_bargain
            set share = coalesce(share, 0) + 1
            where id = #{bargainId}
            """)
    int increaseShare(@Param("bargainId") Integer bargainId);

    @Update("""
            update eb_store_bargain
            set look = coalesce(look, 0) + 1
            where id = #{bargainId}
            """)
    int increaseLook(@Param("bargainId") Integer bargainId);

    @Select("""
            select count(1)
            from eb_store_bargain_user
            where uid = #{uid}
            """)
    long countUserRecords(@Param("uid") Integer uid);

    @Select("""
            select
              bu.id as bargainUserId,
              bu.uid,
              bu.bargain_id as bargainId,
              bu.bargain_price_min as bargainPriceMin,
              bu.bargain_price as bargainPrice,
              bu.price as cutPrice,
              bu.status,
              bu.add_time as addTime,
              bu.is_del as isDel,
              b.id,
              b.product_id as productId,
              b.title,
              b.image,
              b.stop_time as stopTime,
              b.min_price as minPrice,
              b.price,
              b.quota,
              b.unit_name as unitName,
              o.order_id as orderNo,
              o.paid as paid,
              o.is_del as orderIsDel,
              o.is_system_del as orderIsSystemDel
            from eb_store_bargain_user bu
            left join eb_store_bargain b on b.id = bu.bargain_id
            left join eb_store_order o
              on o.bargain_id = bu.bargain_id
             and o.bargain_user_id = bu.id
             and o.uid = bu.uid
            where bu.uid = #{uid}
            order by bu.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectUserRecords(
            @Param("uid") Integer uid,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
