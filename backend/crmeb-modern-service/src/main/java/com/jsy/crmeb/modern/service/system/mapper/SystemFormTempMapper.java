package com.jsy.crmeb.modern.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.system.entity.SystemFormTemp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemFormTempMapper extends BaseMapper<SystemFormTemp> {
    @Select("""
            <script>
            select id,
                   name,
                   info,
                   content,
                   create_time as createTime,
                   update_time as updateTime
            from eb_system_form_temp
            <where>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or name like concat('%', #{keywords}, '%')
                  or info like concat('%', #{keywords}, '%')
                )
              </if>
            </where>
            order by id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SystemFormTemp> selectPage(
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_system_form_temp
            <where>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or name like concat('%', #{keywords}, '%')
                  or info like concat('%', #{keywords}, '%')
                )
              </if>
            </where>
            </script>
            """)
    long countAll(@Param("keywords") String keywords);
}
