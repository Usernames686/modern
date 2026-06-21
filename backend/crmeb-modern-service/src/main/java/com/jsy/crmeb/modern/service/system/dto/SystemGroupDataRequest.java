package com.jsy.crmeb.modern.service.system.dto;

import java.util.Map;

public class SystemGroupDataRequest {
    private Integer gid;
    private Map<String, Object> form;

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    public Map<String, Object> getForm() {
        return form;
    }

    public void setForm(Map<String, Object> form) {
        this.form = form;
    }
}
