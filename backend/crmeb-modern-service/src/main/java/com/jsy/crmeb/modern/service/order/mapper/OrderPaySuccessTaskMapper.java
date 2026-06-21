package com.jsy.crmeb.modern.service.order.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderPaySuccessTaskMapper {
    @Select("""
            select id,
                   order_id as orderNo,
                   uid,
                   pay_price as payPrice,
                   use_integral as useIntegral,
                   total_num as totalNum,
                   total_price as totalPrice,
                   combination_id as combinationId,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   pink_id as pinkId,
                   pay_type as payType,
                   is_channel as isChannel,
                   paid
            from eb_store_order
            where order_id = #{orderNo}
            limit 1
            """)
    Map<String, Object> selectOrderByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            select uid,
                   now_money as nowMoney,
                   integral,
                   experience,
                   spread_uid as spreadUid,
                   is_promoter as isPromoter
            from eb_user
            where uid = #{uid}
            for update
            """)
    Map<String, Object> selectUserForUpdate(@Param("uid") Integer uid);

    @Select("select value from eb_system_config where name = #{name} limit 1")
    String selectConfigValue(@Param("name") String name);

    @Select("""
            select count(1)
            from eb_store_order_status
            where oid = #{orderId}
              and change_type = 'pay_success'
            """)
    int countPaySuccessLog(@Param("orderId") Integer orderId);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderId}, 'pay_success', '用户付款成功', now())
            """)
    int insertPaySuccessLog(@Param("orderId") Integer orderId);

    @Insert("""
            insert into eb_user_bill(uid, link_id, pm, title, category, type, number, balance, mark, status)
            values(#{uid}, #{orderId}, 0, '购买商品', 'now_money', 'pay_order', #{amount}, #{balance}, #{mark}, 1)
            """)
    int insertPayBill(
            @Param("uid") Integer uid,
            @Param("orderId") Integer orderId,
            @Param("amount") BigDecimal amount,
            @Param("balance") BigDecimal balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_integral_record(uid, link_id, link_type, type, title, integral, balance, mark, status)
            values(#{uid}, #{orderNo}, 'order', 2, '订单', #{integral}, #{balance}, #{mark}, 3)
            """)
    int insertIntegralSubRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("integral") Integer integral,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_integral_record(uid, link_id, link_type, type, title, integral, balance, mark, status, frozen_time)
            values(#{uid}, #{orderNo}, 'order', 1, '订单', #{integral}, #{balance}, #{mark}, 1, #{frozenTime})
            """)
    int insertIntegralAddRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("integral") Integer integral,
            @Param("balance") Integer balance,
            @Param("mark") String mark,
            @Param("frozenTime") Integer frozenTime);

    @Insert("""
            insert into eb_user_experience_record(uid, link_id, link_type, type, title, experience, balance, mark, status)
            values(#{uid}, #{orderNo}, 'order', 1, '订单', #{experience}, #{balance}, #{mark}, 1)
            """)
    int insertExperienceRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("experience") Integer experience,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Update("""
            update eb_user
            set experience = coalesce(experience, 0) + #{experience},
                pay_count = coalesce(pay_count, 0) + 1,
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int updateUserAfterPay(@Param("uid") Integer uid, @Param("experience") Integer experience);

    @Update("""
            update eb_user
            set is_promoter = 1,
                promoter_time = current_timestamp,
                update_time = current_timestamp
            where uid = #{uid}
              and coalesce(is_promoter, 0) = 0
            """)
    int markPromoter(@Param("uid") Integer uid);

    @Select("""
            select coalesce(sum(give_integral * pay_num), 0)
            from eb_store_order_info
            where order_id = #{orderId}
              and product_type = 0
            """)
    Integer sumProductGiveIntegral(@Param("orderId") Integer orderId);

    @Select("""
            select id,
                   product_id as productId,
                   attr_value_id as attrValueId,
                   price,
                   vip_price as vipPrice,
                   pay_num as payNum,
                   is_sub as isSub
            from eb_store_order_info
            where order_id = #{orderId}
            order by id asc
            """)
    List<Map<String, Object>> selectOrderInfo(@Param("orderId") Integer orderId);

    @Select("""
            select uid,
                   spread_uid as spreadUid,
                   is_promoter as isPromoter
            from eb_user
            where uid = #{uid}
            limit 1
            """)
    Map<String, Object> selectSpreadUser(@Param("uid") Integer uid);

    @Select("""
            select brokerage,
                   brokerage_two as brokerageTwo
            from eb_store_product_attr_value
            where id = #{attrValueId}
            limit 1
            """)
    Map<String, Object> selectAttrBrokerage(@Param("attrValueId") Integer attrValueId);

    @Insert("""
            insert into eb_user_brokerage_record(
                uid, link_id, link_type, type, title, price, balance, mark,
                status, frozen_time, brokerage_level
            ) values (
                #{uid}, #{orderNo}, 'order', 1, '订单返佣', #{price}, 0.00, #{mark},
                1, #{frozenTime}, #{brokerageLevel}
            )
            """)
    int insertBrokerageRecord(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("price") BigDecimal price,
            @Param("mark") String mark,
            @Param("frozenTime") Integer frozenTime,
            @Param("brokerageLevel") Integer brokerageLevel);

    @Select("""
            select p.id,
                   p.uid,
                   p.cid,
                   p.k_id as kId,
                   p.people,
                   p.status,
                   p.is_refund as isRefund
            from eb_store_pink p
            where p.id = #{pinkId}
            limit 1
            """)
    Map<String, Object> selectPink(@Param("pinkId") Integer pinkId);

    @Select("""
            select count(1)
            from eb_store_pink
            where k_id = #{pinkId}
              and is_refund = 0
            """)
    int countPinkMembers(@Param("pinkId") Integer pinkId);

    @Select("""
            select id
            from eb_store_pink
            where cid = #{cid}
              and k_id = #{kid}
              and is_refund = 0
            union
            select id
            from eb_store_pink
            where id = #{kid}
              and is_refund = 0
            """)
    List<Integer> selectPinkGroupIds(@Param("cid") Integer cid, @Param("kid") Integer kid);

    @Update("""
            <script>
            update eb_store_pink
            set status = 2
            where id in
            <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    int markPinkGroupSuccess(@Param("ids") List<Integer> ids);
}
