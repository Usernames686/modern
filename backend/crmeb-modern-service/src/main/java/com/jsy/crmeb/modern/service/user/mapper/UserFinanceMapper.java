package com.jsy.crmeb.modern.service.user.mapper;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFinanceMapper {
    @Insert("""
            insert into eb_user_bill(uid, link_id, pm, title, category, type, number, balance, mark, status)
            values(#{uid}, '0', #{pm}, '后台操作', 'now_money', #{type}, #{number}, #{balance}, #{mark}, 1)
            """)
    int insertUserBill(
            @Param("uid") Integer uid,
            @Param("pm") Integer pm,
            @Param("type") String type,
            @Param("number") BigDecimal number,
            @Param("balance") BigDecimal balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_bill(uid, link_id, pm, title, category, type, number, balance, mark, status)
            values(#{uid}, #{orderDbId}, 1, '商品退款', 'now_money', 'pay_product_refund', #{amount}, #{balance}, #{mark}, 1)
            """)
    int insertRefundBill(
            @Param("uid") Integer uid,
            @Param("orderDbId") Integer orderDbId,
            @Param("amount") BigDecimal amount,
            @Param("balance") BigDecimal balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_integral_record(uid, link_id, link_type, type, title, integral, balance, mark, status)
            values(#{uid}, '0', 'system', #{type}, '后台积分操作', #{integral}, #{balance}, #{mark}, 3)
            """)
    int insertIntegralRecord(
            @Param("uid") Integer uid,
            @Param("type") Integer type,
            @Param("integral") Integer integral,
            @Param("balance") Integer balance,
            @Param("mark") String mark);

    @Insert("""
            insert into eb_user_level(uid, level_id, grade, status, mark, discount)
            values(#{uid}, #{levelId}, #{grade}, 1, #{mark}, #{discount})
            """)
    int insertUserLevel(
            @Param("uid") Integer uid,
            @Param("levelId") Integer levelId,
            @Param("grade") Integer grade,
            @Param("mark") String mark,
            @Param("discount") Integer discount);
}
