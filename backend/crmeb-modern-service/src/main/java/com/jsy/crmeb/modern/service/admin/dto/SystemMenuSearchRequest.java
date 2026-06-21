package com.jsy.crmeb.modern.service.admin.dto;

public class SystemMenuSearchRequest {
    private String name;
    private String menuType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }
}
