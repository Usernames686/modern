package com.jsy.crmeb.modern.service.admin.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemRoleInfoResponse {
    private Integer id;
    private String roleName;
    private Boolean status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MenuCheckResponse> menuList = new ArrayList<>();

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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<MenuCheckResponse> getMenuList() {
        return menuList;
    }

    public void setMenuList(List<MenuCheckResponse> menuList) {
        this.menuList = menuList;
    }
}
