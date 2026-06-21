package com.jsy.crmeb.modern.service.order.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderAutoReceiptTaskMapper {
    @Select("select value from eb_system_config where name = #{key} limit 1")
    String selectConfigValue(@Param("key") String key);

    @Select("""
            select id
            from eb_store_order
            where status = 1
              and refund_status <> 3
              and update_time <= #{deadline}
              and is_del = 0
            order by id asc
            """)
    List<Integer> selectAwaitTakeDeliveryOrderIds(@Param("deadline") LocalDateTime deadline);

    @Update("""
            update eb_store_order
            set status = 2,
                update_time = now()
            where id = #{id}
              and status = 1
              and refund_status <> 3
              and update_time <= #{deadline}
              and is_del = 0
            """)
    int markAutoTakeDelivery(@Param("id") Integer id, @Param("deadline") LocalDateTime deadline);
}
