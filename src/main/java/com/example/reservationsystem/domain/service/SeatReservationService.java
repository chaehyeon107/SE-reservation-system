package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.dto.SeatReservationRequestDto;
import com.example.reservationsystem.domain.dto.SeatReservationResponseDto;
import com.example.reservationsystem.domain.dto.SeatResponseDto;
import com.example.reservationsystem.domain.entity.Seat;
import com.example.reservationsystem.domain.entity.SeatReservation;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.SeatRepository;
import com.example.reservationsystem.domain.repository.SeatReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

//회의실 예약
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatReservationService {

    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(18, 0);

    private final StudentRepository studentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public SeatResponseDto createSeatReservation(SeatReservationRequestDto req) {

        // 1) 학번 검증 + Student 조회 or 생성
        validateStudentId(req.getStudentId());

        Seat seat = seatRepository.findById(req.getSeatId())
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        Student student = studentRepository.findByStudentId(req.getStudentId())
                .orElseGet(() -> studentRepository.save(Student.of(req.getStudentId())));

        // 2) 종료시간
        LocalTime endTime = req.getStartTime().plusHours(req.getDurationHours());

        // 3) 운영시간 검증 (09:00 ~ 18:00)
        if (req.getStartTime().isBefore(OPEN) ||
                endTime.isAfter(CLOSE) ||
                !req.getStartTime().isBefore(endTime)) {
            throw new CustomException(ErrorCode.OUT_OF_OPERATING_HOURS);
        }

        // 4) 좌석 번호 검증
        if (req.getSeatId() < 1 || req.getSeatId() > 70) {
            throw new CustomException(ErrorCode.INVALID_SEAT_ID);
        }

        // 5) 날짜 변경 시 daily / weekly reset
        student.resetSeatIfNeeded(req.getDate());

        // 6) 하루 / 주간 누적 시간 한도 검증 (일 4시간, 한 번 예약할 때 2시간씩만 가능)
        int duration = req.getDurationHours();
        if (duration != 2 && duration != 4) {
            throw new CustomException(ErrorCode.INVALID_DURATION_HOURS);
        }
        int daily = student.getSeatDailyUsedHours();
        if (daily + duration > 4) {
            throw new CustomException(ErrorCode.SEAT_DAILY_LIMIT_EXCEEDED);
        }

        // 7) 좌석 중복 예약 여부 검사
        if (seatReservationRepository.existsSeatOverlap(
                req.getSeatId(),
                req.getDate(),
                req.getStartTime(),
                endTime))
        {
            throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
        }

        // 8) 사용자의 기존 좌석 예약과 겹침 여부 검사
        if (seatReservationRepository.existsStudentOverlap(
                req.getStudentId(),
                req.getDate(),
                req.getStartTime(),
                endTime))
        {
            throw new CustomException(ErrorCode.OVERLAPPING_RESERVATION);
        }

        // 9) 좌석 예약 저장
        SeatReservation saved = seatReservationRepository.save(
                SeatReservation.of(seat, student, req.getDate(), req.getStartTime(), req.getDurationHours())
        );

        // 10) 누적 시간 반영
        student.applySeatUsageDelta(req.getDurationHours());

        // 11) 응답
        return SeatResponseDto.from(saved);
    }

    @Transactional
    public SeatResponseDto createRandomSeatReservation(SeatReservationRequestDto req) {

        // 1) 학번 검증 + Student 조회 or 생성
        validateStudentId(req.getStudentId());
        Student student = studentRepository.findByStudentId(req.getStudentId())
                .orElseGet(() -> studentRepository.save(Student.of(req.getStudentId())));

        // 2) 종료 시간 계산
        LocalTime endTime = req.getStartTime().plusHours(req.getDurationHours());

        // 3) 운영시간 검증
        if (req.getStartTime().isBefore(OPEN) ||
                endTime.isAfter(CLOSE) ||
                !req.getStartTime().isBefore(endTime)) {
            throw new CustomException(ErrorCode.OUT_OF_OPERATING_HOURS);
        }

        // 4) 날짜 변경 시 daily / weekly reset
        student.resetSeatIfNeeded(req.getDate());

        // 5) 이용 시간 검증 (2 또는 4)
        int duration = req.getDurationHours();
        if (duration != 2 && duration != 4) {
            throw new CustomException(ErrorCode.INVALID_DURATION_HOURS);
        }

        // 6) 하루 사용 4시간 초과 검증
        if (student.getSeatDailyUsedHours() + duration > 4) {
            throw new CustomException(ErrorCode.SEAT_DAILY_LIMIT_EXCEEDED);
        }

        // 7) 사용자의 기존 예약과 시간 겹침 체크
        if (seatReservationRepository.existsStudentOverlap(
                req.getStudentId(), req.getDate(), req.getStartTime(), endTime)) {
            throw new CustomException(ErrorCode.OVERLAPPING_RESERVATION);
        }

        // 8) 해당 시간에 예약된 좌석 목록 조회
        List<Integer> reservedSeatIds = seatReservationRepository.findReservedSeats(
                req.getDate(), req.getStartTime(), endTime);

        // 전체 좌석(1~70)
        List<Integer> allSeats = IntStream.rangeClosed(1, 70)
                .boxed()
                .toList();

        // 예약 가능한 좌석 필터링
        List<Long> availableSeats = allSeats.stream()
                .map(Long::valueOf)
                .toList();

        // 9) 예약 가능한 좌석이 하나도 없으면 에러
        if (availableSeats.isEmpty()) {
            throw new CustomException(ErrorCode.NO_AVAILABLE_SEATS);
        }

        // 10) 랜덤 선택
        Long selectedSeatId = availableSeats.get(new Random().nextInt(availableSeats.size()));

        Seat seat = seatRepository.findById(selectedSeatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        // 11) 좌석 예약 저장
        SeatReservation saved = seatReservationRepository.save(
                SeatReservation.of(seat, student, req.getDate(), req.getStartTime(), duration)
        );

        // 12) 누적 시간 증가
        student.applySeatUsageDelta(duration);

        // 13) 응답 생성
        return SeatResponseDto.from(saved);
    }

    //내 좌석 예약 조회
    @Transactional(readOnly = true)
    public List<SeatReservationResponseDto> getReservationsByStudentId(Long studentId) {

        validateStudentId(studentId);

        //해당 학생의 좌석 예약 리스트 조회
        List<SeatReservation> reservations =
                seatReservationRepository.findByStudent_StudentId(studentId);

        //예약이 없으면 빈 리스트 반환
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }

        return reservations.stream()
                .map(SeatReservationResponseDto::of)
                .toList();
    }

    //
    @Transactional(readOnly = true)
    public List<Integer> getReservedSeatIds(LocalDate date, LocalTime startTime, int durationHours) {

        LocalTime endTime = startTime.plusHours(durationHours);

        // 해당 날짜의 모든 좌석 예약 조회
        List<SeatReservation> reservations =
                seatReservationRepository.findByDate(date);

        // 겹치는 예약만 seatId 리스트로 반환
        return reservations.stream()
                .filter(r ->
                        r.getStartTime().isBefore(endTime) &&
                                r.getEndTime().isAfter(startTime))
                .map(r -> r.getSeat().getId().intValue())
                .distinct()
                .toList();
    }

    private void validateStudentId(Long studentId) {
        if (studentId == null) {
            throw new CustomException(ErrorCode.INVALID_STUDENT_ID);
        }
        // 하드코딩 무효 학번
        if (studentId == 202099999L || studentId == 202288888L) {
            throw new CustomException(ErrorCode.INVALID_STUDENT_ID);
        }
    }

}
