package com.example.reservationsystem.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_reservation")
@Builder
public class RoomReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Room room;

    @ManyToOne
    private Student representative;

    //대표자 학번
    private Long leaderStudentId;

    // 회의실 예약 날짜
    private LocalDate date;

    //시작
    private LocalTime startTime;

    //종료
    private LocalTime endTime;

    //회의실 사용 시간
    //한 번 예약 시 최대 2시간
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