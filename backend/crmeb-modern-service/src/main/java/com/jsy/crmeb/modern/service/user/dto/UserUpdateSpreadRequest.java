package com.jsy.crmeb.modern.service.user.dto;

public class UserUpdateSpreadRequest {
    private Integer userId;
    private String image;
    private Integer spreadUid;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getSpreadUid() { return spreadUid; }
    public void setSpreadUid(Integer spreadUid) { this.spreadUid = spreadUid; }
}
