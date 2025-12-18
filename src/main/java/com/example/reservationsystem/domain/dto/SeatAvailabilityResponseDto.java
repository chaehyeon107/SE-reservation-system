package com.example.reservationsystem.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SeatAvailabilityResponseDto {
    private List<Integer> reservedSeatIds;

    public static SeatAvailabilityResponseDto of(List<Integer> ids) {
        return new SeatAvailabilityResponseDto(ids);
    }
}