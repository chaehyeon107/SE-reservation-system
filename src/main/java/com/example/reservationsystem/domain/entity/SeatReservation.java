package com.example.reservationsystem.domain.entity;

import com.example.reservationsystem.common.entity.BaseEntity;
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
public class SeatReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seat_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "student_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Student student;

    //예약 일자
    private LocalDate date;

    //시작시간
    private LocalTime startTime;

    //종료시간
    private LocalTime endTime;


    public static SeatReservation of(Seat seat, Student student, LocalDate date, LocalTime startTime, int durationHours) {
        SeatReservation r = new SeatReservation();
        r.seat = seat;
        r.student = student;
        r.date = date;
        r.startTime = startTime;
        r.endTime = startTime.plusHours(durationHours); // 종료시간 계산
        return r;
    }
}

//[
//        {
//        "id": 200,
//        "seatId": 12,
//        "date": "2025-12-10",
//        "startTime": "09:00",
//        "endTime": "11:00",
//        "studentId": "202100002"
//        }
//        ]