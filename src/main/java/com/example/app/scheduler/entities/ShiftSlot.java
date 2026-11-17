package com.example.app.scheduler.entities;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/*  One cell of the weekly roster
 *
 *  A (day, start time) pair and who is on it. A null
 *  staff means the slot is unassigned.
 */
@Table(name = "shift_slots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"day_of_week", "start_time"}))
@Entity
public class ShiftSlot
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    public Integer getId()
    {
        return id;
    }

    public DayOfWeek getDayOfWeek()
    {
        return dayOfWeek;
    }

    public ShiftSlot setDayOfWeek(DayOfWeek dayOfWeek)
    {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public LocalTime getStartTime()
    {
        return startTime;
    }

    public ShiftSlot setStartTime(LocalTime startTime)
    {
        this.startTime = startTime;
        return this;
    }

    public Staff getStaff()
    {
        return staff;
    }

    public ShiftSlot setStaff(Staff staff)
    {
        this.staff = staff;
        return this;
    }
}