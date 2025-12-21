package com.example.reservationsystem.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestDto {
    private Long roomId;

    private LocalDate date;

    private LocalTime startTime;

    private int duration; // 1~2

    private Long representativeStudentId;

    private List<Long> participantStudentIds; // 대표자 제외 동반자 id 리스트
}
