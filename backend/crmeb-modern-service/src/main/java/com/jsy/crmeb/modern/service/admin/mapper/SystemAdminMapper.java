package com.jsy.crmeb.modern.service.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemAdminMapper extends BaseMapper<SystemAdmin> {
    @Select("select id, role_name as roleName from eb_system_role")
    List<RoleRow> selectRoleRows();

    class RoleRow {
        private Integer id;
        private String roleName;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }
}
