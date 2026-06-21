package com.jsy.crmeb.modern.service.user.dto;

public class SystemUserLevelResponse {
    private Integer id;
    private String name;
    private Integer experience;
    private Integer isShow;
    private Integer grade;
    private Integer discount;
    private String icon;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
    public Integer getIsShow() { return isShow; }
    public void setIsShow(Integer isShow) { this.isShow = isShow; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public Integer getDiscount() { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
