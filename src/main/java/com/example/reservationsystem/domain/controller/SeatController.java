package com.example.reservationsystem.domain.controller;

import com.example.reservationsystem.common.enums.SuccessCode;
import com.example.reservationsystem.common.response.ApiResponse;
import com.example.reservationsystem.domain.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    // 좌석(1~70) 초기 데이터 생성 (중복 생성 방지는 서비스에서 처리)
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<Void>> createDefaultSeats() {
        seatService.createDefaultSeats();
        return ApiResponse.onSuccess(SuccessCode.CREATE_SEAT_SUCCESS, null);
    }

}
