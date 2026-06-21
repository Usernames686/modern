package com.zbkj.common.exception;

import com.zbkj.common.result.IResultEnum;

/**
 * @ClassName BusinessExceptionAssert
 * @Description 业务异常断言
 * @Version 1.0
 */
public interface BusinessExceptionAssert extends IResultEnum, Assert {

    @Override
    default CrmebException newException() {
        return new BusinessException(this);
    }

}
