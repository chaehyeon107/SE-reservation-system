package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.repository.RoomRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Builder
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public void createDefaultRooms() {

        if (roomRepository.count() > 0) return;

        roomRepository.save(Room.builder().build());
        roomRepository.save(Room.builder().build());
        roomRepository.save(Room.builder().build());
    }
}
