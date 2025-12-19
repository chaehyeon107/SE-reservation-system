package com.example.reservationsystem.domain.controller;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.enums.SuccessCode;
import com.example.reservationsystem.common.response.ApiResponse;
import com.example.reservationsystem.domain.dto.ReservationDetailDto;
import com.example.reservationsystem.domain.dto.ReservationRequestDto;
import com.example.reservationsystem.domain.dto.ReservationResponseDto;
import com.example.reservationsystem.domain.dto.RoomScheduleDto;
import com.example.reservationsystem.domain.service.RoomReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meeting/reservations")
@RequiredArgsConstructor
public class RoomReservationController {

    private final RoomReservationService roomReservationService;

    /**
     * 1) 회의실 예약 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDto>> createReservation(
            @Valid @RequestBody ReservationRequestDto req
    ) {
        ReservationResponseDto dto = roomReservationService.createRoomReservation(req);
        return ApiResponse.onSuccess(SuccessCode.CREATE_RESERVATION_SUCCESS, dto);
    }

    @GetMapping
    public ResponseEntity<?> getReservations(
            @Valid
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String studentId
    ) {

        // A) date 기반 회의실 일정 조회
        if (date != null) {
            LocalDate targetDate = LocalDate.parse(date);
            List<RoomScheduleDto> schedules =
                    roomReservationService.getRoomSchedules(targetDate);

            return ApiResponse.onSuccess(
                    SuccessCode.GET_CURRENT_RESERVATION_SUCCESS,
                    schedules
            );
        }

        // B) studentId 기반 내 예약 조회
        if (studentId != null) {
            List<ReservationDetailDto> list =
                    roomReservationService.getReservationsByStudentId(Long.parseLong(studentId));

            return ApiResponse.onSuccess(
                    SuccessCode.GET_ROOM_LIST_SUCCESS,
                    list
            );
        }

        return ApiResponse.onFailure(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 4) 예약 취소
     */
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @Valid
            @PathVariable Long reservationId,
            @RequestParam Long studentId
    ) {
        roomReservationService.cancelMeetingReservation(reservationId, studentId);
        return ApiResponse.onSuccess(SuccessCode.CANCEL_RESERVATION_SUCCESS, null);
    }
}