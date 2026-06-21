package com.jsy.crmeb.modern.service.distribution.mapper;

import com.jsy.crmeb.modern.service.distribution.dto.SpreadChildUserResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadOrderResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadUserResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DistributionAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_user u
            where u.is_promoter = 1
            <if test="keywords != null and keywords != ''">
              and (u.uid = #{keywords} or u.nickname like concat('%', #{keywords}, '%') or u.phone like concat('%', #{keywords}, '%'))
            </if>
            <if test="startTime != null and endTime != null">
              and u.promoter_time between #{startTime} and #{endTime}
            </if>
            </script>
            """)
    long countPromoters(@Param("keywords") String keywords, @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("""
            <script>
            select u.uid,
                   u.real_name as realName,
                   u.nickname,
                   u.avatar,
                   u.phone,
                   coalesce(u.brokerage_price, 0) as brokeragePrice,
                   u.spread_uid as spreadUid,
                   coalesce(nullif(parent.nickname, ''), '无') as spreadNickname,
                   coalesce(u.pay_count, 0) as payCount,
                   coalesce(u.spread_count, 0) as spreadCount,
                   coalesce(br.spreadOrderNum, 0) as spreadOrderNum,
                   coalesce(br.totalBrokeragePrice, 0) as totalBrokeragePrice,
                   coalesce(orders.spreadOrderTotalPrice, 0) as spreadOrderTotalPrice,
                   coalesce(ex.extractCountPrice, 0) as extractCountPrice,
                   coalesce(ex.extractCountNum, 0) as extractCountNum,
                   coalesce(frozen.freezeBrokeragePrice, 0) as freezeBrokeragePrice,
                   u.promoter_time as promoterTime
            from eb_user u
            left join eb_user parent on parent.uid = u.spread_uid
            left join (
              select uid, count(*) as spreadOrderNum, sum(price) as totalBrokeragePrice
              from eb_user_brokerage_record
              where link_type = 'order' and type = 1 and status = 3
              group by uid
            ) br on br.uid = u.uid
            left join (
              select br.uid, sum(o.pay_price) as spreadOrderTotalPrice
              from eb_user_brokerage_record br
              join eb_store_order o on o.order_id = br.link_id
              where br.link_type = 'order' and br.type = 1 and br.status = 3
              group by br.uid
            ) orders on orders.uid = u.uid
            left join (
              select uid, sum(extract_price) as extractCountPrice, count(*) as extractCountNum
              from eb_user_extract
              where status >= 1
              group by uid
            ) ex on ex.uid = u.uid
            left join (
              select uid, sum(price) as freezeBrokeragePrice
              from eb_user_brokerage_record
              where link_type = 'order' and status = 2
              group by uid
            ) frozen on frozen.uid = u.uid
            where u.is_promoter = 1
            <if test="keywords != null and keywords != ''">
              and (u.uid = #{keywords} or u.nickname like concat('%', #{keywords}, '%') or u.phone like concat('%', #{keywords}, '%'))
            </if>
            <if test="startTime != null and endTime != null">
              and u.promoter_time between #{startTime} and #{endTime}
            </if>
            order by u.uid desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SpreadUserResponse> selectPromoters(
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(*)
            from eb_user u
            where
            <choose>
              <when test="type == 1">u.spread_uid = #{uid}</when>
              <when test="type == 2">u.spread_uid in (select first.uid from eb_user first where first.spread_uid = #{uid})</when>
              <otherwise>(u.spread_uid = #{uid} or u.spread_uid in (select first.uid from eb_user first where first.spread_uid = #{uid}))</otherwise>
            </choose>
            <if test="keywords != null and keywords != ''">
              and (u.uid = #{keywords} or u.nickname like concat('%', #{keywords}, '%') or u.phone = #{keywords})
            </if>
            </script>
            """)
    long countSpreadUsers(@Param("uid") Integer uid, @Param("type") Integer type, @Param("keywords") String keywords);

    @Select("""
            <script>
            select u.uid,
                   u.avatar,
                   u.nickname,
                   u.is_promoter as isPromoter,
                   coalesce(u.spread_count, 0) as spreadCount,
                   coalesce(u.pay_count, 0) as payCount
            from eb_user u
            where
            <choose>
              <when test="type == 1">u.spread_uid = #{uid}</when>
              <when test="type == 2">u.spread_uid in (select first.uid from eb_user first where first.spread_uid = #{uid})</when>
              <otherwise>(u.spread_uid = #{uid} or u.spread_uid in (select first.uid from eb_user first where first.spread_uid = #{uid}))</otherwise>
            </choose>
            <if test="keywords != null and keywords != ''">
              and (u.uid = #{keywords} or u.nickname like concat('%', #{keywords}, '%') or u.phone = #{keywords})
            </if>
            order by u.uid desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SpreadChildUserResponse> selectSpreadUsers(
            @Param("uid") Integer uid,
            @Param("type") Integer type,
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(*)
            from eb_user_brokerage_record br
            where br.uid = #{uid}
              and br.link_type = 'order'
              and br.status = 3
            <if test="type == 1">and br.brokerage_level = 1</if>
            <if test="type == 2">and br.brokerage_level = 2</if>
            <if test="keywords != null and keywords != ''">and br.link_id like concat('%', #{keywords}, '%')</if>
            <if test="startTime != null and endTime != null">and br.update_time between #{startTime} and #{endTime}</if>
            </script>
            """)
    long countSpreadOrders(
            @Param("uid") Integer uid,
            @Param("type") Integer type,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            <script>
            select o.id,
                   o.order_id as orderId,
                   o.real_name as realName,
                   o.user_phone as userPhone,
                   br.price,
                   br.update_time as updateTime
            from eb_user_brokerage_record br
            join eb_store_order o on o.order_id = br.link_id
            where br.uid = #{uid}
              and br.link_type = 'order'
              and br.status = 3
            <if test="type == 1">and br.brokerage_level = 1</if>
            <if test="type == 2">and br.brokerage_level = 2</if>
            <if test="keywords != null and keywords != ''">and br.link_id like concat('%', #{keywords}, '%')</if>
            <if test="startTime != null and endTime != null">and br.update_time between #{startTime} and #{endTime}</if>
            order by br.update_time desc, br.id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SpreadOrderResponse> selectSpreadOrders(
            @Param("uid") Integer uid,
            @Param("type") Integer type,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("select spread_uid from eb_user where uid = #{uid}")
    Integer selectSpreadUid(@Param("uid") Integer uid);

    @Update("update eb_user set path = '/0/', spread_uid = 0, spread_time = null, update_time = current_timestamp where uid = #{uid}")
    int clearSpread(@Param("uid") Integer uid);

    @Update("update eb_user set spread_count = greatest(coalesce(spread_count, 0) - 1, 0), update_time = current_timestamp where uid = #{uid}")
    int decreaseSpreadCount(@Param("uid") Integer uid);
}
