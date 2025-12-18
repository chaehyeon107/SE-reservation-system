package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.RoomReservation;
import com.example.reservationsystem.domain.entity.RoomReservationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface RoomReservationParticipantRepository extends JpaRepository<RoomReservationParticipant, Long> {

    @Query("""
select (count(p) > 0)
from RoomReservationParticipant p
join p.reservation r
where p.student.studentId = :studentId
  and r.date = :date
  and r.status = com.example.reservationsystem.domain.entity.RoomReservationStatus.RESERVED
  and r.startTime < :endTime
  and r.endTime > :startTime
""")
    boolean existsStudentOverlap(@Param("studentId") Long studentId,
                                 @Param("date") LocalDate date,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime);

    @Query("""
    select r
    from RoomReservationParticipant p
    join p.reservation r
    where p.student.studentId = :studentId
      and r.date = :date
      and r.startTime <= :now
      and r.endTime > :now
      and r.status = com.example.reservationsystem.domain.entity.RoomReservationStatus.RESERVED
""")
    Optional<RoomReservation> findCurrentReservationByStudentId(
            Long studentId,
            LocalDate date,
            LocalTime now
    );

    @Query("""
    select p.reservation
    from RoomReservationParticipant p
    where p.student.studentId = :studentId
    order by p.reservation.date asc, p.reservation.startTime asc
""")
    List<RoomReservation> findReservationsByStudentId(Long studentId);


    Optional<RoomReservationParticipant> findByReservation_IdAndStudent_StudentId(Long reservationId, Long studentId);

    List<RoomReservationParticipant> findAllByReservation_Id(Long reservationId);


}