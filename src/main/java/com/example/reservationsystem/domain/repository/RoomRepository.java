package com.example.reservationsystem.domain.repository;


import com.example.reservationsystem.domain.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
