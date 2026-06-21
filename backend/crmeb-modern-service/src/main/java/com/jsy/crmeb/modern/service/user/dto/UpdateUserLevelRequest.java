package com.jsy.crmeb.modern.service.user.dto;

public class UpdateUserLevelRequest {
    private Integer uid;
    private Integer levelId;
    private Boolean isSub;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public Integer getLevelId() { return levelId; }
    public void setLevelId(Integer levelId) { this.levelId = levelId; }
    public Boolean getIsSub() { return isSub; }
    public void setIsSub(Boolean isSub) { this.isSub = isSub; }
}
