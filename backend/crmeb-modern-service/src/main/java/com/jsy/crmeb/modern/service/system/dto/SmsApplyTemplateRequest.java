package com.jsy.crmeb.modern.service.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SmsApplyTemplateRequest {
    @NotBlank(message = "模板标题不能为空")
    private String title;

    @NotBlank(message = "模板内容不能为空")
    private String content;

    @NotNull(message = "模板类型不能为空")
    @Min(value = 1, message = "未知的模板类型")
    @Max(value = 3, message = "未知的模板类型")
    private Integer type;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
