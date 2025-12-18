package com.example.reservationsystem.domain.dto;

import com.example.reservationsystem.domain.entity.Room;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomResponseDto {

    private Long id;

    public static RoomResponseDto from(Room room) {
        return RoomResponseDto.builder()
                .build();
    }
}
