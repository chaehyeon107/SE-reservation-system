package com.example.reservationsystem.domain.entity;

public enum RoomReservationStatus {
    RESERVED,
    CANCELED_REFUND,   // 시작 전 취소(환급)
    CANCELED_PENALTY, // 시작 후 취소(패널티 차감 유지)
    CANCELED
}
