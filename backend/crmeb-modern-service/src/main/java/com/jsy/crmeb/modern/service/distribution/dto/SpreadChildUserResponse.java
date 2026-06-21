package com.jsy.crmeb.modern.service.distribution.dto;

public class SpreadChildUserResponse {
    private Integer uid;
    private String avatar;
    private String nickname;
    private Integer isPromoter;
    private Integer spreadCount;
    private Integer payCount;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getIsPromoter() { return isPromoter; }
    public void setIsPromoter(Integer isPromoter) { this.isPromoter = isPromoter; }
    public Integer getSpreadCount() { return spreadCount; }
    public void setSpreadCount(Integer spreadCount) { this.spreadCount = spreadCount; }
    public Integer getPayCount() { return payCount; }
    public void setPayCount(Integer payCount) { this.payCount = payCount; }
}
