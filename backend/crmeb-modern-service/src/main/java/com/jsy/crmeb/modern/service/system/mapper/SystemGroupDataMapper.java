package com.jsy.crmeb.modern.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.system.entity.SystemGroupData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemGroupDataMapper extends BaseMapper<SystemGroupData> {
    @Select("""
            <script>
            select id,
                   gid,
                   value,
                   sort,
                   status,
                   create_time as createTime,
                   update_time as updateTime
            from eb_system_group_data
            <where>
              gid = #{gid}
              <if test='status != null'>and status = #{status}</if>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or value like concat('%', #{keywords}, '%')
                )
              </if>
            </where>
            order by sort asc, id asc
            limit #{offset}, #{limit}
            </script>
            """)
    List<SystemGroupData> selectPage(
            @Param("gid") Integer gid,
            @Param("status") Boolean status,
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_system_group_data
            <where>
              gid = #{gid}
              <if test='status != null'>and status = #{status}</if>
              <if test='keywords != null and keywords != ""'>
                and (
                  cast(id as char) = #{keywords}
                  or value like concat('%', #{keywords}, '%')
                )
              </if>
            </where>
            </script>
            """)
    long countAll(
            @Param("gid") Integer gid,
            @Param("status") Boolean status,
            @Param("keywords") String keywords);

    @Select("""
            select id,
                   gid,
                   value,
                   sort,
                   status,
                   create_time as createTime,
                   update_time as updateTime
            from eb_system_group_data
            where gid = #{gid}
            order by sort asc, id asc
            """)
    List<SystemGroupData> selectByGid(@Param("gid") Integer gid);
}
