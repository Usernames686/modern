package com.jsy.crmeb.modern.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.system.entity.SystemGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemGroupMapper extends BaseMapper<SystemGroup> {
    @Select("""
            <script>
            select id,
                   name,
                   info,
                   form_id as formId,
                   create_time as createTime,
                   update_time as updateTime
            from eb_system_group
            <where>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or name like concat('%', #{keywords}, '%')
                  or info like concat('%', #{keywords}, '%')
                  or cast(form_id as char) = #{keywords}
                )
              </if>
            </where>
            order by id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SystemGroup> selectPage(
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_system_group
            <where>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or name like concat('%', #{keywords}, '%')
                  or info like concat('%', #{keywords}, '%')
                  or cast(form_id as char) = #{keywords}
                )
              </if>
            </where>
            </script>
            """)
    long countAll(@Param("keywords") String keywords);
}
