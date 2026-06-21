package com.jsy.crmeb.modern.service.user.dto;

public class UserUpdateRequest {
    private Integer uid;
    private String realName;
    private String birthday;
    private String cardId;
    private String mark;
    private Boolean status;
    private String addres;
    private String groupId;
    private String tagId;
    private Boolean isPromoter;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public String getAddres() { return addres; }
    public void setAddres(String addres) { this.addres = addres; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public Boolean getIsPromoter() { return isPromoter; }
    public void setIsPromoter(Boolean isPromoter) { this.isPromoter = isPromoter; }
}
