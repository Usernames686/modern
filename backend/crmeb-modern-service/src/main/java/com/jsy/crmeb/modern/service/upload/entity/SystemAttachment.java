package com.jsy.crmeb.modern.service.upload.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("eb_system_attachment")
public class SystemAttachment {
    @TableId("att_id")
    private Integer attId;
    private String name;
    private String attDir;
    private String sattDir;
    private String attSize;
    private String attType;
    private Integer pid;
    private Integer imageType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getAttId() {
        return attId;
    }

    public void setAttId(Integer attId) {
        this.attId = attId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttDir() {
        return attDir;
    }

    public void setAttDir(String attDir) {
        this.attDir = attDir;
    }

    public String getSattDir() {
        return sattDir;
    }

    public void setSattDir(String sattDir) {
        this.sattDir = sattDir;
    }

    public String getAttSize() {
        return attSize;
    }

    public void setAttSize(String attSize) {
        this.attSize = attSize;
    }

    public String getAttType() {
        return attType;
    }

    public void setAttType(String attType) {
        this.attType = attType;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getImageType() {
        return imageType;
    }

    public void setImageType(Integer imageType) {
        this.imageType = imageType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
