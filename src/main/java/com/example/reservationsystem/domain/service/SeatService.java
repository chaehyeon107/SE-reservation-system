package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.domain.repository.SeatRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.reservationsystem.domain.entity.Seat;

@Builder
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    @Transactional
    public void createDefaultSeats() {

        // if (seatRepository.count() > 0) return; // 중복 생성 방지

        for (int i = 1; i <= 70; i++) {
            seatRepository.save(Seat.builder()
                    .seatNumber(i)
                    .build());
        }
    }
}
