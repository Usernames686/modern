package com.jsy.crmeb.modern.service.front.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FrontCommerceMapper {
    @Select("""
            select id
            from eb_store_cart
            where uid = #{uid}
              and product_id = #{productId}
              and product_attr_unique = #{uniqueId}
              and type = #{type}
              and is_new = 0
              and status = 1
            limit 1
            """)
    Integer selectActiveCartId(
            @Param("uid") Integer uid,
            @Param("productId") Integer productId,
            @Param("uniqueId") String uniqueId,
            @Param("type") String type);

    @Insert("""
            insert into eb_store_cart(uid, type, product_id, product_attr_unique, cart_num, is_new, status, create_time, update_time)
            values(#{uid}, #{type}, #{productId}, #{uniqueId}, #{cartNum}, 0, 1, now(), now())
            """)
    int insertCart(
            @Param("uid") Integer uid,
            @Param("type") String type,
            @Param("productId") Integer productId,
            @Param("uniqueId") String uniqueId,
            @Param("cartNum") Integer cartNum);

    @Update("update eb_store_cart set cart_num = cart_num + #{cartNum}, update_time = now() where id = #{id} and uid = #{uid}")
    int incrementCartNum(@Param("id") Integer id, @Param("uid") Integer uid, @Param("cartNum") Integer cartNum);

    @Update("update eb_store_cart set cart_num = #{number}, update_time = now() where id = #{id} and uid = #{uid} and status = 1")
    int updateCartNum(@Param("id") Integer id, @Param("uid") Integer uid, @Param("number") Integer number);

    @Update("""
            update eb_store_cart
            set product_attr_unique = #{uniqueId},
                cart_num = #{number},
                status = 1,
                update_time = now()
            where id = #{id}
              and uid = #{uid}
              and product_id = #{productId}
              and is_new = 0
            """)
    int resetCart(
            @Param("uid") Integer uid,
            @Param("id") Integer id,
            @Param("productId") Integer productId,
            @Param("uniqueId") String uniqueId,
            @Param("number") Integer number);

    @Select("""
            <script>
            select id,
                   name,
                   introduction,
                   phone,
                   address,
                   detailed_address as detailedAddress,
                   image,
                   latitude,
                   longitude,
                   valid_time as validTime,
                   day_time as dayTime,
                   is_show as isShow,
                   is_del as isDel,
                   create_time as createTime
            from eb_system_store
            where is_del = 0
              and is_show = 1
            order by id asc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectStoreList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(1) from eb_system_store where is_del = 0 and is_show = 1")
    long countStoreList();

    @Select("""
            select id,
                   name,
                   introduction,
                   phone,
                   address,
                   detailed_address as detailedAddress,
                   image,
                   latitude,
                   longitude,
                   valid_time as validTime,
                   day_time as dayTime,
                   is_show as isShow,
                   is_del as isDel,
                   create_time as createTime
            from eb_system_store
            where id = #{id}
              and is_del = 0
              and is_show = 1
            limit 1
            """)
    Map<String, Object> selectStoreById(@Param("id") Integer id);

    @Delete("""
            <script>
            delete from eb_store_cart
            where uid = #{uid}
              and id in
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    int deleteCart(@Param("uid") Integer uid, @Param("ids") List<Integer> ids);

    @Select("""
            select id,
                   product_id as productId,
                   suk,
                   stock,
                   price,
                   image
            from eb_store_product_attr_value
            where product_id = #{productId}
              and type = 0
              and is_del = 0
            order by id asc
            limit 1
            """)
    Map<String, Object> selectFirstSku(@Param("productId") Integer productId);

    @Select("""
            select id,
                   product_id as productId,
                   suk,
                   stock,
                   price,
                   image
            from eb_store_product_attr_value
            where product_id = #{productId}
              and id = #{attrValueId}
              and type = 0
              and is_del = 0
            limit 1
            """)
    Map<String, Object> selectSku(@Param("productId") Integer productId, @Param("attrValueId") Integer attrValueId);

    @Select("""
            select p.id as productId,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.cost,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.give_integral as giveIntegral,
                   p.is_sub as isSub,
                   p.temp_id as tempId,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   av.id as attrValueId,
                   av.id as productAttrUnique,
                   av.suk,
                   av.stock as skuStock,
                   av.price as skuPrice,
                   av.image as skuImage,
                   av.weight,
                   av.volume
            from eb_store_product p
            left join eb_store_product_attr_value av
              on av.product_id = p.id
             and av.id = #{attrValueId}
             and av.type = 0
             and av.is_del = 0
            where p.id = #{productId}
              and av.id is not null
            limit 1
            """)
    Map<String, Object> selectBuyNowProductRow(
            @Param("productId") Integer productId,
            @Param("attrValueId") Integer attrValueId);

    @Select("""
            <script>
            select c.id,
                   c.uid,
                   c.type,
                   c.product_id as productId,
                   c.product_attr_unique as productAttrUnique,
                   c.cart_num as cartNum,
                   c.is_new as isNew,
                   c.status,
                   c.create_time as createTime,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.cost,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.give_integral as giveIntegral,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   av.id as attrValueId,
                   av.suk,
                   av.stock as skuStock,
                   av.price as skuPrice,
                   av.image as skuImage
                   ,av.weight
                   ,av.volume
            from eb_store_cart c
            left join eb_store_product p on p.id = c.product_id
            left join eb_store_product_attr_value av on av.id = c.product_attr_unique and av.type = 0 and av.is_del = 0
            where c.uid = #{uid}
              and c.is_new = 0
              <if test='isValid'>
                and c.status = 1
                and p.is_show = 1
                and p.is_del = 0
                and p.is_recycle = 0
              </if>
              <if test='!isValid'>
                and (c.status = 0 or p.is_show = 0 or p.is_del = 1 or p.is_recycle = 1)
              </if>
            order by c.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectCartList(
            @Param("uid") Integer uid,
            @Param("isValid") boolean isValid,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select c.id,
                   c.uid,
                   c.type,
                   c.product_id as productId,
                   c.product_attr_unique as productAttrUnique,
                   c.cart_num as cartNum,
                   c.is_new as isNew,
                   c.status,
                   c.create_time as createTime,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.cost,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.give_integral as giveIntegral,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   av.id as attrValueId,
                   av.suk,
                   av.stock as skuStock,
                   av.price as skuPrice,
                   av.image as skuImage,
                   av.weight,
                   av.volume
            from eb_store_cart c
            left join eb_store_product p on p.id = c.product_id
            left join eb_store_product_attr_value av on av.id = c.product_attr_unique and av.type = 0 and av.is_del = 0
            where c.uid = #{uid}
              and c.id = #{id}
              and c.is_new = 0
            limit 1
            """)
    Map<String, Object> selectCartRowById(@Param("uid") Integer uid, @Param("id") Integer id);

    @Select("""
            <script>
            select c.id,
                   c.uid,
                   c.type,
                   c.product_id as productId,
                   c.product_attr_unique as productAttrUnique,
                   c.cart_num as cartNum,
                   c.is_new as isNew,
                   c.status,
                   c.create_time as createTime,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.cost,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.give_integral as giveIntegral,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   av.id as attrValueId,
                   av.suk,
                   av.stock as skuStock,
                   av.price as skuPrice,
                   av.image as skuImage,
                   av.weight,
                   av.volume
            from eb_store_cart c
            left join eb_store_product p on p.id = c.product_id
            left join eb_store_product_attr_value av on av.id = c.product_attr_unique and av.type = 0 and av.is_del = 0
            where c.uid = #{uid}
              and c.status = 1
              and c.is_new = 0
              and p.is_show = 1
              and p.is_del = 0
              and p.is_recycle = 0
              and c.id in
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            order by c.id asc
            </script>
            """)
    List<Map<String, Object>> selectCartRowsByIds(@Param("uid") Integer uid, @Param("ids") List<Integer> ids);

    @Select("""
            <script>
            select s.id as seckillId,
                   s.product_id as productId,
                   s.title as storeName,
                   s.image,
                   s.price,
                   s.cost,
                   s.stock as activityStock,
                   s.quota as activityQuota,
                   s.num as activityLimit,
                   s.status as activityStatus,
                   s.is_show as activityIsShow,
                   s.is_del as activityIsDel,
                   s.start_time as startTime,
                   s.stop_time as stopTime,
                   s.give_integral as giveIntegral,
                   s.weight,
                   s.volume,
                   sm.start_time as managerStartHour,
                   sm.end_time as managerEndHour,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   sav.id as attrValueId,
                   sav.suk,
                   sav.stock as skuStock,
                   sav.quota as skuQuota,
                   sav.price as activitySkuPrice,
                   sav.image as skuImage,
                   sav.weight as skuWeight,
                   sav.volume as skuVolume,
                   nav.id as normalAttrValueId,
                   nav.stock as normalSkuStock
            from eb_store_seckill s
            inner join eb_store_product p on p.id = s.product_id
            inner join eb_store_seckill_manger sm on sm.id = s.time_id
                and sm.is_del = 0
                and replace(sm.status, '''', '') = '1'
            inner join eb_store_product_attr_value sav on sav.product_id = s.id
                and sav.type = 1
                and sav.is_del = 0
                <if test='attrValueId != null and attrValueId &gt; 0'>
                and sav.id = #{attrValueId}
                </if>
            left join eb_store_product_attr_value nav on nav.product_id = s.product_id
                and nav.type = 0
                and nav.is_del = 0
                and nav.suk = sav.suk
            where s.id = #{seckillId}
              <if test='productId != null and productId &gt; 0'>
              and s.product_id = #{productId}
              </if>
            order by sav.id asc
            limit 1
            </script>
            """)
    Map<String, Object> selectSeckillOrderRow(
            @Param("seckillId") Integer seckillId,
            @Param("attrValueId") Integer attrValueId,
            @Param("productId") Integer productId);

    @Select("""
            <script>
            select b.id as bargainId,
                   b.product_id as productId,
                   b.title as storeName,
                   b.image,
                   b.price as bargainPrice,
                   b.min_price as bargainMinPrice,
                   b.cost,
                   b.stock as bargainStock,
                   b.quota as bargainQuota,
                   b.num as activityLimit,
                   b.status as bargainStatus,
                   b.is_del as bargainIsDel,
                   b.start_time as startTime,
                   b.stop_time as stopTime,
                   b.give_integral as giveIntegral,
                   b.weight,
                   b.volume,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   bav.id as attrValueId,
                   bav.suk,
                   bav.stock as skuStock,
                   bav.quota as skuQuota,
                   bav.price as activitySkuPrice,
                   bav.image as skuImage,
                   bav.weight as skuWeight,
                   bav.volume as skuVolume,
                   nav.id as normalAttrValueId,
                   nav.stock as normalSkuStock
            from eb_store_bargain b
            inner join eb_store_product p on p.id = b.product_id
            inner join eb_store_product_attr_value bav on bav.product_id = b.id
                and bav.type = 2
                and bav.is_del = 0
                <if test='attrValueId != null and attrValueId &gt; 0'>
                and bav.id = #{attrValueId}
                </if>
            left join eb_store_product_attr_value nav on nav.product_id = b.product_id
                and nav.type = 0
                and nav.is_del = 0
                and nav.suk = bav.suk
            where b.id = #{bargainId}
              <if test='productId != null and productId &gt; 0'>
              and b.product_id = #{productId}
              </if>
            order by bav.id asc
            limit 1
            </script>
            """)
    Map<String, Object> selectBargainOrderRow(
            @Param("bargainId") Integer bargainId,
            @Param("attrValueId") Integer attrValueId,
            @Param("productId") Integer productId);

    @Select("""
            select id,
                   uid,
                   bargain_id as bargainId,
                   bargain_price_min as bargainPriceMin,
                   bargain_price as bargainPrice,
                   price,
                   status,
                   is_del as isDel
            from eb_store_bargain_user
            where id = #{bargainUserId}
            limit 1
            """)
    Map<String, Object> selectBargainUserForOrder(@Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select order_id as orderNo,
                   paid,
                   is_del as isDel,
                   is_system_del as isSystemDel
            from eb_store_order
            where bargain_id = #{bargainId}
              and bargain_user_id = #{bargainUserId}
            order by id desc
            limit 1
            """)
    Map<String, Object> selectBargainOrder(
            @Param("bargainId") Integer bargainId,
            @Param("bargainUserId") Integer bargainUserId);

    @Select("""
            select count(1) as totalCount,
                   coalesce(sum(case when paid = 0 then 1 else 0 end), 0) as unpaidCount
            from eb_store_order
            where uid = #{uid}
              and bargain_id = #{bargainId}
              and is_del = 0
              and is_system_del = 0
            """)
    Map<String, Object> countUserCurrentBargainOrders(
            @Param("uid") Integer uid,
            @Param("bargainId") Integer bargainId);

    @Select("""
            <script>
            select c.id as combinationId,
                   c.product_id as productId,
                   c.title as storeName,
                   c.image,
                   c.price,
                   c.cost,
                   c.stock as activityStock,
                   c.quota as activityQuota,
                   c.num as activityLimit,
                   c.is_show as activityIsShow,
                   c.is_del as activityIsDel,
                   c.start_time as startTime,
                   c.stop_time as stopTime,
                   c.once_num as onceNum,
                   c.people,
                   0 as giveIntegral,
                   c.weight,
                   c.volume,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   cav.id as attrValueId,
                   cav.suk,
                   cav.stock as skuStock,
                   cav.quota as skuQuota,
                   cav.price as activitySkuPrice,
                   cav.image as skuImage,
                   cav.weight as skuWeight,
                   cav.volume as skuVolume,
                   nav.id as normalAttrValueId,
                   nav.stock as normalSkuStock
            from eb_store_combination c
            inner join eb_store_product p on p.id = c.product_id
            inner join eb_store_product_attr_value cav on cav.product_id = c.id
                and cav.type = 3
                and cav.is_del = 0
                <if test='attrValueId != null and attrValueId &gt; 0'>
                and cav.id = #{attrValueId}
                </if>
            left join eb_store_product_attr_value nav on nav.product_id = c.product_id
                and nav.type = 0
                and nav.is_del = 0
                and nav.suk = cav.suk
            where c.id = #{combinationId}
              <if test='productId != null and productId &gt; 0'>
              and c.product_id = #{productId}
              </if>
            order by cav.id asc
            limit 1
            </script>
            """)
    Map<String, Object> selectCombinationOrderRow(
            @Param("combinationId") Integer combinationId,
            @Param("attrValueId") Integer attrValueId,
            @Param("productId") Integer productId);

    @Select("""
            select count(1) as totalCount,
                   coalesce(sum(case when paid = 0 then 1 else 0 end), 0) as unpaidCount
            from eb_store_order
            where uid = #{uid}
              and seckill_id = #{seckillId}
              and create_time >= curdate()
              and create_time < date_add(curdate(), interval 1 day)
              and is_del = 0
              and is_system_del = 0
            """)
    Map<String, Object> countUserCurrentDaySeckillOrders(
            @Param("uid") Integer uid,
            @Param("seckillId") Integer seckillId);

    @Select("""
            select count(1) as totalCount,
                   coalesce(sum(total_num), 0) as totalNum,
                   coalesce(sum(case when paid = 0 then 1 else 0 end), 0) as unpaidCount
            from eb_store_order
            where uid = #{uid}
              and combination_id = #{combinationId}
              and is_del = 0
              and is_system_del = 0
            """)
    Map<String, Object> countUserCurrentCombinationOrders(
            @Param("uid") Integer uid,
            @Param("combinationId") Integer combinationId);

    @Select("""
            select id,
                   uid,
                   cid,
                   k_id as kId,
                   people,
                   status,
                   stop_time as stopTime,
                   is_refund as isRefund
            from eb_store_pink
            where id = #{pinkId}
            limit 1
            """)
    Map<String, Object> selectPinkForJoin(@Param("pinkId") Integer pinkId);

    @Select("""
            <script>
            select count(1)
            from eb_store_cart c
            left join eb_store_product p on p.id = c.product_id
            where c.uid = #{uid}
              and c.is_new = 0
              <if test='isValid'>
                and c.status = 1
                and p.is_show = 1
                and p.is_del = 0
                and p.is_recycle = 0
              </if>
              <if test='!isValid'>
                and (c.status = 0 or p.is_show = 0 or p.is_del = 1 or p.is_recycle = 1)
              </if>
            </script>
            """)
    long countCart(@Param("uid") Integer uid, @Param("isValid") boolean isValid);

    @Select("select coalesce(sum(cart_num), 0) from eb_store_cart where uid = #{uid} and status = 1 and is_new = 0")
    Integer countCartNum(@Param("uid") Integer uid);

    @Select("""
            <script>
            select id,
                   order_id as orderId,
                   uid,
                   real_name as realName,
                   user_phone as userPhone,
                   user_address as userAddress,
                   total_num as totalNum,
                   total_price as totalPrice,
                   pay_price as payPrice,
                   paid,
                   pay_type as payType,
                   pay_time as payTime,
                   create_time as createTime,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   refund_reason_wap as refundReasonWap,
                   refund_reason_wap_explain as refundReasonWapExplain,
                   refund_reason_wap_img as refundReasonWapImg,
                   refund_reason_time as refundReasonTime,
                   refund_reason as refundReason,
                   refund_price as refundPrice,
                   delivery_name as deliveryName,
                   delivery_type as deliveryType,
                   delivery_id as deliveryId,
                   delivery_code as deliveryCode,
                   verify_code as verifyCode,
                   store_id as storeId,
                   cast(shipping_type as signed) as shippingType,
                   pay_postage as payPostage,
                   coupon_id as couponId,
                   coupon_price as couponPrice,
                   deduction_price as deductionPrice,
                   use_integral as useIntegral,
                   pro_total_price as proTotalPrice,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   pink_id as pinkId,
                   type,
                   is_del as isDel
            from eb_store_order
            where uid = #{uid}
              and is_del = 0
              and is_system_del = 0
              <if test='keywords != null and keywords != ""'>
                and order_id like concat('%', #{keywords}, '%')
              </if>
              <choose>
                <when test='type == 0'>and paid = 0 and refund_status = 0</when>
                <when test='type == 1'>and paid = 1 and status = 0 and refund_status = 0</when>
                <when test='type == 2'>and paid = 1 and status = 1 and refund_status = 0</when>
                <when test='type == 3'>and paid = 1 and status = 2 and refund_status = 0</when>
                <when test='type == 4'>and paid = 1 and status = 3 and refund_status = 0</when>
                <when test='type == -3 and refundStatus == 4'>
                  and refund_status = 0
                  and refund_reason is not null
                  and refund_reason != ''
                </when>
                <when test='type == -3 and refundStatus != null'>
                  and refund_status = #{refundStatus}
                </when>
                <when test='type == -3'>
                  and (refund_status &gt; 0 or (refund_reason is not null and refund_reason != ''))
                </when>
              </choose>
            order by id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectOrders(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("type") Integer type,
            @Param("refundStatus") Integer refundStatus,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_store_order
            where uid = #{uid}
              and is_del = 0
              and is_system_del = 0
              <if test='keywords != null and keywords != ""'>
                and order_id like concat('%', #{keywords}, '%')
              </if>
              <choose>
                <when test='type == 0'>and paid = 0 and refund_status = 0</when>
                <when test='type == 1'>and paid = 1 and status = 0 and refund_status = 0</when>
                <when test='type == 2'>and paid = 1 and status = 1 and refund_status = 0</when>
                <when test='type == 3'>and paid = 1 and status = 2 and refund_status = 0</when>
                <when test='type == 4'>and paid = 1 and status = 3 and refund_status = 0</when>
                <when test='type == -3 and refundStatus == 4'>
                  and refund_status = 0
                  and refund_reason is not null
                  and refund_reason != ''
                </when>
                <when test='type == -3 and refundStatus != null'>
                  and refund_status = #{refundStatus}
                </when>
                <when test='type == -3'>
                  and (refund_status &gt; 0 or (refund_reason is not null and refund_reason != ''))
                </when>
              </choose>
            </script>
            """)
    long countOrders(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("type") Integer type,
            @Param("refundStatus") Integer refundStatus);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   real_name as realName,
                   user_phone as userPhone,
                   user_address as userAddress,
                   total_num as totalNum,
                   total_price as totalPrice,
                   pay_price as payPrice,
                   paid,
                   pay_type as payType,
                   pay_time as payTime,
                   create_time as createTime,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   refund_reason_wap as refundReasonWap,
                   refund_reason_wap_explain as refundReasonWapExplain,
                   refund_reason_wap_img as refundReasonWapImg,
                   refund_reason_time as refundReasonTime,
                   refund_reason as refundReason,
                   refund_price as refundPrice,
                   delivery_name as deliveryName,
                   delivery_type as deliveryType,
                   delivery_id as deliveryId,
                   delivery_code as deliveryCode,
                   verify_code as verifyCode,
                   store_id as storeId,
                   cast(shipping_type as signed) as shippingType,
                   pay_postage as payPostage,
                   coupon_id as couponId,
                   coupon_price as couponPrice,
                   deduction_price as deductionPrice,
                   use_integral as useIntegral,
                   pro_total_price as proTotalPrice,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   pink_id as pinkId,
                   type,
                   is_del as isDel
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderId}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderDetail(@Param("uid") Integer uid, @Param("orderId") String orderId);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   pay_price as payPrice,
                   use_integral as useIntegral,
                   paid,
                   pay_type as payType,
                   pay_time as payTime,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   create_time as createTime,
                   pink_id as pinkId,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   is_del as isDel,
                   is_system_del as isSystemDel
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderNo}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectPayOrder(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Update("""
            update eb_store_order
            set paid = 1,
                pay_type = #{payType},
                pay_time = now(),
                status = 0,
                update_time = now()
            where uid = #{uid}
              and order_id = #{orderNo}
              and paid = 0
              and is_del = 0
              and is_system_del = 0
            """)
    int markOrderPaid(@Param("uid") Integer uid, @Param("orderNo") String orderNo, @Param("payType") String payType);

    @Update("""
            update eb_store_order
            set pay_type = #{payType},
                is_channel = #{isChannel},
                update_time = now()
            where uid = #{uid}
              and order_id = #{orderNo}
              and paid = 0
              and is_del = 0
              and is_system_del = 0
            """)
    int updateOrderPayIntent(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("payType") String payType,
            @Param("isChannel") Integer isChannel);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   real_name as realName,
                   user_phone as userPhone,
                   user_address as userAddress,
                   total_num as totalNum,
                   total_price as totalPrice,
                   pay_price as payPrice,
                   paid,
                   pay_type as payType,
                   create_time as createTime,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   refund_reason_wap as refundReasonWap,
                   refund_reason_wap_explain as refundReasonWapExplain,
                   refund_reason_wap_img as refundReasonWapImg,
                   refund_reason_time as refundReasonTime,
                   refund_reason as refundReason,
                   refund_price as refundPrice,
                   delivery_name as deliveryName,
                   delivery_type as deliveryType,
                   delivery_id as deliveryId,
                   delivery_code as deliveryCode,
                   verify_code as verifyCode,
                   store_id as storeId,
                   cast(shipping_type as signed) as shippingType,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   type,
                   is_del as isDel
            from eb_store_order
            where uid = #{uid}
              and id = #{id}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderById(@Param("uid") Integer uid, @Param("id") Integer id);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   user_phone as userPhone,
                   paid,
                   pay_time as payTime,
                   create_time as createTime,
                   update_time as updateTime,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   delivery_name as deliveryName,
                   delivery_type as deliveryType,
                   delivery_id as deliveryId,
                   delivery_code as deliveryCode
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderId}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderExpress(@Param("uid") Integer uid, @Param("orderId") String orderId);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   pay_price as payPrice,
                   paid,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   refund_reason_wap as refundReasonWap,
                   refund_reason_wap_explain as refundReasonWapExplain,
                   refund_reason_wap_img as refundReasonWapImg,
                   refund_reason_time as refundReasonTime,
                   refund_reason as refundReason,
                   refund_price as refundPrice,
                   is_del as isDel,
                   is_system_del as isSystemDel
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderNo}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderByOrderNo(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   paid,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   seckill_id as seckillId,
                   bargain_id as bargainId,
                   combination_id as combinationId,
                   type
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderNo}
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderForAgain(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Select("select count(1) from eb_store_order_info where order_id = #{orderDbId}")
    int countOrderInfoRows(@Param("orderDbId") Integer orderDbId);

    @Select("""
            select oi.id,
                   oi.product_id as productId,
                   oi.attr_value_id as productAttrUnique,
                   oi.attr_value_id as attrValueId,
                   oi.pay_num as cartNum,
                   p.store_name as storeName,
                   p.image,
                   p.price,
                   p.ot_price as otPrice,
                   p.cost,
                   p.cate_id as cateId,
                   p.vip_price as vipPrice,
                   p.give_integral as giveIntegral,
                   p.is_sub as isSub,
                   p.stock as productStock,
                   p.is_show as isShow,
                   p.is_del as isDel,
                   p.is_recycle as isRecycle,
                   av.suk,
                   av.stock as skuStock,
                   av.price as skuPrice,
                   av.image as skuImage,
                   av.weight,
                   av.volume
            from eb_store_order_info oi
            inner join eb_store_order o on o.id = oi.order_id
            inner join eb_store_product p on p.id = oi.product_id
            inner join eb_store_product_attr_value av on av.id = oi.attr_value_id and av.product_id = oi.product_id and av.type = 0 and av.is_del = 0
            where o.uid = #{uid}
              and oi.order_id = #{orderDbId}
              and p.is_show = 1
              and p.is_del = 0
              and p.is_recycle = 0
            order by oi.id asc
            """)
    List<Map<String, Object>> selectAgainOrderRows(@Param("uid") Integer uid, @Param("orderDbId") Integer orderDbId);

    @Select("""
            select id,
                   order_id as orderId,
                   uid,
                   pay_price as payPrice,
                   paid,
                   cast(status as signed) as status,
                   refund_status as refundStatus,
                   is_del as isDel,
                   is_system_del as isSystemDel
            from eb_store_order
            where uid = #{uid}
              and order_id = #{orderId}
              and paid = 1
              and is_del = 0
              and is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectPaidOrderForRefund(@Param("uid") Integer uid, @Param("orderId") String orderId);

    @Update("""
            update eb_store_order
            set refund_status = 1,
                refund_reason_time = now(),
                refund_reason_wap = #{reason},
                refund_reason_wap_explain = #{explain},
                refund_reason_wap_img = #{reasonImage},
                refund_price = 0,
                update_time = now()
            where uid = #{uid}
              and order_id = #{orderId}
              and paid = 1
              and is_del = 0
              and is_system_del = 0
              and refund_status = 0
            """)
    int applyRefund(
            @Param("uid") Integer uid,
            @Param("orderId") String orderId,
            @Param("reason") String reason,
            @Param("explain") String explain,
            @Param("reasonImage") String reasonImage);

    @Insert("""
            insert into eb_store_order_status(oid, change_type, change_message, create_time)
            values(#{orderDbId}, #{changeType}, #{message}, now())
            """)
    int insertOrderStatus(
            @Param("orderDbId") Integer orderDbId,
            @Param("changeType") String changeType,
            @Param("message") String message);

    @Select("select value from eb_system_config where name = #{key} limit 1")
    String selectConfigValue(@Param("key") String key);

    @Update("""
            update eb_store_order
            set is_del = 1,
                is_system_del = 1,
                update_time = now()
            where uid = #{uid}
              and id = #{id}
              and is_del = 0
              and is_system_del = 0
            """)
    int cancelOrder(@Param("uid") Integer uid, @Param("id") Integer id);

    @Update("""
            update eb_store_order
            set status = 2,
                update_time = now()
            where uid = #{uid}
              and id = #{id}
              and paid = 1
              and status = 1
              and refund_status = 0
              and is_del = 0
              and is_system_del = 0
            """)
    int takeOrder(@Param("uid") Integer uid, @Param("id") Integer id);

    @Update("""
            update eb_store_order
            set is_del = 1,
                update_time = now()
            where uid = #{uid}
              and id = #{id}
              and is_del = 0
              and is_system_del = 0
            """)
    int deleteUserOrder(@Param("uid") Integer uid, @Param("id") Integer id);

    @Select("""
            <script>
            select oi.id,
                   oi.order_id as orderId,
                   oi.product_id as productId,
                   oi.product_name as productName,
                   coalesce(nullif(p.image, ''), nullif(av.image, ''), nullif(oi.image, '')) as image,
                   oi.image as orderImage,
                   p.image as productImage,
                   av.image as skuImage,
                   oi.sku,
                   oi.price,
                   oi.pay_num as payNum,
                   oi.attr_value_id as attrValueId,
                   oi.is_reply as isReply,
                   oi.`unique`
            from eb_store_order_info oi
            left join eb_store_product p on p.id = oi.product_id
            left join eb_store_product_attr_value av on av.id = oi.attr_value_id
                                                   and av.product_id = oi.product_id
                                                   and av.type = 0
                                                   and av.is_del = 0
            where oi.order_id in
              <foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            order by oi.id asc
            </script>
            """)
    List<Map<String, Object>> selectOrderInfos(@Param("orderIds") List<Integer> orderIds);

    @Select("""
            select o.id,
                   o.order_id as orderId,
                   o.uid,
                   o.paid,
                   cast(o.status as signed) as status,
                   o.refund_status as refundStatus
            from eb_store_order o
            where o.uid = #{uid}
              and o.order_id = #{orderNo}
              and o.is_del = 0
              and o.is_system_del = 0
            limit 1
            """)
    Map<String, Object> selectOrderForReply(@Param("uid") Integer uid, @Param("orderNo") String orderNo);

    @Select("""
            select oi.id,
                   oi.order_id as orderDbId,
                   oi.order_no as orderId,
                   oi.product_id as productId,
                   oi.product_name as productName,
                   oi.image,
                   oi.sku,
                   oi.price,
                   oi.pay_num as payNum,
                   oi.attr_value_id as attrValueId,
                   oi.is_reply as isReply,
                   oi.`unique`
            from eb_store_order_info oi
            inner join eb_store_order o on o.id = oi.order_id
            where o.uid = #{uid}
              and oi.order_id = #{orderDbId}
              and o.is_del = 0
              and o.is_system_del = 0
              and (#{unique} is null or #{unique} = '' or oi.`unique` = #{unique} or cast(oi.attr_value_id as char) = #{unique})
            order by oi.id asc
            limit 1
            """)
    Map<String, Object> selectOrderInfoForReplyByOrderId(
            @Param("uid") Integer uid,
            @Param("orderDbId") Integer orderDbId,
            @Param("unique") String unique);

    @Select("""
            select oi.id,
                   oi.order_id as orderDbId,
                   oi.order_no as orderId,
                   oi.product_id as productId,
                   oi.product_name as productName,
                   oi.image,
                   oi.sku,
                   oi.price,
                   oi.pay_num as payNum,
                   oi.attr_value_id as attrValueId,
                   oi.is_reply as isReply,
                   oi.`unique`
            from eb_store_order_info oi
            inner join eb_store_order o on o.id = oi.order_id
            where o.uid = #{uid}
              and o.order_id = #{orderNo}
              and o.is_del = 0
              and o.is_system_del = 0
              and (#{unique} is null or #{unique} = '' or oi.`unique` = #{unique} or cast(oi.attr_value_id as char) = #{unique})
            order by oi.id asc
            limit 1
            """)
    Map<String, Object> selectOrderInfoForReplyByOrderNo(
            @Param("uid") Integer uid,
            @Param("orderNo") String orderNo,
            @Param("unique") String unique);

    @Select("""
            select count(1)
            from eb_store_product_reply
            where oid = #{orderDbId}
              and product_id = #{productId}
              and `unique` = #{unique}
              and is_del = 0
            """)
    int countProductReply(
            @Param("orderDbId") Integer orderDbId,
            @Param("productId") Integer productId,
            @Param("unique") String unique);

    @Insert("""
            insert into eb_store_product_reply(
                uid, oid, `unique`, product_id, reply_type,
                product_score, service_score, comment, pics,
                nickname, avatar, sku, is_del, is_reply,
                create_time, update_time
            ) values (
                #{uid}, #{oid}, #{unique}, #{productId}, 'product',
                #{productScore}, #{serviceScore}, #{comment}, #{pics},
                #{nickname}, #{avatar}, #{sku}, 0, 0,
                now(), now()
            )
            """)
    int insertProductReply(Map<String, Object> reply);

    @Update("""
            update eb_store_order_info
            set is_reply = 1,
                update_time = now()
            where order_id = #{orderDbId}
              and product_id = #{productId}
              and (`unique` = #{unique} or cast(attr_value_id as char) = #{unique})
            """)
    int updateOrderInfoReply(
            @Param("orderDbId") Integer orderDbId,
            @Param("productId") Integer productId,
            @Param("unique") String unique);

    @Select("select count(1) from eb_store_order_info where order_id = #{orderDbId} and is_reply = 0")
    int countUnrepliedOrderInfo(@Param("orderDbId") Integer orderDbId);

    @Update("""
            update eb_store_order
            set status = 3,
                update_time = now()
            where uid = #{uid}
              and id = #{orderDbId}
              and paid = 1
              and status = 2
              and refund_status = 0
              and is_del = 0
              and is_system_del = 0
            """)
    int completeOrderAfterReply(@Param("uid") Integer uid, @Param("orderDbId") Integer orderDbId);

    @Select("select count(1) from eb_store_order where uid = #{uid} and is_del = 0 and is_system_del = 0 and paid = 0 and refund_status = 0")
    Integer countUnpaid(@Param("uid") Integer uid);

    @Select("select count(1) from eb_store_order where uid = #{uid} and is_del = 0 and is_system_del = 0 and paid = 1 and status = 0 and refund_status = 0")
    Integer countUnShipped(@Param("uid") Integer uid);

    @Select("select count(1) from eb_store_order where uid = #{uid} and is_del = 0 and is_system_del = 0 and paid = 1 and status = 1 and refund_status = 0")
    Integer countReceived(@Param("uid") Integer uid);

    @Select("select count(1) from eb_store_order where uid = #{uid} and is_del = 0 and is_system_del = 0 and paid = 1 and status = 2 and refund_status = 0")
    Integer countEvaluated(@Param("uid") Integer uid);

    @Select("select count(1) from eb_store_order where uid = #{uid} and is_del = 0 and is_system_del = 0 and refund_status > 0")
    Integer countRefund(@Param("uid") Integer uid);

    @Select("""
            select id,
                   uid,
                   real_name as realName,
                   phone,
                   province,
                   city,
                   district,
                   detail,
                   post_code as postCode,
                   longitude,
                   latitude,
                   is_default as isDefault,
                   is_del as isDel,
                   create_time as createTime
            from eb_user_address
            where uid = #{uid}
              and is_del = 0
            order by is_default desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectAddressList(@Param("uid") Integer uid, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(1) from eb_user_address where uid = #{uid} and is_del = 0")
    long countAddress(@Param("uid") Integer uid);

    @Select("""
            select id,
                   uid,
                   real_name as realName,
                   phone,
                   province,
                   city,
                   district,
                   detail,
                   post_code as postCode,
                   longitude,
                   latitude,
                   is_default as isDefault,
                   is_del as isDel,
                   create_time as createTime
            from eb_user_address
            where uid = #{uid}
              and id = #{id}
              and is_del = 0
            limit 1
            """)
    Map<String, Object> selectAddressById(@Param("uid") Integer uid, @Param("id") Integer id);

    @Select("""
            select id,
                   uid,
                   real_name as realName,
                   phone,
                   province,
                   city,
                   district,
                   detail,
                   post_code as postCode,
                   longitude,
                   latitude,
                   is_default as isDefault,
                   is_del as isDel,
                   create_time as createTime
            from eb_user_address
            where uid = #{uid}
              and is_del = 0
            order by is_default desc, id desc
            limit 1
            """)
    Map<String, Object> selectDefaultAddress(@Param("uid") Integer uid);

    @Update("update eb_user_address set is_default = 0 where uid = #{uid}")
    int clearDefaultAddress(@Param("uid") Integer uid);

    @Insert("""
            insert into eb_user_address(uid, real_name, phone, province, city, city_id, district, detail, post_code, longitude, latitude, is_default, is_del)
            values(#{uid}, #{realName}, #{phone}, #{province}, #{city}, 0, #{district}, #{detail}, 0, '0', '0', #{isDefault}, 0)
            """)
    int insertAddress(Map<String, Object> address);

    @Update("""
            update eb_user_address
            set real_name = #{realName},
                phone = #{phone},
                province = #{province},
                city = #{city},
                district = #{district},
                detail = #{detail},
                is_default = #{isDefault},
                update_time = now()
            where uid = #{uid}
              and id = #{id}
              and is_del = 0
            """)
    int updateAddress(Map<String, Object> address);

    @Update("update eb_user_address set is_del = 1, update_time = now() where uid = #{uid} and id = #{id}")
    int deleteAddress(@Param("uid") Integer uid, @Param("id") Integer id);

    @Update("update eb_user_address set is_default = 1, update_time = now() where uid = #{uid} and id = #{id} and is_del = 0")
    int setDefaultAddress(@Param("uid") Integer uid, @Param("id") Integer id);

    @Select("select id from eb_user_address where uid = #{uid} and is_del = 0 order by id desc limit 1")
    Integer selectLatestAddressId(@Param("uid") Integer uid);

    @Select("""
            <script>
            select id,
                   coupon_id as couponId,
                   uid,
                   name,
                   money,
                   min_price as minPrice,
                   type,
                   cast(status as signed) as status,
                   start_time as startTime,
                   end_time as endTime,
                   date_format(start_time, '%Y.%m.%d') as useStartTimeStr,
                   date_format(end_time, '%Y.%m.%d') as useEndTimeStr,
                   cast(use_type as signed) as useType,
                   primary_key as primaryKey
            from eb_store_coupon_user
            where uid = #{uid}
              and status = 0
              and min_price &lt;= #{maxPrice}
              and start_time &lt; now()
              and end_time &gt; now()
              and (
                use_type = 1
                <if test='productIds != null and productIds.size() &gt; 0'>
                or (use_type = 2 and
                  <foreach collection='productIds' item='productId' open='(' separator=' or ' close=')'>
                    find_in_set(#{productId}, primary_key)
                  </foreach>
                )
                </if>
                <if test='categoryIds != null and categoryIds.size() &gt; 0'>
                or (use_type = 3 and
                  <foreach collection='categoryIds' item='categoryId' open='(' separator=' or ' close=')'>
                    find_in_set(#{categoryId}, primary_key)
                  </foreach>
                )
                </if>
              )
            order by id desc
            </script>
            """)
    List<Map<String, Object>> selectCouponsForOrder(
            @Param("uid") Integer uid,
            @Param("maxPrice") Object maxPrice,
            @Param("productIds") List<Integer> productIds,
            @Param("categoryIds") List<Integer> categoryIds);

    @Select("""
            select id,
                   coupon_id as couponId,
                   uid,
                   name,
                   money,
                   min_price as minPrice,
                   cast(status as signed) as status,
                   start_time as startTime,
                   end_time as endTime,
                   cast(use_type as signed) as useType,
                   primary_key as primaryKey
            from eb_store_coupon_user
            where uid = #{uid}
              and id = #{id}
              and start_time < now()
              and end_time > now()
            limit 1
            """)
    Map<String, Object> selectCouponUser(@Param("uid") Integer uid, @Param("id") Integer id);

    @Update("""
            update eb_store_coupon_user
            set status = 1,
                use_time = now(),
                update_time = now()
            where uid = #{uid}
              and id = #{id}
              and status = 0
            """)
    int useCoupon(@Param("uid") Integer uid, @Param("id") Integer id);

    @Insert("""
            insert into eb_store_order(
                order_id, uid, real_name, user_phone, user_address,
                freight_price, total_num, total_price, total_postage,
                pay_price, pay_postage, deduction_price, coupon_id, coupon_price,
                paid, pay_type, status, refund_status, mark, is_del,
                mer_id, is_mer_check, combination_id, pink_id, cost,
                seckill_id, bargain_id, verify_code, store_id, shipping_type,
                clerk_id, is_channel, is_remind, is_system_del, bargain_user_id,
                type, pro_total_price, before_pay_price, is_alter_price
            ) values (
                #{orderId}, #{uid}, #{realName}, #{userPhone}, #{userAddress},
                0.00, #{totalNum}, #{totalPrice}, 0.00,
                #{payPrice}, 0.00, #{deductionPrice}, #{couponId}, #{couponPrice},
                0, '', 0, 0, #{mark}, 0,
                0, 0, #{combinationId}, #{pinkId}, #{cost},
                #{seckillId}, #{bargainId}, #{verifyCode}, #{storeId}, #{shippingType},
                0, 0, 0, 0, #{bargainUserId},
                0, #{proTotalPrice}, #{payPrice}, 0
            )
            """)
    int insertOrder(Map<String, Object> order);

    @Select("select id from eb_store_order where uid = #{uid} and order_id = #{orderId} limit 1")
    Integer selectOrderDbId(@Param("uid") Integer uid, @Param("orderId") String orderId);

    @Insert("""
            <script>
            insert into eb_store_order_info(
                order_id, product_id, info, `unique`, order_no,
                product_name, attr_value_id, image, sku, price,
                pay_num, weight, volume, give_integral, is_reply,
                is_sub, vip_price, product_type
            ) values
            <foreach collection='rows' item='row' separator=','>
            (
                #{row.orderDbId}, #{row.productId}, #{row.info}, #{row.unique}, #{row.orderNo},
                #{row.productName}, #{row.attrValueId}, #{row.image}, #{row.sku}, #{row.price},
                #{row.payNum}, #{row.weight}, #{row.volume}, #{row.giveIntegral}, 0,
                #{row.isSub}, #{row.vipPrice}, #{row.productType}
            )
            </foreach>
            </script>
            """)
    int insertOrderInfos(@Param("rows") List<Map<String, Object>> rows);

    @Update("""
            update eb_store_seckill
            set stock = stock - #{num},
                sales = sales + #{num},
                quota = quota - #{num}
            where id = #{seckillId}
              and stock >= #{num}
              and quota >= #{num}
              and is_del = 0
              and status = 1
            """)
    int decreaseSeckillStock(@Param("seckillId") Integer seckillId, @Param("num") Integer num);

    @Update("""
            update eb_store_bargain
            set stock = stock - #{num},
                sales = sales + #{num},
                quota = quota - #{num}
            where id = #{bargainId}
              and stock >= #{num}
              and quota >= #{num}
              and is_del = 0
              and status = 1
            """)
    int decreaseBargainStock(@Param("bargainId") Integer bargainId, @Param("num") Integer num);

    @Update("""
            update eb_store_combination
            set stock = stock - #{num},
                sales = sales + #{num},
                quota = quota - #{num}
            where id = #{combinationId}
              and stock >= #{num}
              and quota >= #{num}
              and is_del = 0
              and is_show = 1
            """)
    int decreaseCombinationStock(@Param("combinationId") Integer combinationId, @Param("num") Integer num);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock - #{num},
                sales = sales + #{num},
                quota = quota - #{num},
                version = version + 1
            where product_id = #{activityId}
              and id = #{attrValueId}
              and type = #{type}
              and stock >= #{num}
              and quota >= #{num}
              and is_del = 0
            """)
    int decreaseActivityAttrStock(
            @Param("activityId") Integer activityId,
            @Param("attrValueId") Integer attrValueId,
            @Param("type") Integer type,
            @Param("num") Integer num);

    @Update("""
            update eb_store_product
            set stock = stock - #{num},
                sales = sales + #{num},
                version = version + 1
            where id = #{productId}
              and stock >= #{num}
              and is_del = 0
              and is_show = 1
              and is_recycle = 0
            """)
    int decreaseProductStock(@Param("productId") Integer productId, @Param("num") Integer num);

    @Update("""
            update eb_store_product_attr_value
            set stock = stock - #{num},
                sales = sales + #{num},
                version = version + 1
            where id = #{attrValueId}
              and type = 0
              and stock >= #{num}
              and is_del = 0
            """)
    int decreaseNormalAttrStock(@Param("attrValueId") Integer attrValueId, @Param("num") Integer num);
}
