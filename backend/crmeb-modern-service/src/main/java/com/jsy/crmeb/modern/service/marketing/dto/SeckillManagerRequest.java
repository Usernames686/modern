package com.jsy.crmeb.modern.service.marketing.dto;

public class SeckillManagerRequest {
    private String name;
    private String time;
    private String img;
    private String silderImgs;
    private Integer sort;
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getSilderImgs() {
        return silderImgs;
    }

    public void setSilderImgs(String silderImgs) {
        this.silderImgs = silderImgs;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
