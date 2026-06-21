package com.jsy.crmeb.modern.service.dashboard.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.jsy.crmeb.modern.service.dashboard.dto.DashboardChartPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardAdminMapper {
    @Select("""
            select coalesce(sum(pay_price), 0)
            from eb_store_order
            where paid = 1
              and date_format(create_time, '%Y-%m-%d') = #{date}
            """)
    BigDecimal sumPaidOrderAmountByDate(@Param("date") String date);

    @Select("""
            select count(1)
            from eb_store_order
            where paid = 1
              and date_format(create_time, '%Y-%m-%d') = #{date}
            """)
    Integer countPaidOrderByDate(@Param("date") String date);

    @Select("""
            select count(1)
            from eb_user_visit_record
            where date = #{date}
            """)
    Integer countPageviewsByDate(@Param("date") String date);

    @Select("""
            select count(1)
            from eb_user
            where date_format(create_time, '%Y-%m-%d') = #{date}
            """)
    Integer countNewUserByDate(@Param("date") String date);

    @Select("""
            select count(1)
            from eb_store_order
            where paid = 1
              and status = 0
              and refund_status = 0
              and shipping_type = 1
              and is_del = 0
              and is_system_del = 0
            """)
    Integer countNotShippingOrders();

    @Select("""
            select count(1)
            from eb_store_order
            where paid = 1
              and refund_status in (1, 3)
              and is_del = 0
              and is_system_del = 0
            """)
    Integer countRefundingOrders();

    @Select("""
            select count(1)
            from eb_store_order
            where paid = 1
              and status = 0
              and refund_status = 0
              and shipping_type = 2
              and is_del = 0
              and is_system_del = 0
            """)
    Integer countNotWriteOffOrders();

    @Select("""
            select count(1)
            from eb_store_product
            where stock <= #{stock}
              and is_recycle = 0
              and is_del = 0
            """)
    Integer countVigilanceInventory(@Param("stock") int stock);

    @Select("""
            select count(1)
            from eb_store_product
            where is_show = 1
              and is_recycle = 0
              and is_del = 0
            """)
    Integer countOnSaleProducts();

    @Select("""
            select count(1)
            from eb_store_product
            where is_show = 0
              and is_recycle = 0
              and is_del = 0
            """)
    Integer countNotSaleProducts();

    @Select("""
            select count(1)
            from eb_user_extract
            where status = 0
            """)
    Integer countNotAuditExtracts();

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_recharge
            where paid = 1
            """)
    BigDecimal sumRechargeAmount();

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where link_type = 'yue'
              and type = 2
              and status = 3
            """)
    BigDecimal sumBrokerageToBalanceAmount();

    @Select("""
            select value
            from eb_system_config
            where name = #{key}
            limit 1
            """)
    String selectConfigValue(@Param("key") String key);

    @Select("""
            select date_format(create_time, '%Y-%m-%d') as label,
                   coalesce(sum(pay_price), 0) as price,
                   count(id) as countValue
            from eb_store_order
            where create_time >= #{startTime}
              and create_time <= #{endTime}
            group by date_format(create_time, '%Y-%m-%d')
            order by label asc
            """)
    List<DashboardChartPoint> selectOrderTrendByDay(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select date_format(create_time, '%Y-%m') as label,
                   coalesce(sum(pay_price), 0) as price,
                   count(id) as countValue
            from eb_store_order
            where create_time >= #{startTime}
              and create_time <= #{endTime}
            group by date_format(create_time, '%Y-%m')
            order by label asc
            """)
    List<DashboardChartPoint> selectOrderTrendByMonth(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select date_format(create_time, '%Y-%m-%d') as label,
                   count(uid) as countValue
            from eb_user
            where create_time >= #{startTime}
              and create_time <= #{endTime}
            group by date_format(create_time, '%Y-%m-%d')
            order by label asc
            """)
    List<DashboardChartPoint> selectNewUserTrendByDay(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select count(1) as productCount,
                   coalesce(sum(case when is_show = 1 then 1 else 0 end), 0) as onSaleProductNum,
                   coalesce(sum(case when is_show = 0 then 1 else 0 end), 0) as offSaleProductNum,
                   coalesce(sum(stock), 0) as stockNum,
                   coalesce(sum(sales), 0) as salesNum,
                   coalesce(sum(browse), 0) as browseNum,
                   coalesce(sum(case when add_time >= #{startUnix} and add_time < #{endUnix} then 1 else 0 end), 0) as newProductNum
            from eb_store_product
            where is_del = 0
              and is_recycle = 0
            """)
    Map<String, Object> selectProductBaseData(
            @Param("startUnix") long startUnix,
            @Param("endUnix") long endUnix);

    @Select("""
            select coalesce(sum(i.pay_num), 0) as payProductNum,
                   coalesce(sum(i.price * i.pay_num), 0) as payAmount,
                   coalesce(count(distinct o.uid), 0) as payUserNum,
                   coalesce(count(distinct o.id), 0) as payOrderNum
            from eb_store_order_info i
            inner join eb_store_order o on o.order_id = i.order_id
            where o.paid = 1
              and o.refund_status = 0
              and o.is_del = 0
              and o.is_system_del = 0
              and o.pay_time >= #{startTime}
              and o.pay_time < #{endTime}
            """)
    Map<String, Object> selectProductPayData(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select p.id as id,
                   p.store_name as storeName,
                   p.image as image,
                   p.price as price,
                   p.sales as sales,
                   p.browse as browse,
                   p.stock as stock,
                   p.is_show as isShow,
                   coalesce(sum(case when o.id is not null then i.pay_num else 0 end), 0) as payNum,
                   coalesce(sum(case when o.id is not null then i.price * i.pay_num else 0 end), 0) as payPrice
            from eb_store_product p
            left join eb_store_order_info i on i.product_id = p.id
            left join eb_store_order o on o.order_id = i.order_id
              and o.paid = 1
              and o.refund_status = 0
              and o.is_del = 0
              and o.is_system_del = 0
              and o.pay_time >= #{startTime}
              and o.pay_time < #{endTime}
            where p.is_del = 0
              and p.is_recycle = 0
            group by p.id, p.store_name, p.image, p.price, p.sales, p.browse, p.stock, p.is_show
            order by
              case when #{sort} = 'payPrice' then coalesce(sum(case when o.id is not null then i.price * i.pay_num else 0 end), 0) end desc,
              case when #{sort} = 'payNum' then coalesce(sum(case when o.id is not null then i.pay_num else 0 end), 0) end desc,
              case when #{sort} = 'browse' then p.browse end desc,
              p.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectProductRanking(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("sort") String sort,
            @Param("limit") int limit);

    @Select("""
            select date_format(o.pay_time, '%Y-%m-%d') as label,
                   coalesce(sum(i.price * i.pay_num), 0) as price,
                   coalesce(sum(i.pay_num), 0) as countValue
            from eb_store_order_info i
            inner join eb_store_order o on o.order_id = i.order_id
            where o.paid = 1
              and o.refund_status = 0
              and o.is_del = 0
              and o.is_system_del = 0
              and o.pay_time >= #{startTime}
              and o.pay_time < #{endTime}
            group by date_format(o.pay_time, '%Y-%m-%d')
            order by label asc
            """)
    List<DashboardChartPoint> selectProductPayTrend(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select date_format(from_unixtime(add_time), '%Y-%m-%d') as label,
                   count(1) as countValue
            from eb_store_product_log
            where type = 'visit'
              and visit_num = 1
              and add_time >= #{startUnix}
              and add_time < #{endUnix}
            group by date_format(from_unixtime(add_time), '%Y-%m-%d')
            order by label asc
            """)
    List<DashboardChartPoint> selectProductVisitTrend(
            @Param("startUnix") long startUnix,
            @Param("endUnix") long endUnix);

    @Select("""
            select count(1) as registerNum,
                   coalesce(sum(case when status = 1 then 1 else 0 end), 0) as enabledUserNum,
                   coalesce(sum(case when is_promoter = 1 then 1 else 0 end), 0) as promoterNum,
                   coalesce(sum(now_money), 0) as userBalance,
                   coalesce(sum(brokerage_price), 0) as brokerageBalance,
                   coalesce(sum(integral), 0) as integralBalance
            from eb_user
            where is_logoff = 0
            """)
    Map<String, Object> selectUserTotalData();

    @Select("""
            select user_type as channel,
                   count(1) as num
            from eb_user
            where is_logoff = 0
            group by user_type
            order by num desc
            """)
    List<Map<String, Object>> selectUserChannelData();

    @Select("""
            select case sex when 1 then '男' when 2 then '女' when 3 then '保密' else '未知' end as name,
                   count(1) as num
            from eb_user
            where is_logoff = 0
            group by sex
            order by num desc
            """)
    List<Map<String, Object>> selectUserSexData();

    @Select("""
            select coalesce(nullif(country, ''), '未知') as name,
                   count(1) as num
            from eb_user
            where is_logoff = 0
            group by coalesce(nullif(country, ''), '未知')
            order by num desc
            """)
    List<Map<String, Object>> selectUserAreaData();

    @Select("""
            select count(1) as registerNum,
                   coalesce((select count(distinct v.uid)
                             from eb_user_visit_record v
                             where v.uid > 0
                               and v.date >= #{startDate}
                               and v.date < #{endDate}), 0) as activeUserNum,
                   coalesce((select count(distinct r.uid)
                             from eb_user_recharge r
                             where r.paid = 1
                               and r.pay_time >= #{startTime}
                               and r.pay_time < #{endTime}), 0) as rechargeUserNum,
                   coalesce((select count(1)
                             from eb_user_visit_record v
                             where v.date >= #{startDate}
                               and v.date < #{endDate}), 0) as pageviews,
                   coalesce((select count(distinct o.uid)
                             from eb_store_order o
                             where o.create_time >= #{startTime}
                               and o.create_time < #{endTime}
                               and o.is_del = 0
                               and o.is_system_del = 0), 0) as orderUserNum,
                   coalesce((select count(distinct o.uid)
                             from eb_store_order o
                             where o.paid = 1
                               and o.pay_time >= #{startTime}
                               and o.pay_time < #{endTime}
                               and o.is_del = 0
                               and o.is_system_del = 0), 0) as orderPayUserNum,
                   coalesce((select sum(o.pay_price)
                             from eb_store_order o
                             where o.paid = 1
                               and o.pay_time >= #{startTime}
                               and o.pay_time < #{endTime}
                               and o.is_del = 0
                               and o.is_system_del = 0), 0) as payOrderAmount
            from eb_user u
            where u.is_logoff = 0
              and u.create_time >= #{startTime}
              and u.create_time < #{endTime}
            """)
    Map<String, Object> selectUserOverviewRange(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select date_format(pay_time, '%Y-%m-%d') as label,
                   count(id) as countValue,
                   coalesce(sum(pay_price), 0) as price
            from eb_store_order
            where paid = 1
              and pay_time >= #{startTime}
              and pay_time < #{endTime}
              and is_del = 0
              and is_system_del = 0
            group by date_format(pay_time, '%Y-%m-%d')
            order by label asc
            """)
    List<DashboardChartPoint> selectPaidTradeTrend(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            select count(1) as orderNum,
                   coalesce(sum(case when paid = 1 then 1 else 0 end), 0) as payOrderNum,
                   coalesce(sum(case when paid = 0 then 1 else 0 end), 0) as waitPayOrderNum,
                   coalesce(sum(case when refund_status in (1, 3) then 1 else 0 end), 0) as refundingOrderNum,
                   coalesce(sum(case when refund_status = 2 then 1 else 0 end), 0) as refundedOrderNum,
                   coalesce(sum(case when paid = 1 then pay_price else 0 end), 0) as payOrderAmount,
                   coalesce(sum(case when refund_status = 2 then refund_price else 0 end), 0) as refundAmount,
                   count(distinct uid) as orderUserNum,
                   count(distinct case when paid = 1 then uid end) as payUserNum
            from eb_store_order
            where create_time >= #{startTime}
              and create_time < #{endTime}
              and is_del = 0
              and is_system_del = 0
            """)
    Map<String, Object> selectTradeOverview(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
}
