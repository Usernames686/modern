package com.jsy.crmeb.modern.service.product.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeResponse {
    private Integer id;
    private Integer pid;
    private String path;
    private String name;
    private Integer type;
    private String url;
    private String extra;
    private Boolean status;
    private Integer sort;
    private List<CategoryTreeResponse> child = new ArrayList<>();

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<CategoryTreeResponse> getChild() {
        return child;
    }

    public void setChild(List<CategoryTreeResponse> child) {
        this.child = child;
    }
}
