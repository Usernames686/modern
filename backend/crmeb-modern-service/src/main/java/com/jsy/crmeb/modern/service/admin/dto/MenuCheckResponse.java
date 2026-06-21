package com.jsy.crmeb.modern.service.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class MenuCheckResponse {
    private Integer id;
    private Integer pid;
    private String name;
    private String icon;
    private Boolean checked = false;
    private Boolean disabled = false;
    private Integer sort;
    private List<MenuCheckResponse> childList = new ArrayList<>();

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

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<MenuCheckResponse> getChildList() {
        return childList;
    }

    public void setChildList(List<MenuCheckResponse> childList) {
        this.childList = childList;
    }
}
