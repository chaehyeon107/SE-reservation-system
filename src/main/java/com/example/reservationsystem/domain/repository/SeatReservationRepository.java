package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SeatReservationRepository  extends JpaRepository<SeatReservation, Long> {

    @Query("""
        SELECT COUNT(sr) > 0
        FROM SeatReservation sr
        WHERE sr.seat.id = :seatId
          AND sr.date = :date
          AND (
                (sr.startTime < :endTime)
                AND (sr.endTime > :startTime)
              )
    """)
    boolean existsSeatOverlap(
            @Param("seatId") Long seatId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
        SELECT COUNT(r) > 0
        FROM SeatReservation r
        WHERE r.student.studentId = :studentId
          AND r.date = :date
          AND (
                (r.startTime < :endTime AND r.endTime > :startTime)
              )
    """)
    boolean existsStudentOverlap(@Param("studentId") Long studentId,
                                 @Param("date") LocalDate date,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime);

    List<SeatReservation> findByStudent_StudentId(Long studentId);

    @Query("""
    select r.seat.id 
    from SeatReservation r
    where r.date = :date
      and r.startTime < :endTime
      and r.endTime > :startTime
""")
    List<Integer> findReservedSeats(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
    select distinct r.seat.id
    from SeatReservation r
    where r.date = :date
      and r.startTime < :endTime
      and r.endTime > :startTime
""")
    List<Long> findReservedSeatIds(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    List<SeatReservation> findByDate(LocalDate date);
}
