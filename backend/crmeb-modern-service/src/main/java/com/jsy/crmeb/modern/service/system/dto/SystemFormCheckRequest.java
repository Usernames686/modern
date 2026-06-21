package com.jsy.crmeb.modern.service.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SystemFormCheckRequest {
    @Min(value = 0, message = "请选择表单")
    private Integer id;
    private Integer sort;
    private Boolean status;
    @Valid
    @NotEmpty(message = "fields 至少要有一组数据")
    private List<SystemFormItemCheckRequest> fields;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public List<SystemFormItemCheckRequest> getFields() {
        return fields;
    }

    public void setFields(List<SystemFormItemCheckRequest> fields) {
        this.fields = fields;
    }
}
