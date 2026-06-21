package com.jsy.crmeb.modern.service.admin.dto;

import java.util.Map;

public class AdminLoginRequest {
    private String account;
    private String pwd;
    private Map<String, Object> captchaVO;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public Map<String, Object> getCaptchaVO() {
        return captchaVO;
    }

    public void setCaptchaVO(Map<String, Object> captchaVO) {
        this.captchaVO = captchaVO;
    }
}
