package com.jsy.crmeb.modern.service.admin;

public class AdminAuthException extends RuntimeException {
    private final int code;

    public AdminAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
