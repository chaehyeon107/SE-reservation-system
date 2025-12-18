package com.example.reservationsystem.domain.dto;

import com.example.reservationsystem.domain.entity.RoomReservation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationResponseDto {

    private final Long id;

        public static ReservationResponseDto from (RoomReservation reservation){
            return ReservationResponseDto.builder()
                    .id(reservation.getId())
                    .build();
        }
    }
