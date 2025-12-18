package com.example.reservationsystem.domain.controller;

import com.example.reservationsystem.common.enums.SuccessCode;
import com.example.reservationsystem.common.response.ApiResponse;
import com.example.reservationsystem.domain.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    // 회의실 초기 데이터 생성 (중복 생성 방지는 서비스에서 처리)
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<Object>> createDefaultRooms() {
        roomService.createDefaultRooms();
        return ApiResponse.onSuccess(SuccessCode.CREATE_ROOM_SUCCESS, null);
    }
}
