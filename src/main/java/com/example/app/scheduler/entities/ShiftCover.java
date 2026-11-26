package com.example.app.scheduler.entities;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/*  A one-off cover queued against a roster slot
 *
 *  When the scheduler fires for this (day, start time) it
 *  notifies the covering staff instead of the rostered one,
 *  then deletes the cover. At most one cover per slot.
 */
@Table(name = "shift_covers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"day_of_week", "start_time"}))
@Entity
public class ShiftCover
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    public Integer getId()
    {
        return id;
    }

    public ShiftCover setId(Integer id)
    {
        this.id = id;
        return this;
    }

    public DayOfWeek getDayOfWeek()
    {
        return dayOfWeek;
    }

    public ShiftCover setDayOfWeek(DayOfWeek dayOfWeek)
    {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public LocalTime getStartTime()
    {
        return startTime;
    }

    public ShiftCover setStartTime(LocalTime startTime)
    {
        this.startTime = startTime;
        return this;
    }

    public Staff getStaff()
    {
        return staff;
    }

    public ShiftCover setStaff(Staff staff)
    {
        this.staff = staff;
        return this;
    }
}
