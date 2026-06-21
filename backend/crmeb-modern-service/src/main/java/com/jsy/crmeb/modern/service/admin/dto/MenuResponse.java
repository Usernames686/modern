package com.jsy.crmeb.modern.service.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class MenuResponse {
    private Integer id;
    private Integer pid;
    private String name;
    private String icon;
    private String perms;
    private String component;
    private String menuType;
    private Integer sort;
    private List<MenuResponse> childList = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<MenuResponse> getChildList() {
        return childList;
    }

    public void setChildList(List<MenuResponse> childList) {
        this.childList = childList;
    }
}
