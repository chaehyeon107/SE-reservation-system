package com.example.reservationsystem.common.exception;

import com.example.reservationsystem.common.enums.BaseCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final BaseCode errorCode;

    // ✅ 핵심: RuntimeException의 message를 세팅해준다.
    public CustomException(BaseCode errorCode) {
        super(errorCode.getReasonHttpStatus().getMessage());
        this.errorCode = errorCode;
    }

    // (선택) cause까지 붙이고 싶을 때
    public CustomException(BaseCode errorCode, Throwable cause) {
        super(errorCode.getReasonHttpStatus().getMessage(), cause);
        this.errorCode = errorCode;
    }
}
