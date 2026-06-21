package com.zbkj.common.exception;

import com.zbkj.common.result.CommonResultCode;
import com.zbkj.common.result.IResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Crmeb异常类


 */
@Slf4j
@RestControllerAdvice
public class CrmebException extends RuntimeException {

    private static final long serialVersionUID = 6397082987802748517L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String message;

    public CrmebException() {
        super();
    }

    public CrmebException(String message) {
        super(CommonResultCode.ERROR.getCode() + "-" + message);
        this.code = CommonResultCode.ERROR.getCode();
        this.message = message;
    }

    public CrmebException(IResultEnum iResultEnum) {
        super(iResultEnum.getCode() + "-" + iResultEnum.getMessage());
        this.code = iResultEnum.getCode();
        this.message = iResultEnum.getMessage();
    }

    public CrmebException(IResultEnum iResultEnum, String message) {
        super(iResultEnum.getCode() + "-" + message);
        this.code = iResultEnum.getCode();
        this.message = message;
    }

    public CrmebException(IResultEnum iResultEnum, Throwable throwable) {
        super(iResultEnum.getCode() + "-" + iResultEnum.getMessage() + ", " + throwable.getMessage(), throwable);
        this.code = iResultEnum.getCode();
        this.message = iResultEnum.getMessage();
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
