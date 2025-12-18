package com.example.reservationsystem.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class RoomReservationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private RoomReservation reservation;

    private boolean isRepresentative = false;


    @ManyToOne
    private Student student;

    public static RoomReservationParticipant of(RoomReservation reservation, Student student, boolean isRepresentative) {
        RoomReservationParticipant p = new RoomReservationParticipant();
        p.reservation = reservation;
        p.student = student;
        p.isRepresentative = isRepresentative;
        return p;
    }
}
