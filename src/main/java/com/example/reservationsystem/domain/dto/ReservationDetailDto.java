package com.example.reservationsystem.domain.dto;

import com.example.reservationsystem.domain.entity.RoomReservation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ReservationDetailDto {

    private final Long id;

    private final Long roomId;

    private final LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private final LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private final LocalTime endTime;

    private final Long leaderStudentId;

    public static ReservationDetailDto from(RoomReservation r) {
        return ReservationDetailDto.builder()
                .id(r.getId())
                .roomId(r.getRoom().getId())
                .date(r.getDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .leaderStudentId(r.getRepresentative().getStudentId())
                .build();
    }
}
