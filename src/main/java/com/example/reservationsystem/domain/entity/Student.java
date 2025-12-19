package com.example.reservationsystem.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private String name;

    private int seatDailyUsedHours;

    private LocalDate usageDate;
    private LocalDate usageWeekStart;

    private int meetingDailyUsedHours;
    private int meetingWeeklyUsedHours;

    private LocalDate seatUsageDate;


    public static Student of(Long studentId) {
        Student s = new Student();
        s.studentId = studentId;
        return s;
    }

    public void resetIfNeeded(LocalDate date) {
        LocalDate weekStart = date.with(java.time.temporal.TemporalAdjusters
                .previousOrSame(java.time.DayOfWeek.MONDAY));

        if (usageDate == null || !usageDate.equals(date)) {
            meetingDailyUsedHours = 0;
            usageDate = date;
        }
        if (usageWeekStart == null || !usageWeekStart.equals(weekStart)) {
            meetingWeeklyUsedHours = 0;
            usageWeekStart = weekStart;
        }
    }

    public void resetSeatIfNeeded(LocalDate date) {
        if (seatUsageDate == null || !seatUsageDate.equals(date)) {
            seatDailyUsedHours = 0;
            seatUsageDate = date;
        }
    }

    public void resetMeetingDailyUsage() {
        this.meetingDailyUsedHours = 0;
    }

    public void resetMeetingWeeklyUsage() {
        this.meetingWeeklyUsedHours = 0;
    }

    public void applyMeetingUsageDelta(int deltaHours) {
        meetingDailyUsedHours = Math.max(0, meetingDailyUsedHours + deltaHours);
        meetingWeeklyUsedHours = Math.max(0, meetingWeeklyUsedHours + deltaHours);
    }


    public void updateUsageDate(LocalDate date) {
        this.usageDate = date;
    }

    public void updateUsageWeekStart(LocalDate weekStart) {
        this.usageWeekStart = weekStart;
    }

    public void applySeatUsageDelta(int hours) {
        seatDailyUsedHours += hours;
    }
}
