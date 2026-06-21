package com.jsy.crmeb.modern.service.upload.dto;

public class AttachmentRequest {
    private Integer attId;
    private String name;
    private String attDir;
    private String sattDir;
    private String attSize;
    private String attType;
    private Integer pid;
    private Integer imageType;

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
}
