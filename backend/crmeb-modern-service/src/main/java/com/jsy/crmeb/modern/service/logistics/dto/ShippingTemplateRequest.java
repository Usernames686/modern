package com.jsy.crmeb.modern.service.logistics.dto;

import java.util.List;

public class ShippingTemplateRequest {
    private String name;
    private Integer type;
    private Integer appoint;
    private Integer sort;
    private List<ShippingTemplateRegionRequest> shippingTemplatesRegionRequestList;
    private List<ShippingTemplateFreeRequest> shippingTemplatesFreeRequestList;

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

    public List<ShippingTemplateRegionRequest> getShippingTemplatesRegionRequestList() {
        return shippingTemplatesRegionRequestList;
    }

    public void setShippingTemplatesRegionRequestList(List<ShippingTemplateRegionRequest> shippingTemplatesRegionRequestList) {
        this.shippingTemplatesRegionRequestList = shippingTemplatesRegionRequestList;
    }

    public List<ShippingTemplateFreeRequest> getShippingTemplatesFreeRequestList() {
        return shippingTemplatesFreeRequestList;
    }

    public void setShippingTemplatesFreeRequestList(List<ShippingTemplateFreeRequest> shippingTemplatesFreeRequestList) {
        this.shippingTemplatesFreeRequestList = shippingTemplatesFreeRequestList;
    }
}
