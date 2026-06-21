package com.jsy.crmeb.modern.service.finance.mapper;

import com.jsy.crmeb.modern.service.finance.dto.UserRechargeResponse;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRechargeAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_user_recharge r
            where r.paid = 1
              <if test="uid != null">
                and r.uid = #{uid}
              </if>
              <if test="keywords != null and keywords != ''">
                and r.order_id like concat('%', #{keywords}, '%')
              </if>
              <if test="startTime != null and startTime != ''">
                and r.create_time &gt;= #{startTime}
              </if>
              <if test="endTime != null and endTime != ''">
                and r.create_time &lt;= #{endTime}
              </if>
            </script>
            """)
    long countRecharge(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            <script>
            select r.id,
                   r.uid,
                   r.order_id as orderId,
                   r.price,
                   r.give_price as givePrice,
                   r.recharge_type as rechargeType,
                   r.paid,
                   r.pay_time as payTime,
                   r.create_time as createTime,
                   r.refund_price as refundPrice,
                   coalesce(u.avatar, '') as avatar,
                   coalesce(u.nickname, '') as nickname
            from eb_user_recharge r
            left join eb_user u on u.uid = r.uid
            where r.paid = 1
              <if test="uid != null">
                and r.uid = #{uid}
              </if>
              <if test="keywords != null and keywords != ''">
                and r.order_id like concat('%', #{keywords}, '%')
              </if>
              <if test="startTime != null and startTime != ''">
                and r.create_time &gt;= #{startTime}
              </if>
              <if test="endTime != null and endTime != ''">
                and r.create_time &lt;= #{endTime}
              </if>
            order by r.id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<UserRechargeResponse> selectRecharge(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select coalesce(sum(price), 0)
            from eb_user_recharge
            where paid = 1
              <if test="type != null and type != ''">
                and recharge_type = #{type}
              </if>
            </script>
            """)
    BigDecimal sumByType(@Param("type") String type);

    @Select("""
            select coalesce(sum(refund_price), 0)
            from eb_user_recharge
            where refund_price > 0
              and paid = 1
            """)
    BigDecimal sumRefund();
}
