package com.jsy.crmeb.modern.service.front;

public class FrontAuthException extends RuntimeException {
    private final int code;

    public FrontAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
