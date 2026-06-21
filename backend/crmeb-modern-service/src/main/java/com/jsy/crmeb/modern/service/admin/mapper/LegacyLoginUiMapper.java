package com.jsy.crmeb.modern.service.admin.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LegacyLoginUiMapper {
    @Select("select value from eb_system_config where name = #{name} limit 1")
    String selectConfigValue(@Param("name") String name);

    @Select("select value from eb_system_group_data where gid = #{gid} and status = 1 order by sort desc, id asc")
    List<String> selectGroupDataValues(@Param("gid") Integer gid);
}
