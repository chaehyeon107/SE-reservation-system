package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.dto.*;
import com.example.reservationsystem.domain.entity.*;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationParticipantRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomReservationService {

    private final RoomReservationRepository roomReservationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationParticipantRepository roomReservationParticipantRepository;

    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(18, 0);
    private static final int ROOM_DAILY_LIMIT_HOURS = 2;
    private static final int ROOM_WEEKLY_LIMIT_HOURS = 5;

    @Transactional
    public ReservationResponseDto createRoomReservation(ReservationRequestDto req) {

        /* =====================================================
         * 0) 학번 검증 (대표자 + 참가자 전원)
         * ===================================================== */
        Set<Long> allStudentIds = new LinkedHashSet<>();
        allStudentIds.add(req.getRepresentativeStudentId());
        if (req.getParticipantStudentIds() != null) {
            allStudentIds.addAll(req.getParticipantStudentIds());
        }

        for (Long sid : allStudentIds) {
            validateStudentId(sid);
        }

        /* =====================================================
         * 1) 회의실 존재 확인
         * ===================================================== */
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        /* =====================================================
         * 2) 대표자 Student 확보
         * ===================================================== */
        Student representative = studentRepository.findByStudentId(req.getRepresentativeStudentId())
                .orElseGet(() -> studentRepository.save(Student.of(req.getRepresentativeStudentId())));

        /* =====================================================
         * 3) 종료 시간 계산
         * ===================================================== */
        LocalTime endTime = req.getStartTime().plusHours(req.getDuration());

        /* =====================================================
         * 4) 회의실 시간 겹침 검사
         * ===================================================== */
        boolean hasConflict =
                roomReservationRepository.existsActiveReservationOverlap(
                        req.getRoomId(),
                        req.getDate(),
                        req.getStartTime(),
                        endTime
                );

        if (hasConflict) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_RESERVED);
        }

        /* =====================================================
         * 5) 참가자 수 검증 (최소 3명)
         * ===================================================== */
        Set<Long> participantIds = new LinkedHashSet<>();
        participantIds.add(req.getRepresentativeStudentId());
        if (req.getParticipantStudentIds() != null) {
            participantIds.addAll(req.getParticipantStudentIds());
        }

        if (participantIds.size() < 3) {
            throw new CustomException(ErrorCode.INVALID_PARTICIPANT_COUNT);
        }

        /* =====================================================
         * 6) 운영 시간 검증
         * ===================================================== */
        if (req.getStartTime().isBefore(OPEN)
                || endTime.isAfter(CLOSE)
                || !req.getStartTime().isBefore(endTime)) {
            throw new CustomException(ErrorCode.OUT_OF_OPERATING_HOURS);
        }

        /* =====================================================
         * 7) 회의실 중복 예약 검증 (RESERVED 기준)
         * ===================================================== */
        if (roomReservationRepository.existsRoomOverlap(
                room.getId(),
                req.getDate(),
                RoomReservationStatus.RESERVED,
                req.getStartTime(),
                endTime)) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_RESERVED);
        }

        /* =====================================================
         * 8) 참가자 Student 확보 + 시간/한도 검증
         * ===================================================== */
        Map<Long, Student> studentMap = new HashMap<>();

        for (Long sid : participantIds) {
            Student stu = studentRepository.findByStudentId(sid)
                    .orElseGet(() -> studentRepository.save(Student.of(sid)));

            // 날짜 변경 시 daily / weekly reset
            stu.resetIfNeeded(req.getDate());

            // (A) 개인 일정 겹침 방지
            if (roomReservationParticipantRepository.existsStudentOverlap(
                    sid,
                    req.getDate(),
                    req.getStartTime(),
                    endTime)) {
                throw new CustomException(ErrorCode.OVERLAPPING_RESERVATION);
            }

            // (B) 일일 / 주간 한도 검증
            if (stu.getMeetingDailyUsedHours() + req.getDuration() > ROOM_DAILY_LIMIT_HOURS) {
                throw new CustomException(ErrorCode.ROOM_DAILY_LIMIT_EXCEEDED);
            }
            if (stu.getMeetingWeeklyUsedHours() + req.getDuration() > ROOM_WEEKLY_LIMIT_HOURS) {
                throw new CustomException(ErrorCode.ROOM_WEEKLY_LIMIT_EXCEEDED);
            }

            studentMap.put(sid, stu);
        }

        /* =====================================================
         * 9) 예약 저장
         * ===================================================== */
        RoomReservation saved = roomReservationRepository.save(
                RoomReservation.of(
                        room,
                        representative,
                        req.getDate(),
                        req.getStartTime(),
                        req.getDuration()
                )
        );

        /* =====================================================
         * 10) 참가자-예약 매핑 저장
         * ===================================================== */
        roomReservationParticipantRepository.save(
                RoomReservationParticipant.of(saved, representative, true)
        );

        if (req.getParticipantStudentIds() != null) {
            Set<Long> companions = new LinkedHashSet<>(req.getParticipantStudentIds());
            companions.remove(req.getRepresentativeStudentId());

            for (Long sid : companions) {
                roomReservationParticipantRepository.save(
                        RoomReservationParticipant.of(saved, studentMap.get(sid), false)
                );
            }
        }

        /* =====================================================
         * 11) 누적 사용 시간 반영
         * ===================================================== */
        for (Student stu : studentMap.values()) {
            stu.applyMeetingUsageDelta(req.getDuration());
        }

        return ReservationResponseDto.from(saved);
    }

    //리스트 형식으로 전체조회
    @Transactional(readOnly = true)
    public List<ReservationDetailDto> getReservationsByStudentId(Long studentId) {
        validateStudentId(studentId);

        List<RoomReservation> reservations =
                roomReservationParticipantRepository.findReservationsByStudentId(studentId);

        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }

        return reservations.stream()
                .map(ReservationDetailDto::from)
                .toList();
    }

    //회의실 조회 리스트
    @Transactional(readOnly = true)
    public List<RoomScheduleDto> getRoomSchedules(LocalDate date) {

        // 전체 회의실 조회
        List<Room> rooms = roomRepository.findAll();

        List<RoomScheduleDto> result = new ArrayList<>();

        for (Room room : rooms) {

            // 해당 회의실 + 날짜의 예약들 가져오기
            List<RoomReservation> reservations =
                    roomReservationRepository.findByRoom_IdAndDate(room.getId(), date);

            // 시간 정보 DTO로 변환
            List<RoomReservationTimeDto> reservationDtos = reservations.stream()
                    .map(r -> new RoomReservationTimeDto(
                            r.getId(),
                            r.getStartTime(),
                            r.getEndTime(),
                            r.getLeaderStudentId()
                    ))
                    .toList();

            // 회의실 일정표 DTO 생성
            result.add(new RoomScheduleDto(
                    room.getId(),
                    reservationDtos
            ));
        }

        return result;
    }

    //회의실에서 예약 취소 업데이트
    @Transactional
    public void cancelMeetingReservation(Long reservationId, Long studentId) {

        validateStudentId(studentId);

        //예약이 있는지 확인.
        RoomReservation reservation = roomReservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        // 이미 취소된 예약이면 방어
        if (reservation.getStatus() == RoomReservationStatus.CANCELED
                || reservation.getStatus() == RoomReservationStatus.CANCELED_REFUND
                || reservation.getStatus() == RoomReservationStatus.CANCELED_PENALTY) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED_RESERVATION);
        }

        // 대표자만 취소 가능
        RoomReservationParticipant me = roomReservationParticipantRepository
                .findByReservation_IdAndStudent_StudentId(reservationId, studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_CANCEL_PERMISSION));

        if (!me.isRepresentative()) {
            throw new CustomException(ErrorCode.NO_CANCEL_PERMISSION);
        }

        //현재 시간 비교
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(kst);

        LocalDateTime startAt = LocalDateTime.of(
                reservation.getDate(),
                reservation.getStartTime()
        );
        LocalDateTime endAt = LocalDateTime.of(
                reservation.getDate(),
                reservation.getEndTime()
        );

        if (now.isAfter(endAt)) {
            throw new CustomException(ErrorCode.RESERVATION_ALREADY_FINISHED);
        }

        boolean beforeStart = now.isBefore(startAt);

        int durationHours = (int) Duration
                .between(reservation.getStartTime(), reservation.getEndTime())
                .toHours();

        int delta = beforeStart ? -durationHours : +durationHours;

        List<RoomReservationParticipant> participants =
                roomReservationParticipantRepository.findAllByReservation_Id(reservationId);

        LocalDate today = now.toLocalDate();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        for (RoomReservationParticipant p : participants) {
            Student s = p.getStudent();

            resetMeetingUsageIfNeeded(s, today, weekStart);
            s.applyMeetingUsageDelta(delta);
        }

        reservation.cancel(
                beforeStart
                        ? RoomReservationStatus.CANCELED_REFUND
                        : RoomReservationStatus.CANCELED_PENALTY
        );
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

    private void resetMeetingUsageIfNeeded(Student s,
                                           LocalDate today,
                                           LocalDate weekStart) {

        if (!today.equals(s.getUsageDate())) {
            s.resetMeetingDailyUsage();
            s.updateUsageDate(today);
        }

        if (!weekStart.equals(s.getUsageWeekStart())) {
            s.resetMeetingWeeklyUsage();
            s.updateUsageWeekStart(weekStart);
        }
    }


}


