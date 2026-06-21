package com.jsy.crmeb.modern.service.upload.dto;

public class AttachmentMoveRequest {
    private String attrId;
    private Integer pid;

    public String getAttrId() {
        return attrId;
    }

    public void setAttrId(String attrId) {
        this.attrId = attrId;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }
}
