package com.jsy.crmeb.modern.service.marketing.mapper;

import com.jsy.crmeb.modern.service.marketing.dto.BargainProductRequest;
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
public interface BargainAdminMapper {
    @Select("""
            <script>
            select count(1)
            from eb_store_bargain b
            where b.is_del = 0
            <if test="status != null">
              and b.status = #{status}
            </if>
            <if test="keywords != null and keywords != ''">
              and (b.id like concat('%', #{keywords}, '%')
                or b.store_name like concat('%', #{keywords}, '%')
                or b.title like concat('%', #{keywords}, '%'))
            </if>
            </script>
            """)
    long countProducts(@Param("status") Integer status, @Param("keywords") String keywords);

    @Select("""
            <script>
            select
              b.id,
              b.product_id as productId,
              b.title,
              b.image,
              b.unit_name as unitName,
              b.stock,
              b.sales,
              b.images,
              b.start_time as startTime,
              b.stop_time as stopTime,
              b.store_name as storeName,
              b.price,
              b.min_price as minPrice,
              b.num,
              b.bargain_max_price as bargainMaxPrice,
              b.bargain_min_price as bargainMinPrice,
              b.bargain_num as bargainNum,
              b.status,
              b.give_integral as giveIntegral,
              b.info,
              b.cost,
              b.sort,
              b.is_hot as isHot,
              b.is_del as isDel,
              b.add_time as addTime,
              b.is_postage as isPostage,
              b.postage,
              b.rule,
              b.look,
              b.share,
              b.temp_id as tempId,
              b.weight,
              b.volume,
              b.quota,
              b.quota_show as quotaShow,
              b.people_num as peopleNum,
              coalesce(user_count.countPeopleAll, 0) as countPeopleAll,
              coalesce(user_count.countPeopleSuccess, 0) as countPeopleSuccess,
              coalesce(help_count.countPeopleHelp, 0) as countPeopleHelp
            from eb_store_bargain b
            left join (
              select bargain_id,
                     count(1) as countPeopleAll,
                     sum(case when status = 3 then 1 else 0 end) as countPeopleSuccess
              from eb_store_bargain_user
              where is_del = 0
              group by bargain_id
            ) user_count on user_count.bargain_id = b.id
            left join (
              select bargain_id, count(1) as countPeopleHelp
              from eb_store_bargain_user_help
              group by bargain_id
            ) help_count on help_count.bargain_id = b.id
            where b.is_del = 0
            <if test="status != null">
              and b.status = #{status}
            </if>
            <if test="keywords != null and keywords != ''">
              and (b.id like concat('%', #{keywords}, '%')
                or b.store_name like concat('%', #{keywords}, '%')
                or b.title like concat('%', #{keywords}, '%'))
            </if>
            order by b.sort desc, b.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectProducts(
            @Param("status") Integer status,
            @Param("keywords") String keywords,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              b.id,
              b.product_id as productId,
              b.title,
              b.image,
              b.unit_name as unitName,
              b.stock,
              b.sales,
              b.images,
              b.start_time as startTime,
              b.stop_time as stopTime,
              b.store_name as storeName,
              b.price,
              b.min_price as minPrice,
              b.num,
              b.bargain_max_price as bargainMaxPrice,
              b.bargain_min_price as bargainMinPrice,
              b.bargain_num as bargainNum,
              b.status,
              b.give_integral as giveIntegral,
              b.info,
              b.cost,
              b.sort,
              b.is_hot as isHot,
              b.is_del as isDel,
              b.add_time as addTime,
              b.is_postage as isPostage,
              b.postage,
              b.rule,
              b.look,
              b.share,
              b.temp_id as tempId,
              b.weight,
              b.volume,
              b.quota,
              b.quota_show as quotaShow,
              b.people_num as peopleNum
            from eb_store_bargain b
            where b.id = #{id}
            limit 1
            """)
    Map<String, Object> selectProductById(@Param("id") Integer id);

    @Update("update eb_store_bargain set status = #{status} where id = #{id} and is_del = 0")
    int updateProductStatus(@Param("id") Integer id, @Param("status") int status);

    @Update("update eb_store_bargain set is_del = 1 where id = #{id} and is_del = 0")
    int softDeleteProduct(@Param("id") Integer id);

    @Insert("""
            insert into eb_store_bargain(
              product_id, title, image, unit_name, stock, sales, images, start_time, stop_time,
              store_name, price, min_price, num, bargain_num, status, give_integral, info, cost,
              sort, is_hot, is_del, add_time, is_postage, postage, rule, look, share, temp_id,
              weight, volume, quota, quota_show, people_num
            )
            values(
              #{productId}, #{title}, #{image}, #{unitName}, #{stock}, 0, #{images}, #{startTime}, #{stopTime},
              #{storeName}, #{price}, #{minPrice}, #{num}, #{bargainNum}, #{status}, #{giveIntegral}, #{info}, #{cost},
              #{sort}, 0, 0, #{addTime}, #{isPostage}, #{postage}, #{rule}, 0, 0, #{tempId},
              #{weight}, #{volume}, #{quota}, #{quotaShow}, #{peopleNum}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProduct(BargainProductRequest request);

    @Update("""
            update eb_store_bargain
            set title = #{request.title},
                image = #{request.image},
                unit_name = #{request.unitName},
                stock = #{request.stock},
                images = #{request.images},
                start_time = #{request.startTime},
                stop_time = #{request.stopTime},
                store_name = #{request.storeName},
                price = #{request.price},
                min_price = #{request.minPrice},
                num = #{request.num},
                bargain_num = #{request.bargainNum},
                status = #{request.status},
                give_integral = #{request.giveIntegral},
                info = #{request.info},
                cost = #{request.cost},
                sort = #{request.sort},
                is_postage = #{request.isPostage},
                postage = #{request.postage},
                rule = #{request.rule},
                temp_id = #{request.tempId},
                weight = #{request.weight},
                volume = #{request.volume},
                quota = #{request.quota},
                quota_show = #{request.quotaShow},
                people_num = #{request.peopleNum}
            where id = #{id}
              and is_del = 0
            """)
    int updateProduct(@Param("id") Integer id, @Param("request") BargainProductRequest request);

    @Update("""
            update eb_store_product_attr_value
            set is_del = 1
            where product_id = #{productId}
              and type = 2
            """)
    int softDeleteAttrValues(@Param("productId") Integer productId);

    @Update("""
            update eb_store_product_attr
            set is_del = 1
            where product_id = #{productId}
              and type = 2
            """)
    int softDeleteAttrs(@Param("productId") Integer productId);

    @Insert("""
            insert into eb_store_product_attr(
                product_id, attr_name, attr_values, type, is_del
            )
            values(
                #{productId}, #{attrName}, #{attrValues}, 2, 0
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
                #{otPrice}, #{weight}, #{volume}, 0, 0, 2, #{quota},
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
            from eb_store_bargain_user bu
            where bu.is_del = 0
            <if test="status != null">
              and bu.status = #{status}
            </if>
            <if test="startMillis != null and endMillis != null">
              and bu.add_time between #{startMillis} and #{endMillis}
            </if>
            </script>
            """)
    long countBargainUsers(
            @Param("status") Integer status,
            @Param("startMillis") Long startMillis,
            @Param("endMillis") Long endMillis);

    @Select("""
            <script>
            select
              bu.id,
              bu.uid,
              bu.bargain_id as bargainId,
              bu.bargain_price_min as bargainPriceMin,
              bu.bargain_price as bargainPrice,
              bu.price,
              bu.status,
              bu.add_time as addTime,
              u.avatar,
              u.nickname,
              b.title,
              b.stop_time as dataTime,
              b.people_num as peopleNum,
              greatest(coalesce(b.people_num, 0) - coalesce(help_count.helpCount, 0), 0) as num,
              (coalesce(bu.bargain_price, 0) - coalesce(bu.price, 0)) as nowPrice
            from eb_store_bargain_user bu
            left join eb_user u on u.uid = bu.uid
            left join eb_store_bargain b on b.id = bu.bargain_id
            left join (
              select bargain_user_id, count(1) as helpCount
              from eb_store_bargain_user_help
              group by bargain_user_id
            ) help_count on help_count.bargain_user_id = bu.id
            where bu.is_del = 0
            <if test="status != null">
              and bu.status = #{status}
            </if>
            <if test="startMillis != null and endMillis != null">
              and bu.add_time between #{startMillis} and #{endMillis}
            </if>
            order by bu.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectBargainUsers(
            @Param("status") Integer status,
            @Param("startMillis") Long startMillis,
            @Param("endMillis") Long endMillis,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            select
              help.id,
              help.uid,
              help.bargain_id as bargainId,
              help.bargain_user_id as bargainUserId,
              help.price,
              help.add_time as addTime,
              u.avatar,
              u.nickname
            from eb_store_bargain_user_help help
            left join eb_user u on u.uid = help.uid
            where help.bargain_user_id = #{bargainUserId}
            order by help.id desc
            """)
    List<Map<String, Object>> selectBargainUserHelp(@Param("bargainUserId") Integer bargainUserId);
}
