package com.jsy.crmeb.modern.service.logistics.dto;

import java.util.List;

public class ShippingTemplateInfoResponse {
    private Integer id;
    private String name;
    private Integer type;
    private Integer appoint;
    private Integer sort;
    private List<ShippingTemplateRegionResponse> regionList;
    private List<ShippingTemplateFreeResponse> freeList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getAppoint() {
        return appoint;
    }

    public void setAppoint(Integer appoint) {
        this.appoint = appoint;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<ShippingTemplateRegionResponse> getRegionList() {
        return regionList;
    }

    public void setRegionList(List<ShippingTemplateRegionResponse> regionList) {
        this.regionList = regionList;
    }

    public List<ShippingTemplateFreeResponse> getFreeList() {
        return freeList;
    }

    public void setFreeList(List<ShippingTemplateFreeResponse> freeList) {
        this.freeList = freeList;
    }
}
