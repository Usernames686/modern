package com.jsy.crmeb.modern.service.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateFreeResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRegionResponse;
import com.jsy.crmeb.modern.service.logistics.dto.SystemCityTreeResponse;
import com.jsy.crmeb.modern.service.logistics.entity.ShippingTemplate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShippingTemplateMapper extends BaseMapper<ShippingTemplate> {
    @Select("""
            <script>
            select id, name, type, appoint, sort, create_time as createTime, update_time as updateTime
            from eb_shipping_templates
            <where>
              <if test='keywords != null and keywords != ""'>name like concat('%', #{keywords}, '%')</if>
            </where>
            order by sort desc, id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<ShippingTemplate> selectPage(
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1) from eb_shipping_templates
            <where>
              <if test='keywords != null and keywords != ""'>name like concat('%', #{keywords}, '%')</if>
            </where>
            </script>
            """)
    long countAll(@Param("keywords") String keywords);

    @Select("""
            <script>
            select exists(
              select 1 from eb_shipping_templates
              where name = #{name}
              <if test='excludeId != null'>and id != #{excludeId}</if>
            )
            </script>
            """)
    boolean existsByName(@Param("name") String name, @Param("excludeId") Integer excludeId);

    @Insert("""
            insert into eb_shipping_templates(name, type, appoint, sort, create_time, update_time)
            values(#{name}, #{type}, #{appoint}, #{sort}, #{createTime}, #{updateTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTemplate(ShippingTemplate template);

    @Update("""
            update eb_shipping_templates
            set name = #{name}, type = #{type}, appoint = #{appoint}, sort = #{sort}, update_time = #{updateTime}
            where id = #{id}
            """)
    int updateTemplate(ShippingTemplate template);

    @Delete("delete from eb_shipping_templates where id = #{id}")
    int deleteTemplate(@Param("id") Integer id);

    @Delete("""
            delete r, f
            from eb_shipping_templates_region r
            left join eb_shipping_templates_free f on f.temp_id = r.temp_id
            where r.temp_id = #{tempId}
            """)
    int deleteChildrenByTempIdJoined(@Param("tempId") Integer tempId);

    @Delete("delete from eb_shipping_templates_region where temp_id = #{tempId}")
    int deleteRegionsByTempId(@Param("tempId") Integer tempId);

    @Delete("delete from eb_shipping_templates_free where temp_id = #{tempId}")
    int deleteFreeByTempId(@Param("tempId") Integer tempId);

    default void deleteChildrenByTempId(Integer tempId) {
        deleteRegionsByTempId(tempId);
        deleteFreeByTempId(tempId);
    }

    @Select("select count(1) from eb_store_product where temp_id = #{tempId} and is_del = 0")
    int isTemplateUsed(@Param("tempId") Integer tempId);

    @Insert("""
            <script>
            insert into eb_shipping_templates_region
              (temp_id, city_id, title, first, first_price, renewal, renewal_price, type, uniqid, status)
            values
            <foreach collection='rows' item='row' separator=','>
              (#{row.tempId}, #{row.cityId}, #{row.title}, #{row.first}, #{row.firstPrice}, #{row.renewal}, #{row.renewalPrice}, #{row.type}, #{row.uniqid}, #{row.status})
            </foreach>
            </script>
            """)
    int insertRegionBatch(@Param("rows") List<Map<String, Object>> rows);

    @Insert("""
            <script>
            insert into eb_shipping_templates_free
              (temp_id, city_id, title, number, price, type, uniqid, status)
            values
            <foreach collection='rows' item='row' separator=','>
              (#{row.tempId}, #{row.cityId}, #{row.title}, #{row.number}, #{row.price}, #{row.type}, #{row.uniqid}, #{row.status})
            </foreach>
            </script>
            """)
    int insertFreeBatch(@Param("rows") List<Map<String, Object>> rows);

    @Select("""
            select concat('[', group_concat(title order by city_id separator ','), ']') as title,
                   min(first) as first,
                   min(first_price) as firstPrice,
                   min(renewal) as renewal,
                   min(renewal_price) as renewalPrice,
                   uniqid
            from eb_shipping_templates_region
            where temp_id = #{tempId}
            group by uniqid
            order by min(id)
            """)
    List<ShippingTemplateRegionResponse> selectRegionGroup(@Param("tempId") Integer tempId);

    @Select("""
            select concat('[', group_concat(title order by city_id separator ','), ']') as title,
                   min(number) as number,
                   min(price) as price,
                   uniqid
            from eb_shipping_templates_free
            where temp_id = #{tempId}
            group by uniqid
            order by min(id)
            """)
    List<ShippingTemplateFreeResponse> selectFreeGroup(@Param("tempId") Integer tempId);

    @Select("""
            select id,
                   city_id as cityId,
                   level,
                   parent_id as parentId,
                   area_code as areaCode,
                   name,
                   merger_name as mergerName,
                   lng,
                   lat,
                   is_show as isShow
            from eb_system_city
            where is_show = 1
            order by parent_id asc, id asc
            """)
    List<SystemCityTreeResponse> selectCityTree();

    @Select("""
            select id,
                   city_id as cityId,
                   level,
                   parent_id as parentId,
                   area_code as areaCode,
                   name,
                   merger_name as mergerName,
                   lng,
                   lat,
                   is_show as isShow
            from eb_system_city
            where parent_id = #{parentId}
              and is_show = 1
            order by id asc
            """)
    List<SystemCityTreeResponse> selectCityList(@Param("parentId") Integer parentId);

    @Select("""
            select id,
                   city_id as cityId,
                   level,
                   parent_id as parentId,
                   area_code as areaCode,
                   name,
                   merger_name as mergerName,
                   lng,
                   lat,
                   is_show as isShow
            from eb_system_city
            where id = #{id}
            """)
    SystemCityTreeResponse selectCityById(@Param("id") Integer id);

    @Update("""
            update eb_system_city
            set name = #{name},
                update_time = now()
            where id = #{id}
            """)
    int updateCityName(@Param("id") Integer id, @Param("name") String name);

    @Select("""
            <script>
            select id,
                   name,
                   code,
                   sort,
                   account,
                   net_name as netName,
                   partner_id as partnerId,
                   partner_key as partnerKey,
                   net,
                   is_show as isShow,
                   status
            from eb_express
            <where>
              <if test='keywords != null and keywords != ""'>
                and (name like concat('%', #{keywords}, '%') or code like concat('%', #{keywords}, '%'))
              </if>
              <if test='isShow != null'>is_show = #{isShow}</if>
            </where>
            order by sort desc, id asc
            limit #{offset}, #{limit}
            </script>
            """)
    List<Map<String, Object>> selectExpressPage(
            @Param("keywords") String keywords,
            @Param("isShow") Integer isShow,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_express
            <where>
              <if test='keywords != null and keywords != ""'>
                and (name like concat('%', #{keywords}, '%') or code like concat('%', #{keywords}, '%'))
              </if>
              <if test='isShow != null'>is_show = #{isShow}</if>
            </where>
            </script>
            """)
    long countExpress(@Param("keywords") String keywords, @Param("isShow") Integer isShow);

    @Select("""
            select id,
                   name,
                   code,
                   sort,
                   account,
                   password,
                   net_name as netName,
                   partner_id as partnerId,
                   partner_key as partnerKey,
                   net,
                   is_show as isShow,
                   status
            from eb_express
            where id = #{id}
            """)
    Map<String, Object> selectExpressById(@Param("id") Integer id);

    @Update("""
            update eb_express
            set account = #{account},
                password = #{password},
                net_name = #{netName},
                sort = #{sort},
                status = #{status}
            where id = #{id}
            """)
    int updateExpress(
            @Param("id") Integer id,
            @Param("account") String account,
            @Param("password") String password,
            @Param("netName") String netName,
            @Param("sort") Integer sort,
            @Param("status") Boolean status);

    @Update("update eb_express set is_show = #{isShow} where id = #{id}")
    int updateExpressShow(@Param("id") Integer id, @Param("isShow") Boolean isShow);

    @Select("select count(1) from eb_express")
    long countExpressAll();
}
