package com.jsy.crmeb.modern.service.system.dto;

import jakarta.validation.constraints.NotBlank;

public class SystemFormItemCheckRequest {
    @NotBlank(message = "请设置字段名称")
    private String name;
    private String value;
    private String title;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
