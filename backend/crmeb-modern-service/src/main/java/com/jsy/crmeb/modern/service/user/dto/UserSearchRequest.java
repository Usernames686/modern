package com.jsy.crmeb.modern.service.user.dto;

public class UserSearchRequest {
    private int page = 1;
    private int limit = 20;
    private String keywords;
    private String dateLimit;
    private String groupId;
    private String labelId;
    private String userType;
    private Integer status;
    private Integer isPromoter;
    private String payCount;
    private String level;
    private Integer accessType = 0;
    private String country;
    private String province;
    private String city;
    private String sex;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getDateLimit() { return dateLimit; }
    public void setDateLimit(String dateLimit) { this.dateLimit = dateLimit; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getLabelId() { return labelId; }
    public void setLabelId(String labelId) { this.labelId = labelId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsPromoter() { return isPromoter; }
    public void setIsPromoter(Integer isPromoter) { this.isPromoter = isPromoter; }
    public String getPayCount() { return payCount; }
    public void setPayCount(String payCount) { this.payCount = payCount; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getAccessType() { return accessType; }
    public void setAccessType(Integer accessType) { this.accessType = accessType; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
}
