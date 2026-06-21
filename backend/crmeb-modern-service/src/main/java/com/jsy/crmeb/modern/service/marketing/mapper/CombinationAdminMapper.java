package com.jsy.crmeb.modern.service.marketing.mapper;

import com.jsy.crmeb.modern.service.marketing.dto.CombinationProductRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CombinationAdminMapper {
    @Select("""
            <script>
            select count(1)
            from eb_store_combination c
            where c.is_del = 0
            <if test="isShow != null">
              and c.is_show = #{isShow}
            </if>
            <if test="keywords != null and keywords != ''">
              and (c.product_id like concat('%', #{keywords}, '%')
                or c.id like concat('%', #{keywords}, '%')
                or c.title like concat('%', #{keywords}, '%'))
            </if>
            </script>
            """)
    long countProducts(@Param("isShow") Integer isShow, @Param("keywords") String keywords);

    @Select("""
            <script>
            select
              c.id,
              c.product_id as productId,
              c.mer_id as merId,
              c.image,
              c.images,
              c.title,
              c.attr,
              c.people,
              c.info,
              c.price,
              c.sort,
              c.sales,
              c.stock,
              c.add_time as addTime,
              c.is_host as isHost,
              c.is_show as isShow,
              c.is_del as isDel,
              c.combination,
              c.mer_use as merUse,
              c.is_postage as isPostage,
              c.postage,
              c.start_time as startTime,
              c.stop_time as stopTime,
              c.effective_time as effectiveTime,
              c.cost,
              c.browse,
              c.unit_name as unitName,
              c.temp_id as tempId,
              c.weight,
              c.volume,
              c.num,
              c.quota,
              c.quota_show as quotaShow,
              c.ot_price as otPrice,
              c.once_num as onceNum,
              c.virtual_ration as virtualRation,
              coalesce(pink_count.countPeople, 0) as countPeople,
              coalesce(pink_count.countPeopleAll, 0) as countPeopleAll,
              coalesce(pink_count.countPeoplePink, 0) as countPeoplePink
            from eb_store_combination c
            left join (
              select cid,
                     sum(case when k_id = 0 then 1 else 0 end) as countPeople,
                     count(1) as countPeopleAll,
                     sum(case when k_id = 0 and status = 2 then 1 else 0 end) as countPeoplePink
              from eb_store_pink
              group by cid
            ) pink_count on pink_count.cid = c.id
            where c.is_del = 0
            <if test="isShow != null">
              and c.is_show = #{isShow}
            </if>
            <if test="keywords != null and keywords != ''">
              and (c.product_id like concat('%', #{keywords}, '%')
                or c.id like concat('%', #{keywords}, '%')
                or c.title like concat('%', #{keywords}, '%'))
            </if>
            order by c.sort desc, c.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectProducts(
            @Param("isShow") Integer isShow,
            @Param("keywords") String keywords,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              c.id,
              c.product_id as productId,
              c.mer_id as merId,
              c.image,
              c.images,
              c.title,
              c.attr,
              c.people,
              c.info,
              c.price,
              c.sort,
              c.sales,
              c.stock,
              c.add_time as addTime,
              c.is_host as isHost,
              c.is_show as isShow,
              c.is_del as isDel,
              c.combination,
              c.mer_use as merUse,
              c.is_postage as isPostage,
              c.postage,
              c.start_time as startTime,
              c.stop_time as stopTime,
              c.effective_time as effectiveTime,
              c.cost,
              c.browse,
              c.unit_name as unitName,
              c.temp_id as tempId,
              c.weight,
              c.volume,
              c.num,
              c.quota,
              c.quota_show as quotaShow,
              c.ot_price as otPrice,
              c.once_num as onceNum,
              c.virtual_ration as virtualRation
            from eb_store_combination c
            where c.id = #{id}
            limit 1
            """)
    Map<String, Object> selectProductById(@Param("id") Integer id);

    @Update("update eb_store_combination set is_show = #{isShow} where id = #{id} and is_del = 0")
    int updateProductShow(@Param("id") Integer id, @Param("isShow") int isShow);

    @Update("update eb_store_combination set is_del = 1 where id = #{id} and is_del = 0")
    int softDeleteProduct(@Param("id") Integer id);

    @Insert("""
            insert into eb_store_combination(
              product_id, mer_id, image, images, title, attr, people, info, price, sort,
              sales, stock, add_time, is_host, is_show, is_del, combination, mer_use,
              is_postage, postage, start_time, stop_time, effective_time, cost, browse,
              unit_name, temp_id, weight, volume, num, quota, quota_show, ot_price,
              once_num, virtual_ration
            )
            values(
              #{productId}, 0, #{image}, #{images}, #{title}, null, #{people}, #{info}, #{price}, #{sort},
              0, #{stock}, #{addTime}, 0, #{isShow}, 0, 1, null,
              #{isPostage}, #{postage}, #{startTime}, #{stopTime}, #{effectiveTime}, #{cost}, 0,
              #{unitName}, #{tempId}, #{weight}, #{volume}, #{num}, #{quota}, #{quotaShow}, #{otPrice},
              #{onceNum}, #{virtualRation}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProduct(CombinationProductRequest request);

    @Update("""
            update eb_store_combination
            set image = #{request.image},
                images = #{request.images},
                title = #{request.title},
                people = #{request.people},
                info = #{request.info},
                price = #{request.price},
                sort = #{request.sort},
                stock = #{request.stock},
                is_show = #{request.isShow},
                is_postage = #{request.isPostage},
                postage = #{request.postage},
                start_time = #{request.startTime},
                stop_time = #{request.stopTime},
                effective_time = #{request.effectiveTime},
                cost = #{request.cost},
                unit_name = #{request.unitName},
                temp_id = #{request.tempId},
                weight = #{request.weight},
                volume = #{request.volume},
                num = #{request.num},
                quota = #{request.quota},
                quota_show = #{request.quotaShow},
                ot_price = #{request.otPrice},
                once_num = #{request.onceNum},
                virtual_ration = #{request.virtualRation}
            where id = #{id}
              and is_del = 0
            """)
    int updateProduct(@Param("id") Integer id, @Param("request") CombinationProductRequest request);

    @Update("""
            update eb_store_product_attr_value
            set is_del = 1
            where product_id = #{productId}
              and type = 3
            """)
    int softDeleteAttrValues(@Param("productId") Integer productId);

    @Update("""
            update eb_store_product_attr
            set is_del = 1
            where product_id = #{productId}
              and type = 3
            """)
    int softDeleteAttrs(@Param("productId") Integer productId);

    @Insert("""
            insert into eb_store_product_attr(
                product_id, attr_name, attr_values, type, is_del
            )
            values(
                #{productId}, #{attrName}, #{attrValues}, 3, 0
            )
            """)
    int insertAttr(
            @Param("productId") Integer productId,
            @Param("attrName") String attrName,
            @Param("attrValues") String attrValues);

    @Insert("""
            insert into eb_store_product_attr_value(
                product_id, suk, stock, sales, price, image, `unique`, cost, bar_code,
                ot_price, weight, volume, brokerage, brokerage_two, type, quota,
                quota_show, attr_value, is_del, version
            )
            values(
                #{productId}, #{suk}, #{stock}, 0, #{price}, #{image}, #{uniqueValue}, #{cost}, #{barCode},
                #{otPrice}, #{weight}, #{volume}, 0, 0, 3, #{quota},
                #{quotaShow}, #{attrValue}, 0, 0
            )
            """)
    int insertAttrValue(
            @Param("productId") Integer productId,
            @Param("suk") String suk,
            @Param("stock") Integer stock,
            @Param("price") BigDecimal price,
            @Param("image") String image,
            @Param("uniqueValue") String uniqueValue,
            @Param("cost") BigDecimal cost,
            @Param("barCode") String barCode,
            @Param("otPrice") BigDecimal otPrice,
            @Param("weight") BigDecimal weight,
            @Param("volume") BigDecimal volume,
            @Param("quota") Integer quota,
            @Param("quotaShow") Integer quotaShow,
            @Param("attrValue") String attrValue);

    @Select("""
            <script>
            select count(1)
            from eb_store_pink p
            where p.k_id = 0
            <if test="status != null">
              and p.status = #{status}
            </if>
            <if test="startMillis != null and endMillis != null">
              and p.add_time between #{startMillis} and #{endMillis}
            </if>
            </script>
            """)
    long countPinkHeads(
            @Param("status") Integer status,
            @Param("startMillis") Long startMillis,
            @Param("endMillis") Long endMillis);

    @Select("""
            <script>
            select
              p.id,
              p.uid,
              p.order_id as orderId,
              p.order_id_key as orderIdKey,
              p.total_num as totalNum,
              p.total_price as totalPrice,
              p.cid,
              p.pid,
              p.people,
              p.price,
              p.add_time as addTime,
              p.stop_time as stopTime,
              p.k_id as kId,
              p.is_tpl as isTpl,
              p.is_refund as isRefund,
              p.status,
              p.is_virtual as isVirtual,
              p.nickname,
              p.avatar,
              c.title,
              coalesce(member_count.countPeople, 0) as countPeople
            from eb_store_pink p
            left join eb_store_combination c on c.id = p.cid
            left join (
              select head.id as headId, count(member.id) as countPeople
              from eb_store_pink head
              left join eb_store_pink member on member.id = head.id or member.k_id = head.id
              where head.k_id = 0
              group by head.id
            ) member_count on member_count.headId = p.id
            where p.k_id = 0
            <if test="status != null">
              and p.status = #{status}
            </if>
            <if test="startMillis != null and endMillis != null">
              and p.add_time between #{startMillis} and #{endMillis}
            </if>
            order by p.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectPinkHeads(
            @Param("status") Integer status,
            @Param("startMillis") Long startMillis,
            @Param("endMillis") Long endMillis,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              p.id,
              p.uid,
              p.order_id as orderId,
              p.order_id_key as orderIdKey,
              p.total_num as totalNum,
              p.total_price as totalPrice,
              p.cid,
              p.pid,
              p.people,
              p.price,
              p.add_time as addTime,
              p.stop_time as stopTime,
              p.k_id as kId,
              p.is_tpl as isTpl,
              p.is_refund as isRefund,
              p.status,
              p.is_virtual as isVirtual,
              p.nickname,
              p.avatar,
              o.status as orderStatus,
              o.refund_status as refundStatus
            from eb_store_pink p
            left join eb_store_order o on o.order_id = p.order_id
            where p.id = #{pinkId} or p.k_id = #{pinkId}
            order by p.id desc
            """)
    List<Map<String, Object>> selectPinkOrders(@Param("pinkId") Integer pinkId);

    @Select("select count(1) from eb_store_pink")
    long countAllPink();

    @Select("select count(1) from eb_store_pink where k_id = 0 and status = 2")
    long countSuccessTeams();
}
