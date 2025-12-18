package com.example.reservationsystem.domain.dto;

import com.example.reservationsystem.domain.entity.SeatReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationResponseDto {

    private Long id;
    private int seatId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long studentId;

    public static SeatReservationResponseDto of(SeatReservation r) {
        return SeatReservationResponseDto.builder()
                .id(r.getId())
                .seatId(r.getSeat().getId().intValue())
                .date(r.getDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .studentId(r.getStudent().getStudentId())
                .build();
    }
}
