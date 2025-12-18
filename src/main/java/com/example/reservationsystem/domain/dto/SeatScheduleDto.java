package com.example.reservationsystem.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SeatScheduleDto {

    private Long seatId;
    private List<SeatReservationTimeDto> reservations;

}
