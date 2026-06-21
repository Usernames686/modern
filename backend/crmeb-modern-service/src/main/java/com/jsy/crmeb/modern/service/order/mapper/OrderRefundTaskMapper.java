package com.jsy.crmeb.modern.service.order.mapper;

import java.math.BigDecimal;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderRefundTaskMapper {
    @Select("""
            select id,
                   uid,
                   order_id as orderNo,
                   refund_price as refundPrice,
                   coupon_id as couponId,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   refund_status as refundStatus
            from eb_store_order
            where id = #{id}
            limit 1
            """)
    Map<String, Object> selectOrder(@Param("id") Integer id);

    @Select("""
            select coalesce(sum(integral), 0)
            from eb_user_integral_record
            where uid = #{uid}
              and link_id = #{orderNo}
              and link_type = 'order'
              and type = 2
            """)
    Integer selectRefundIntegral(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Update("""
            update eb_user
            set integral = integral + #{integral},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int restoreUserIntegral(@Param("uid") Integer uid, @Param("integral") Integer integral);

    @Select("select integral from eb_user where uid = #{uid}")
    Integer selectUserIntegral(@Param("uid") Integer uid);

    @Insert("""
            insert into eb_user_integral_record(uid, link_id, link_type, type, title, integral, balance, mark, status)
            values(#{uid}, #{orderNo}, 'order', 1, '订单退款', #{integral}, #{balance}, #{mark}, 3)
            """)
    int insertRefundIntegralRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("integral") Integer integral,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Update("""
            update eb_user_integral_record
            set status = 4,
                update_time = current_timestamp
            where uid = #{uid}
              and link_id = #{orderNo}
              and link_type = 'order'
              and type = 1
              and status < 3
            """)
    int invalidateOrderAddIntegral(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Update("""
            update eb_user_brokerage_record
            set status = 4,
                update_time = current_timestamp
            where link_id = #{orderNo}
              and link_type = 'order'
              and status < 3
            """)
    int invalidateBrokerage(@Param("orderNo") String orderNo);

    @Select("""
            select coalesce(sum(experience), 0)
            from eb_user_experience_record
            where uid = #{uid}
              and link_id = #{orderNo}
              and link_type = 'order'
              and type = 1
            """)
    Integer selectRefundExperience(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Update("""
            update eb_user
            set experience = greatest(coalesce(experience, 0) - #{experience}, 0),
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int deductUserExperience(@Param("uid") Integer uid, @Param("experience") Integer experience);

    @Select("select experience from eb_user where uid = #{uid}")
    Integer selectUserExperience(@Param("uid") Integer uid);

    @Insert("""
            insert into eb_user_experience_record(uid, link_id, link_type, type, title, experience, balance, mark, status)
            values(#{uid}, #{orderNo}, 'order', 2, '用户退款', #{experience}, #{balance}, #{mark}, 1)
            """)
    int insertRefundExperienceRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("experience") Integer experience,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Update("""
            update eb_store_order
            set refund_status = 2,
                update_time = current_timestamp
            where id = #{id}
            """)
    int markRefunded(@Param("id") Integer id);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderId}, 'refund_price', #{message}, now())
            """)
    int insertRefundLog(@Param("orderId") Integer orderId, @Param("message") String message);

    @Update("""
            update eb_store_pink
            set status = 3,
                is_refund = 1
            where order_id = #{orderNo}
            """)
    int markPinkRefunded(@Param("orderNo") String orderNo);

    @Update("""
            update eb_store_coupon_user
            set status = 0,
                update_time = current_timestamp
            where id = #{couponId}
            """)
    int restoreCoupon(@Param("couponId") Integer couponId);

    @Select("""
            select count(1)
            from eb_store_order_status
            where oid = #{orderId}
              and change_type = 'refund_price'
              and change_message like concat('%', #{amount}, '%成功%')
            """)
    int countRefundSuccessLog(@Param("orderId") Integer orderId, @Param("amount") BigDecimal amount);
}
