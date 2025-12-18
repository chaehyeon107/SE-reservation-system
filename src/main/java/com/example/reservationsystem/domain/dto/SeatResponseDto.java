package com.example.reservationsystem.domain.dto;

import com.example.reservationsystem.domain.entity.SeatReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SeatResponseDto {

    private Long id;

    public static SeatResponseDto from(SeatReservation reservation) {
        return SeatResponseDto.builder()
                .id(reservation.getId())
                .build();
    }
}
