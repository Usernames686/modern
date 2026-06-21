package com.jsy.crmeb.modern.service.marketing.dto;

import com.jsy.crmeb.modern.service.marketing.entity.ActivityStyle;
import java.time.LocalDateTime;

public class ActivityStyleResponse {
    private Integer id;
    private String name;
    private Boolean type;
    private LocalDateTime starttime;
    private LocalDateTime endtime;
    private String style;
    private Integer runningStatus;
    private Boolean status;
    private Integer method;
    private String products;
    private LocalDateTime createtime;
    private LocalDateTime updatetime;

    public static ActivityStyleResponse from(ActivityStyle style, LocalDateTime now) {
        ActivityStyleResponse response = new ActivityStyleResponse();
        response.setId(style.getId());
        response.setName(style.getName());
        response.setType(style.getType());
        response.setStarttime(style.getStarttime());
        response.setEndtime(style.getEndtime());
        response.setStyle(style.getStyle());
        response.setStatus(style.getStatus());
        response.setMethod(style.getMethod());
        response.setProducts(style.getProducts());
        response.setCreatetime(style.getCreatetime());
        response.setUpdatetime(style.getUpdatetime());
        response.setRunningStatus(runningStatus(style.getStarttime(), style.getEndtime(), now));
        return response;
    }

    private static Integer runningStatus(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start == null || end == null) {
            return null;
        }
        if (end.isBefore(now)) {
            return -1;
        }
        if (start.isAfter(now)) {
            return 0;
        }
        return 1;
    }

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

    public Boolean getType() {
        return type;
    }

    public void setType(Boolean type) {
        this.type = type;
    }

    public LocalDateTime getStarttime() {
        return starttime;
    }

    public void setStarttime(LocalDateTime starttime) {
        this.starttime = starttime;
    }

    public LocalDateTime getEndtime() {
        return endtime;
    }

    public void setEndtime(LocalDateTime endtime) {
        this.endtime = endtime;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Integer getRunningStatus() {
        return runningStatus;
    }

    public void setRunningStatus(Integer runningStatus) {
        this.runningStatus = runningStatus;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getMethod() {
        return method;
    }

    public void setMethod(Integer method) {
        this.method = method;
    }

    public String getProducts() {
        return products;
    }

    public void setProducts(String products) {
        this.products = products;
    }

    public LocalDateTime getCreatetime() {
        return createtime;
    }

    public void setCreatetime(LocalDateTime createtime) {
        this.createtime = createtime;
    }

    public LocalDateTime getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(LocalDateTime updatetime) {
        this.updatetime = updatetime;
    }
}
