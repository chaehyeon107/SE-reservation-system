package com.example.reservationsystem.domain.controller;

import com.example.reservationsystem.common.enums.SuccessCode;
import com.example.reservationsystem.common.response.ApiResponse;
import com.example.reservationsystem.domain.dto.SeatReservationRequestDto;
import com.example.reservationsystem.domain.dto.SeatReservationResponseDto;
import com.example.reservationsystem.domain.dto.SeatResponseDto;
import com.example.reservationsystem.domain.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatReservationController {

    private final SeatReservationService seatReservationService;

    // ----------------------------------------------------------
    // A) 좌석 예약 현황 조회 (해당 시간대 예약된 seatId 리스트)
    // GET /api/seats/availability
    // ----------------------------------------------------------
    @GetMapping("/availability")
    public ResponseEntity<?> getReservedSeatIds(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm")
            LocalTime startTime,
            @RequestParam int durationHours
    ) {
        List<Integer> reservedSeatIds =
                seatReservationService.getReservedSeatIds(date, startTime, durationHours);

        return ApiResponse.onSuccess(SuccessCode.GET_SEAT_AVAILABILITY_SUCCESS, reservedSeatIds);
    }


    // ----------------------------------------------------------
    // B) 내 좌석 예약 조회
    // GET /api/seats/reservations?studentId=202100002
    // ----------------------------------------------------------
    @GetMapping("/reservations")
    public ResponseEntity<?> getMySeatReservations(
            @RequestParam Long studentId
    ) {
        List<SeatReservationResponseDto> list =
                seatReservationService.getReservationsByStudentId(studentId);

        return ApiResponse.onSuccess(SuccessCode.GET_SEAT_RESERVATIONS_SUCCESS, list);
    }


    // ----------------------------------------------------------
    // C) 좌석 지정 예약 생성
    // POST /api/seats/reservations
    // ----------------------------------------------------------
    @PostMapping("/reservations")
    public ResponseEntity<?> createSeatReservation(
            @RequestBody SeatReservationRequestDto req
    ) {
        SeatResponseDto dto =
                seatReservationService.createSeatReservation(req);

        return ApiResponse.onSuccess(SuccessCode.CREATE_SEAT_RESERVATION_SUCCESS, dto);
    }


    // ----------------------------------------------------------
    // D) 좌석 랜덤 예약 생성
    // POST /api/seats/reservations/random
    // ----------------------------------------------------------
    @PostMapping("/reservations/random")
    public ResponseEntity<?> createRandomSeatReservation(
            @RequestBody SeatReservationRequestDto req
    ) {
        SeatResponseDto dto =
                seatReservationService.createRandomSeatReservation(req);

        return ApiResponse.onSuccess(SuccessCode.CREATE_RANDOM_SEAT_RESERVATION_SUCCESS, dto);
    }


    //     ----------------------------------------------------------
//     E) 좌석 예약 취소
//     DELETE /api/seats/reservations/{id}?studentId=XXXX
//     ----------------------------------------------------------
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<?> cancelSeatReservation(@PathVariable("id") Long reservationId, @RequestParam("studentId") Long studentId) {
        String message = seatReservationService.cancelSeatReservation(reservationId, studentId);
        return ResponseEntity.ok(Map.of("message", message));
    }
}

