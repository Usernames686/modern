package com.jsy.crmeb.modern.service.order.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderReceiptTaskMapper {
    @Select("""
            select id,
                   uid,
                   order_id as orderId
            from eb_store_order
            where id = #{id}
            limit 1
            """)
    Map<String, Object> selectOrder(@Param("id") Integer id);

    @Select("""
            select count(1)
            from eb_user_brokerage_record
            where link_id = #{orderNo}
              and link_type = 'order'
              and status <> 1
            """)
    int countNonCreateBrokerageRecords(@Param("orderNo") String orderNo);

    @Update("""
            update eb_user_brokerage_record
            set status = 2,
                thaw_time = #{nowMillis} + greatest(coalesce(frozen_time, 0), 0) * 86400000,
                update_time = current_timestamp
            where link_id = #{orderNo}
              and link_type = 'order'
              and status = 1
            """)
    int freezeBrokerageRecords(@Param("orderNo") String orderNo, @Param("nowMillis") long nowMillis);

    @Update("""
            update eb_user_integral_record
            set status = 2,
                thaw_time = #{nowMillis} + greatest(coalesce(frozen_time, 0), 0) * 86400000,
                update_time = current_timestamp
            where link_id = #{orderNo}
              and uid = #{uid}
              and link_type = 'order'
              and type = 1
              and status = 1
            """)
    int freezeIntegralRecords(
            @Param("orderNo") String orderNo,
            @Param("uid") Integer uid,
            @Param("nowMillis") long nowMillis);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderDbId}, 'user_take_delivery', '用户已收货', now())
            """)
    int insertTakeDeliveryLog(@Param("orderDbId") Integer orderDbId);
}
