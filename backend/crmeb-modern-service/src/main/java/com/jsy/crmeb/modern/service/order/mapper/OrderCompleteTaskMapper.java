package com.jsy.crmeb.modern.service.order.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderCompleteTaskMapper {
    @Select("""
            select id,
                   uid,
                   order_id as orderNo
            from eb_store_order
            where paid = 1
              and status = 2
              and refund_status = 0
              and is_del = 0
              and coalesce(is_system_del, 0) = 0
            order by id asc
            """)
    List<Map<String, Object>> selectReceiptOrders();

    @Select("""
            select change_type as changeType,
                   create_time as createTime
            from eb_store_order_status
            where oid = #{orderId}
            order by create_time desc
            limit 1
            """)
    Map<String, Object> selectLastOrderStatus(@Param("orderId") Integer orderId);

    @Select("""
            select id,
                   product_id as productId,
                   `unique`,
                   sku,
                   is_reply as isReply
            from eb_store_order_info
            where order_id = #{orderId}
            order by id asc
            """)
    List<Map<String, Object>> selectOrderInfo(@Param("orderId") Integer orderId);

    @Select("""
            select nickname,
                   avatar,
                   account
            from eb_user
            where uid = #{uid}
            limit 1
            """)
    Map<String, Object> selectUser(@Param("uid") Integer uid);

    @Select("""
            select count(1)
            from eb_store_product_reply
            where oid = #{orderId}
              and product_id = #{productId}
              and `unique` = #{unique}
              and is_del = 0
            """)
    int countProductReply(
            @Param("orderId") Integer orderId,
            @Param("productId") Integer productId,
            @Param("unique") String unique);

    @Insert("""
            insert into eb_store_product_reply(
                uid, oid, `unique`, product_id, reply_type,
                product_score, service_score, comment, pics,
                nickname, avatar, sku
            ) values (
                #{uid}, #{orderId}, #{unique}, #{productId}, 'product',
                5, 5, '', '', #{nickname}, #{avatar}, #{sku}
            )
            """)
    int insertAutoReply(
            @Param("uid") Integer uid,
            @Param("orderId") Integer orderId,
            @Param("unique") String unique,
            @Param("productId") Integer productId,
            @Param("nickname") String nickname,
            @Param("avatar") String avatar,
            @Param("sku") String sku);

    @Update("""
            update eb_store_order_info
            set is_reply = 1,
                update_time = current_timestamp
            where id = #{id}
            """)
    int markOrderInfoReplied(@Param("id") Integer id);

    @Update("""
            update eb_store_order
            set status = 3,
                update_time = current_timestamp
            where id = #{orderId}
              and paid = 1
              and status = 2
              and refund_status = 0
              and is_del = 0
              and coalesce(is_system_del, 0) = 0
            """)
    int markOrderComplete(@Param("orderId") Integer orderId);

    @Select("select count(1) from eb_store_order_status where oid = #{orderId} and change_type = 'check_order_over'")
    int countCompleteLog(@Param("orderId") Integer orderId);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderId}, 'check_order_over', '用户评价', now())
            """)
    int insertCompleteLog(@Param("orderId") Integer orderId);

    @Select("select id from eb_store_order where id = #{orderId} limit 1")
    Integer selectOrderId(@Param("orderId") Integer orderId);
}
