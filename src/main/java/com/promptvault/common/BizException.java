package com.promptvault.common;

/**
 * 业务异常：由 Service 主动抛出，由全局异常处理器转成 code=1 的响应。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
