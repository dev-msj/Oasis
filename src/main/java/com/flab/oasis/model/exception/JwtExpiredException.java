package com.flab.oasis.model.exception;

import com.flab.oasis.constant.ErrorCode;
import com.flab.oasis.utils.LogUtils;
import lombok.Getter;

@Getter
public class JwtExpiredException extends RuntimeException {
    private static final long serialVersionUID = -8894974550753507839L;

    private final ErrorCode errorCode;

    public JwtExpiredException(ErrorCode errorCode, String message, String value) {
        super(message);
        this.errorCode = errorCode;

        LogUtils.error(this.getClass(), errorCode, message, value);
    }
}
