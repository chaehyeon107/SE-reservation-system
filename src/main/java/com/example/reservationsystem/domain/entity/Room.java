package com.example.reservationsystem.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

//회의실 3개. 1시간 단위 예약 가능
//학번 제출. 최소 3명부터 가능하며 오전 9시부터 18시까지
//하루 최대 2시간 일주일에 최대 5시간
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    //최소인원 체크
    private int capacity;

    private String location;

}

