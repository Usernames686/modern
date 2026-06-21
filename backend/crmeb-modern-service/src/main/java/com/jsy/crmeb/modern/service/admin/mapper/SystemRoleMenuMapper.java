package com.jsy.crmeb.modern.service.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.admin.entity.SystemRoleMenu;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemRoleMenuMapper extends BaseMapper<SystemRoleMenu> {
    @Delete("delete from eb_system_role_menu where rid = #{rid}")
    int deleteByRid(@Param("rid") Integer rid);

    @Select("select menu_id from eb_system_role_menu where rid = #{rid}")
    List<Integer> selectMenuIdsByRid(@Param("rid") Integer rid);
}
