package com.jsy.crmeb.modern.service.combination.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StorePinkTaskMapper {
    @Select("""
            select p.id,
                   p.cid,
                   p.people,
                   p.order_id_key as orderIdKey,
                   coalesce(c.virtual_ration, 0) as virtualRation,
                   (
                       select count(1)
                       from eb_store_pink member
                       where member.is_refund = 0
                         and member.cid = p.cid
                         and (member.k_id = p.id or member.id = p.id)
                   ) as currentPeople
            from eb_store_pink p
            left join eb_store_combination c on c.id = p.cid
            where p.status = 1
              and p.k_id = 0
              and p.stop_time <= #{nowMillis}
            order by p.id asc
            """)
    List<Map<String, Object>> selectExpiredActiveHeads(@Param("nowMillis") long nowMillis);

    @Update("""
            update eb_store_pink
            set status = 2,
                is_virtual = #{virtual}
            where is_refund = 0
              and cid = #{cid}
              and (k_id = #{headId} or id = #{headId})
              and status = 1
            """)
    int markTeamSuccess(
            @Param("cid") Integer cid,
            @Param("headId") Integer headId,
            @Param("virtual") boolean virtual);

    @Update("""
            update eb_store_pink
            set status = 3
            where is_refund = 0
              and cid = #{cid}
              and (k_id = #{headId} or id = #{headId})
              and status = 1
            """)
    int markTeamFail(@Param("cid") Integer cid, @Param("headId") Integer headId);

    @Update("""
            update eb_store_order order_table
            join eb_store_pink pink on pink.order_id_key = order_table.id
            set order_table.refund_status = 1,
                order_table.refund_reason_time = now(),
                order_table.refund_reason = #{reason},
                order_table.refund_reason_wap_explain = #{explain},
                order_table.refund_reason_wap_img = '',
                order_table.refund_price = 0.00,
                order_table.update_time = now()
            where pink.is_refund = 0
              and pink.cid = #{cid}
              and (pink.k_id = #{headId} or pink.id = #{headId})
              and order_table.refund_status = 0
            """)
    int applyRefundForFailedTeam(
            @Param("cid") Integer cid,
            @Param("headId") Integer headId,
            @Param("reason") String reason,
            @Param("explain") String explain);
}
