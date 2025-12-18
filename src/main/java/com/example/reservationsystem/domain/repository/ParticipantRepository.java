package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.RoomReservationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository
        extends JpaRepository<RoomReservationParticipant, Long> {

    Optional<RoomReservationParticipant> findByReservation_IdAndStudent_StudentId(Long reservationId, Long studentId);
}