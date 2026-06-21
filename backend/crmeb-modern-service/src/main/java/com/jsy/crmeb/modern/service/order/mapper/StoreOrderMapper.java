package com.jsy.crmeb.modern.service.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderTimeResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreStaffDetailResponse;
import com.jsy.crmeb.modern.service.order.entity.StoreOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreOrderMapper extends BaseMapper<StoreOrder> {
    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderDbId}, #{changeType}, #{message}, now())
            """)
    int insertOrderStatus(
            @Param("orderDbId") Integer orderDbId,
            @Param("changeType") String changeType,
            @Param("message") String message);

    @Update("""
            update eb_user
            set now_money = now_money + #{amount},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int addUserBalance(@Param("uid") Integer uid, @Param("amount") BigDecimal amount);

    @Select("""
            <script>
            select coalesce(sum(o.pay_price), 0) as price,
                   count(o.id) as count,
                   date_format(o.create_time, '%Y-%m-%d') as time
            from eb_store_order o
            where o.is_del = 0
              and o.paid = 1
              and o.refund_status = 0
            <if test="startTime != null">
              and o.create_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              and o.create_time &lt; #{endTime}
            </if>
            group by date_format(o.create_time, '%Y-%m-%d')
            order by o.create_time desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<StoreStaffDetailResponse> selectOrderVerificationDetail(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select date_format(o.pay_time, #{dateFormat}) as time,
                   <choose>
                     <when test="type == 1">coalesce(sum(o.pay_price), 0)</when>
                     <otherwise>count(o.id)</otherwise>
                   </choose> as num
            from eb_store_order o
            where o.is_del = 0
              and o.paid = 1
              and o.refund_status = 0
            <if test="startTime != null">
              and o.pay_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              and o.pay_time &lt; #{endTime}
            </if>
            group by date_format(o.pay_time, #{dateFormat})
            order by min(o.pay_time) asc
            </script>
            """)
    List<StoreOrderTimeResponse.StoreOrderTimeChartItem> selectOrderTimeChart(
            @Param("type") int type,
            @Param("dateFormat") String dateFormat,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("select name from eb_express where code = #{code} limit 1")
    String selectExpressName(@Param("code") String code);

    @Select("""
            select s.oid,
                   s.change_type as changeType,
                   s.change_message as changeMessage,
                   s.create_time as createTime
            from eb_store_order_status s
            inner join eb_store_order o on o.id = s.oid
            where o.order_id = #{orderNo}
            order by s.create_time desc
            limit #{offset}, #{limit}
            """)
    List<Map<String, Object>> selectOrderStatusList(
            @Param("orderNo") String orderNo,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select count(1)
            from eb_store_order_status s
            inner join eb_store_order o on o.id = s.oid
            where o.order_id = #{orderNo}
            """)
    long countOrderStatusList(@Param("orderNo") String orderNo);

    @Select("""
            <script>
            select s.oid,
                   max(s.create_time) as deliveryTime
            from eb_store_order_status s
            where s.change_type = 'delivery'
              and s.oid in
              <foreach collection="orderIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            group by s.oid
            </script>
            """)
    List<Map<String, Object>> selectDeliveryTimes(@Param("orderIds") List<Integer> orderIds);
}
