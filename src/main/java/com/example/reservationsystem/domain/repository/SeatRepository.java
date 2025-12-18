package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {

}

