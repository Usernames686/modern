package com.jsy.crmeb.modern.service.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.admin.entity.SystemMenu;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemMenuMapper extends BaseMapper<SystemMenu> {
    @Select("""
            SELECT m.*
            FROM eb_system_menu m
            RIGHT JOIN eb_system_role_menu rm ON rm.menu_id = m.id
            RIGHT JOIN eb_system_role r ON rm.rid = r.id
            RIGHT JOIN eb_system_admin a ON FIND_IN_SET(r.id, a.roles)
            WHERE m.is_delte = 0
              AND r.status = 1
              AND m.menu_type != 'A'
              AND m.is_show = 1
              AND a.id = #{userId}
            GROUP BY m.id
            """)
    List<SystemMenu> selectMenusByUserId(@Param("userId") Integer userId);
}
