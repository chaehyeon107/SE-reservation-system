package com.example.reservationsystem.common.exception;

import com.example.reservationsystem.common.enums.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomException extends RuntimeException {

    private final BaseCode errorCode;

}

