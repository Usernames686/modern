package com.jsy.crmeb.modern.service.front.mapper;

import com.jsy.crmeb.modern.service.finance.entity.UserExtract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FrontUserCenterMapper {
    @Select("select value from eb_system_config where name = #{name} limit 1")
    String configValue(@Param("name") String name);

    @Select("""
            select coalesce(sum(number), 0)
            from eb_user_bill
            where uid = #{uid}
              and status = 1
              and category = 'now_money'
              and pm = #{pm}
            """)
    BigDecimal sumBalanceBill(@Param("uid") Integer uid, @Param("pm") Integer pm);

    @Select("""
            select count(*)
            from eb_user_bill
            where uid = #{uid}
              and status = 1
              and category = 'now_money'
              and (#{pm} is null or pm = #{pm})
            """)
    long countBalanceBills(@Param("uid") Integer uid, @Param("pm") Integer pm);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   pm,
                   title,
                   category,
                   type,
                   number,
                   balance,
                   mark,
                   status,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as add_time,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime,
                   date_format(create_time, '%Y-%m') as billMonth
            from eb_user_bill
            where uid = #{uid}
              and status = 1
              and category = 'now_money'
              and (#{pm} is null or pm = #{pm})
            order by create_time desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectBalanceBills(
            @Param("uid") Integer uid,
            @Param("pm") Integer pm,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select coalesce(sum(price + give_price), 0)
            from eb_user_recharge
            where uid = #{uid}
              and paid = 1
            """)
    BigDecimal sumRecharge(@Param("uid") Integer uid);

    @Select("""
            select coalesce(sum(pay_price), 0)
            from eb_store_order
            where uid = #{uid}
              and paid = 1
              and is_del = 0
              and refund_status <> 2
            """)
    BigDecimal sumPaidOrders(@Param("uid") Integer uid);

    @Select("""
            select count(*)
            from eb_user_integral_record
            where uid = #{uid}
              and status = 3
            """)
    long countIntegralRecords(@Param("uid") Integer uid);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   integral,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime
            from eb_user_integral_record
            where uid = #{uid}
              and status = 3
            order by update_time desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectIntegralRecords(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select coalesce(sum(integral), 0)
            from eb_user_integral_record
            where uid = #{uid}
              and status = 3
              and type = #{type}
              and (#{linkTypesCsv} is null or find_in_set(link_type, #{linkTypesCsv}) > 0)
            """)
    Integer sumIntegral(
            @Param("uid") Integer uid,
            @Param("type") Integer type,
            @Param("linkTypesCsv") String linkTypesCsv);

    @Select("""
            select coalesce(sum(integral), 0)
            from eb_user_integral_record
            where uid = #{uid}
              and status = 2
              and type = 1
              and link_type = 'order'
            """)
    Integer frozenIntegral(@Param("uid") Integer uid);

    @Select("""
            select id,
                   gid,
                   value,
                   sort,
                   status
            from eb_system_group_data
            where gid = #{gid}
              and status = 1
            order by sort desc, id desc
            """)
    List<Map<String, Object>> selectGroupDataByGid(@Param("gid") Integer gid);

    @Select("""
            select id,
                   gid,
                   value,
                   sort,
                   status
            from eb_system_group_data
            where id = #{id}
              and gid = #{gid}
              and status = 1
            limit 1
            """)
    Map<String, Object> selectGroupDataById(@Param("gid") Integer gid, @Param("id") Integer id);

    @Insert("""
            insert into eb_user_recharge(
                uid, order_id, price, give_price, recharge_type, paid,
                refund_price, is_wechat_shipping, out_trade_no
            ) values (
                #{uid}, #{orderId}, #{price}, #{givePrice}, #{rechargeType}, 0,
                0, 0, ''
            )
            """)
    int insertRechargeDryRun(
            @Param("uid") Integer uid,
            @Param("orderId") String orderId,
            @Param("price") BigDecimal price,
            @Param("givePrice") BigDecimal givePrice,
            @Param("rechargeType") String rechargeType);

    @Select("""
            select id,
                   uid,
                   title,
                   number,
                   balance,
                   type,
                   date_format(create_day, '%Y-%m-%d') as createDay,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime
            from eb_user_sign
            where uid = #{uid}
              and type = 1
            order by create_day desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectSignList(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("select count(*) from eb_user_sign where uid = #{uid} and type = 1")
    long countSigns(@Param("uid") Integer uid);

    @Select("""
            select create_day
            from eb_user_sign
            where uid = #{uid}
              and type = 1
            order by create_day desc, id desc
            limit 1
            """)
    LocalDate selectLastSignDay(@Param("uid") Integer uid);

    @Select("""
            select count(*)
            from eb_user_sign
            where uid = #{uid}
              and type = 1
              and create_day = #{day}
            """)
    int countSignByDay(@Param("uid") Integer uid, @Param("day") LocalDate day);

    @Insert("""
            insert into eb_user_sign(uid, title, number, balance, type, create_day)
            values(#{uid}, '签到积分奖励', #{number}, #{balance}, 1, curdate())
            """)
    int insertSign(
            @Param("uid") Integer uid,
            @Param("number") Integer number,
            @Param("balance") Integer balance);

    @Insert("""
            insert into eb_user_integral_record(uid, link_id, link_type, type, title, integral, balance, mark, status)
            values(#{uid}, '0', 'sign', 1, '签到积分奖励', #{integral}, #{balance}, #{mark}, 3)
            """)
    int insertSignIntegralRecord(
            @Param("uid") Integer uid,
            @Param("integral") Integer integral,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_experience_record(uid, link_id, link_type, type, title, experience, balance, mark, status)
            values(#{uid}, '0', 'sign', 1, '签到经验奖励', #{experience}, #{balance}, #{mark}, 1)
            """)
    int insertSignExperienceRecord(
            @Param("uid") Integer uid,
            @Param("experience") Integer experience,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Update("""
            update eb_user
            set sign_num = #{signNum},
                integral = #{integral},
                experience = #{experience},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int updateSignReward(
            @Param("uid") Integer uid,
            @Param("signNum") Integer signNum,
            @Param("integral") Integer integral,
            @Param("experience") Integer experience);

    @Update("update eb_user set sign_num = 0, update_time = current_timestamp where uid = #{uid}")
    int resetSignNum(@Param("uid") Integer uid);

    @Select("""
            select id,
                   name,
                   experience,
                   icon,
                   grade,
                   discount
            from eb_system_user_level
            where is_show = 1
              and is_del = 0
            order by grade asc, id asc
            """)
    List<Map<String, Object>> selectVisibleUserLevels();

    @Select("""
            select count(*)
            from eb_user_experience_record
            where uid = #{uid}
            """)
    long countExperienceRecords(@Param("uid") Integer uid);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   experience,
                   balance,
                   mark,
                   status,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime
            from eb_user_experience_record
            where uid = #{uid}
            order by id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectExperienceRecords(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where uid = #{uid}
              and type = 1
              and link_type = 'order'
              and status = 3
              and date(update_time) = date_sub(curdate(), interval 1 day)
            """)
    BigDecimal sumYesterdayBrokerage(@Param("uid") Integer uid);

    @Select("""
            select coalesce(sum(extract_price), 0)
            from eb_user_extract
            where uid = #{uid}
              and status = 1
            """)
    BigDecimal sumUserExtracted(@Param("uid") Integer uid);

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where uid = #{uid}
              and link_type = 'order'
              and status = 2
            """)
    BigDecimal sumFrozenBrokerage(@Param("uid") Integer uid);

    @Insert("""
            insert into eb_user_extract(
                uid, real_name, extract_type, bank_code, bank_address, alipay_code,
                extract_price, mark, balance, fail_msg, status, wechat,
                create_time, update_time, bank_name, qrcode_url
            ) values (
                #{uid}, #{realName}, #{extractType}, #{bankCode}, #{bankAddress}, #{alipayCode},
                #{extractPrice}, #{mark}, #{balance}, #{failMsg}, #{status}, #{wechat},
                #{createTime}, #{updateTime}, #{bankName}, #{qrcodeUrl}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserExtract(UserExtract extract);

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where uid = #{uid}
              and status in (3, 5)
            """)
    BigDecimal sumCompletedBrokerage(@Param("uid") Integer uid);

    @Select("""
            select count(distinct date_format(update_time, '%Y-%m'))
            from eb_user_brokerage_record
            where uid = #{uid}
              and status in (3, 5)
            """)
    long countBrokerageRecordMonths(@Param("uid") Integer uid);

    @Select("""
            select date_format(update_time, '%Y-%m') as date
            from eb_user_brokerage_record
            where uid = #{uid}
              and status in (3, 5)
            group by date_format(update_time, '%Y-%m')
            order by date desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectBrokerageRecordMonths(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   price,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   brokerage_level as brokerageLevel,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime
            from eb_user_brokerage_record
            where uid = #{uid}
              and status in (3, 5)
              and date_format(update_time, '%Y-%m') = #{month}
            order by update_time desc, id desc
            """)
    List<Map<String, Object>> selectBrokerageRecordsByMonth(
            @Param("uid") Integer uid,
            @Param("month") String month);

    @Select("""
            select count(distinct date_format(create_time, '%Y-%m'))
            from eb_user_extract
            where uid = #{uid}
            """)
    long countExtractRecordMonths(@Param("uid") Integer uid);

    @Select("""
            select date_format(create_time, '%Y-%m') as date
            from eb_user_extract
            where uid = #{uid}
            group by date_format(create_time, '%Y-%m')
            order by date desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectExtractRecordMonths(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select id,
                   uid,
                   real_name as realName,
                   extract_type as extractType,
                   bank_code as bankCode,
                   bank_address as bankAddress,
                   alipay_code as alipayCode,
                   extract_price as extractPrice,
                   mark,
                   balance,
                   fail_msg as failMsg,
                   status,
                   wechat,
                   bank_name as bankName,
                   qrcode_url as qrcodeUrl,
                   date_format(create_time, '%Y-%m-%d %H:%i:%s') as createTime,
                   date_format(update_time, '%Y-%m-%d %H:%i:%s') as updateTime,
                   date_format(fail_time, '%Y-%m-%d %H:%i:%s') as failTime
            from eb_user_extract
            where uid = #{uid}
              and date_format(create_time, '%Y-%m') = #{month}
            order by create_time desc, id desc
            """)
    List<Map<String, Object>> selectExtractRecordsByMonth(
            @Param("uid") Integer uid,
            @Param("month") String month);

    @Select("""
            select count(*)
            from eb_user_brokerage_record br
            inner join eb_store_order o on o.order_id = br.link_id
            where br.uid = #{uid}
              and br.link_type = 'order'
              and br.type = 1
              and br.status = 3
              and o.is_del = 0
            """)
    long countSpreadOrders(@Param("uid") Integer uid);

    @Select("""
            select count(*)
            from eb_user_brokerage_record br
            inner join eb_store_order o on o.order_id = br.link_id
            where br.uid = #{uid}
              and br.link_type = 'order'
              and br.type = 1
              and br.status = 3
              and o.is_del = 0
              and date_format(br.update_time, '%Y-%m') = #{month}
            """)
    long countSpreadOrdersByMonth(@Param("uid") Integer uid, @Param("month") String month);

    @Select("""
            select br.link_id as orderId,
                   date_format(br.update_time, '%Y-%m-%d %H:%i:%s') as time,
                   date_format(br.update_time, '%Y-%m') as timeMonth,
                   br.price as number,
                   coalesce(u.avatar, '') as avatar,
                   coalesce(u.nickname, concat('用户', o.uid)) as nickname
            from eb_user_brokerage_record br
            inner join eb_store_order o on o.order_id = br.link_id
            left join eb_user u on u.uid = o.uid
            where br.uid = #{uid}
              and br.link_type = 'order'
              and br.type = 1
              and br.status = 3
              and o.is_del = 0
            order by br.update_time desc, br.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectSpreadOrders(
            @Param("uid") Integer uid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select u.spread_uid as uid,
                   coalesce(p.avatar, '') as avatar,
                   case
                     when coalesce(p.nickname, '') <> '' then p.nickname
                     when coalesce(p.phone, '') <> '' and length(p.phone) >= 7 then concat(substr(p.phone, 1, 2), '****', substr(p.phone, 8))
                     else concat('用户', u.spread_uid)
                   end as nickname,
                   count(u.spread_count) as spreadCount
            from eb_user u
            inner join eb_user p on p.uid = u.spread_uid
            where u.spread_uid > 0
              and u.status = 1
              and (#{startTime} is null or u.spread_time between #{startTime} and #{endTime})
            group by u.spread_uid, p.avatar, p.nickname, p.phone
            order by spreadCount desc, u.spread_uid asc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectSpreadRank(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select br.uid,
                   coalesce(u.avatar, '') as avatar,
                   case
                     when coalesce(u.nickname, '') <> '' then u.nickname
                     when coalesce(u.phone, '') <> '' and length(u.phone) >= 7 then concat(substr(u.phone, 1, 2), '****', substr(u.phone, 8))
                     else concat('用户', br.uid)
                   end as nickname,
                   coalesce(sum(br.price), 0) as brokeragePrice
            from eb_user_brokerage_record br
            inner join eb_user u on u.uid = br.uid
            where br.link_type = 'order'
              and br.status = 3
              and (#{startTime} is null or br.update_time between #{startTime} and #{endTime})
            group by br.uid, u.avatar, u.nickname, u.phone
            having brokeragePrice > 0
            order by brokeragePrice desc, br.uid asc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectBrokerageRank(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select ranked.rankNo
            from (
                select br.uid,
                       row_number() over(order by coalesce(sum(br.price), 0) desc, br.uid asc) as rankNo
                from eb_user_brokerage_record br
                where br.link_type = 'order'
                  and br.status = 3
                  and (#{startTime} is null or br.update_time between #{startTime} and #{endTime})
                group by br.uid
                having coalesce(sum(br.price), 0) > 0
            ) ranked
            where ranked.uid = #{uid}
            """)
    Integer selectBrokerageRankNumber(
            @Param("uid") Integer uid,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select uid
            from eb_user
            where spread_uid = #{uid}
              and is_logoff = 0
            order by spread_time desc, uid desc
            """)
    List<Integer> selectSpreadPeopleIds(@Param("uid") Integer uid);

    @Select("""
            <script>
            select uid
            from eb_user
            where spread_uid in
            <foreach collection="parentIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
              and is_logoff = 0
            order by spread_time desc, uid desc
            </script>
            """)
    List<Integer> selectSpreadPeopleIdsByParents(@Param("parentIds") List<Integer> parentIds);

    @Select("""
            select count(*)
            from eb_user
            where spread_uid = #{uid}
              and is_logoff = 0
            """)
    long countFirstSpreadPeople(@Param("uid") Integer uid);

    @Select("""
            select count(*)
            from eb_user
            where spread_uid in (
                select uid
                from eb_user
                where spread_uid = #{uid}
                  and is_logoff = 0
            )
              and is_logoff = 0
            """)
    long countSecondSpreadPeople(@Param("uid") Integer uid);

    @Select("""
            <script>
            select count(*)
            from eb_user u
            where u.uid in
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            <if test="keyword != null and keyword != ''">
              and (u.real_name like concat('%', #{keyword}, '%') or u.nickname like concat('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countSpreadPeopleList(
            @Param("ids") List<Integer> ids,
            @Param("keyword") String keyword);

    @Select("""
            <script>
            select u.uid,
                   u.nickname,
                   u.avatar,
                   date_format(u.spread_time, '%Y-%m-%d %H:%i:%s') as time,
                   u.spread_count as childCount,
                   (
                       select count(*)
                       from eb_store_order o
                       right join eb_user_brokerage_record br
                         on br.link_id = o.order_id
                        and br.status = 3
                       where o.uid = u.uid
                         and br.uid = u.spread_uid
                         and o.status &gt; 1
                   ) as orderCount,
                   (
                       select coalesce(sum(o.pay_price), 0)
                       from eb_store_order o
                       right join eb_user_brokerage_record br
                         on br.link_id = o.order_id
                        and br.status = 3
                       where o.uid = u.uid
                         and br.uid = u.spread_uid
                         and o.status &gt; 1
                   ) as numberCount
            from eb_user u
            where u.uid in
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            <if test="keyword != null and keyword != ''">
              and (u.real_name like concat('%', #{keyword}, '%') or u.nickname like concat('%', #{keyword}, '%'))
            </if>
            order by ${sortSql}
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectSpreadPeopleList(
            @Param("ids") List<Integer> ids,
            @Param("keyword") String keyword,
            @Param("sortSql") String sortSql,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select spread_uid
            from eb_user
            where uid = #{uid}
              and is_logoff = 0
            """)
    Integer selectUserSpreadUid(@Param("uid") Integer uid);

    @Insert("""
            insert into eb_user_visit_record(date, uid, visit_type)
            values(#{date}, #{uid}, #{visitType})
            """)
    int insertVisitRecord(
            @Param("date") String date,
            @Param("uid") Integer uid,
            @Param("visitType") Integer visitType);
}
