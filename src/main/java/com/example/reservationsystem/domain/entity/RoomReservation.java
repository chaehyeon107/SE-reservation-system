package com.example.reservationsystem.domain.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Room room;

    @ManyToOne
    private Student representative;

    private Long leaderStudentId;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private int duration;

    @Enumerated(EnumType.STRING)
    private RoomReservationStatus status;

//    public boolean isCanceled() {
//        return status == RoomReservationStatus.CANCELED_REFUND
//                || status == RoomReservationStatus.CANCELED_PENALTY;
//    }

    public void cancel(RoomReservationStatus status) {
        this.status = status;
    }

    public static RoomReservation of(
            Room room,
            Student representative,
            LocalDate date,
            LocalTime startTime,
            int duration
    ) {
        RoomReservation reservation = new RoomReservation();
        reservation.room = room;
        reservation.representative = representative;
        reservation.leaderStudentId = representative.getStudentId();
        reservation.date = date;
        reservation.startTime = startTime;
        reservation.duration = duration;
        reservation.endTime = startTime.plusHours(duration);
        reservation.status = RoomReservationStatus.RESERVED;
        return reservation;
    }
}

//[
//        {
//        "id": 10,
//        "roomId": 1,
//        "date": "2025-12-10",
//        "startTime": "09:00",
//        "endTime": "11:00",
//        "leaderStudentId": "202100001"
//        }
//        ]