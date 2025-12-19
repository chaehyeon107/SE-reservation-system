package com.example.reservationsystem.common.enums;

import com.example.reservationsystem.common.response.ApiResult;
import org.springframework.http.HttpStatus;

public enum SuccessCode implements BaseCode {

    CREATE_ROOM_SUCCESS(HttpStatus.CREATED, "회의실이 생성되었습니다."),
    CREATE_SEAT_SUCCESS(HttpStatus.CREATED, "좌석이 생성되었습니다."),
    GET_CURRENT_RESERVATION_SUCCESS(HttpStatus.OK,"현재 예약 조회에 성공했습니다."),
    GET_ROOM_LIST_SUCCESS(HttpStatus.OK, "회의실 목록 조회에 성공했습니다."),
    CANCEL_RESERVATION_SUCCESS(HttpStatus.OK, "예약이 정상적으로 취소되었습니다."),
    CREATE_RESERVATION_SUCCESS(HttpStatus.CREATED,"회의실 예약이 완료되었습니다."),

    GET_SEAT_AVAILABILITY_SUCCESS(HttpStatus.OK,"예약된 좌석 조회에 성공했습니다."),
    GET_SEAT_RESERVATIONS_SUCCESS(HttpStatus.OK,"내 좌석 예약 조회에 성공했습니다."),
    CREATE_SEAT_RESERVATION_SUCCESS(HttpStatus.OK,"좌석 예약이 완료되었습니다."),
    CREATE_RANDOM_SEAT_RESERVATION_SUCCESS(HttpStatus.OK,"랜덤 좌석 예약이 완료되었습니다.");
    //CANCEL_SEAT_RESERVATION_SUCCESS("예약이 취소되었습니다.");

    // 본 코드
    private final HttpStatus httpStatus;
    private final String message;
    private final ApiResult apiResult;

    SuccessCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.apiResult = ApiResult.builder()
                .success(true)
                .httpStatus(httpStatus)
                .message(message)
                .build();

    }

    @Override
    public ApiResult getReasonHttpStatus() {
        return apiResult;
    }
}
