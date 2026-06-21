package com.jsy.crmeb.modern.service.distribution.dto;

public class SpreadUserRequest {
    private String nickName;
    private String dateLimit;
    private Integer type = 0;
    private Integer uid;

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getDateLimit() { return dateLimit; }
    public void setDateLimit(String dateLimit) { this.dateLimit = dateLimit; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
}
