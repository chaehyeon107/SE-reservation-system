package com.example.reservationsystem.common.enums;

import com.example.reservationsystem.common.response.ApiResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@Schema(hidden = true)
public enum ErrorCode implements BaseCode {

    // 400 BAD REQUEST
    INVALID_PARTICIPANT_COUNT(HttpStatus.BAD_REQUEST, "참가자는 최소 3명 이상이어야 합니다."),
    OUT_OF_OPERATING_HOURS(HttpStatus.BAD_REQUEST, "운영 시간(09:00~18:00) 내에서만 예약할 수 있습니다."),
    ROOM_ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "이미 예약된 시간입니다."),
    OVERLAPPING_RESERVATION(HttpStatus.BAD_REQUEST, "해당 학생은 같은 시간대에 다른 예약이 존재합니다."),
    ROOM_DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "하루 최대 회의실 이용 시간을 초과했습니다."),
    ROOM_WEEKLY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "주간 최대 회의실 이용 시간을 초과했습니다."),
    INVALID_SEAT_ID(HttpStatus.BAD_REQUEST, "좌석 번호는 1~70 사이여야 합니다."),
    SEAT_DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "일 최대 이용 시간(4시간)을 초과했습니다."),
    INVALID_DURATION_HOURS(HttpStatus.BAD_REQUEST, "이용 가능 시간은 2시간 또는 4시간입니다."),

    // ====== 404 NOT_FOUND ======
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회의실입니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 내역을 찾을 수 없습니다."),
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 좌석을 찾을 수 없습니다."),

    // ====== 403 FORBIDDEN ======
    NO_CANCEL_PERMISSION(HttpStatus.FORBIDDEN, "예약 취소 권한은 대표자에게만 있습니다."),

    // ====== 409 CONFLICT ======
    ALREADY_CANCELED_RESERVATION(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),
    INVALID_STUDENT_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 학번입니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "예약 시간 범위가 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "해당 시간대에 이미 예약된 좌석입니다."),
    NO_AVAILABLE_SEATS(HttpStatus.CONFLICT, "예약 가능한 좌석이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
    @JsonIgnore
    @Schema(hidden = true)
    private final ApiResult apiResult;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.apiResult = ApiResult.builder()
                .success(false)
                .httpStatus(httpStatus)
                .message(message)
                .build();
    }

    @Override
    public ApiResult getReasonHttpStatus() {
        return apiResult;
    }
}
