package com.example.reservationsystem.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;


@Getter
@AllArgsConstructor
public class RoomReservationTimeDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long leaderStudentId;
}