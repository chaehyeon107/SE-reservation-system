package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.RoomReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    @Query("""
    select count(r) > 0
    from RoomReservation r
    where r.room.id = :roomId
      and r.date = :date
      and r.startTime < :endTime
      and r.endTime > :startTime
""")
    boolean existsRoomOverlap(
            @Param("roomId") Long roomId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

//    @Query("""
//    SELECT COALESCE(SUM(r.duration), 0)
//    FROM RoomReservationParticipant p
//    JOIN p.reservation r
//    WHERE p.student.studentId = :studentId
//      AND r.date = :date
//      AND r.status = 'ACTIVE'
//""")
//    int sumRoomDurationDaily(Long studentId, LocalDate date);
//
//    @Query("""
//    SELECT COALESCE(SUM(r.duration), 0)
//    FROM RoomReservationParticipant p
//    JOIN p.reservation r
//    WHERE p.student.studentId = :studentId
//      AND r.date BETWEEN :weekStart AND :weekEnd
//      AND r.status = 'ACTIVE'
//""")
//    int sumRoomDurationWeekly(Long studentId, LocalDate weekStart, LocalDate weekEnd);

    List<RoomReservation> findByRoom_IdAndDate(Long roomId, LocalDate date);
}