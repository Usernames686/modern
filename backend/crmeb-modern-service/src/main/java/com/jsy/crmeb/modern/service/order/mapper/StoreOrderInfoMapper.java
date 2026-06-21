package com.jsy.crmeb.modern.service.order.mapper;

import com.jsy.crmeb.modern.service.order.dto.StoreOrderInfoRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StoreOrderInfoMapper {
    @Select("""
            <script>
            select id,
                   order_id as orderId,
                   product_id as productId,
                   `unique`,
                   image,
                   attr_value_id as attrValueId,
                   product_name as productName,
                   sku,
                   price,
                   pay_num as payNum
            from eb_store_order_info
            where order_id in
            <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>#{orderId}</foreach>
            order by id asc
            </script>
            """)
    List<StoreOrderInfoRow> selectByOrderIds(@Param("orderIds") List<Integer> orderIds);
}
