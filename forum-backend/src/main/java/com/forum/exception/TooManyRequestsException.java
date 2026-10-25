package com.forum.exception;
/** 限流触发（HTTP 429），与普通业务错误区分开。 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
