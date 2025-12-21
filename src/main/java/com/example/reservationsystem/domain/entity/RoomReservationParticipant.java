package com.example.reservationsystem.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "room_reservation_participant")
public class RoomReservationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private RoomReservation reservation;

    private boolean isRepresentative = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 참여 등록 시각 (ex: 2025-11-30 10:15:00)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;
    // 참여자 역할 (REPRESENTATIVE / PARTICIPANT)


    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    public static RoomReservationParticipant of(RoomReservation reservation, Student student, boolean isRepresentative) {
        RoomReservationParticipant p = new RoomReservationParticipant();
        p.reservation = reservation;
        p.student = student;
        p.isRepresentative = isRepresentative;
        return p;
    }
}
