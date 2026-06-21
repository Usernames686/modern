package com.jsy.crmeb.modern.service.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("eb_user_integral_record")
public class UserIntegralRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    @TableField("link_id")
    private String linkId;
    @TableField("link_type")
    private String linkType;
    private Integer type;
    private String title;
    private Integer integral;
    private Integer balance;
    private String mark;
    private Integer status;
    @TableField("frozen_time")
    private Integer frozenTime;
    @TableField("thaw_time")
    private Long thawTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }
    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getIntegral() { return integral; }
    public void setIntegral(Integer integral) { this.integral = integral; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getFrozenTime() { return frozenTime; }
    public void setFrozenTime(Integer frozenTime) { this.frozenTime = frozenTime; }
    public Long getThawTime() { return thawTime; }
    public void setThawTime(Long thawTime) { this.thawTime = thawTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
