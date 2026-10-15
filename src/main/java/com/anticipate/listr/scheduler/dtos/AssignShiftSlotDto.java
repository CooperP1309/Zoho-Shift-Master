package com.anticipate.listr.scheduler.dtos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class AssignShiftSlotDto
{

    private DayOfWeek dayOfWeek;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    // blank in the form (null here) unassigns the slot
    private Integer stationMasterId;

    public DayOfWeek getDayOfWeek()
    {
        return dayOfWeek;
    }

    public AssignShiftSlotDto setDayOfWeek(DayOfWeek dayOfWeek)
    {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public LocalTime getStartTime()
    {
        return startTime;
    }

    public AssignShiftSlotDto setStartTime(LocalTime startTime)
    {
        this.startTime = startTime;
        return this;
    }

    public Integer getStationMasterId()
    {
        return stationMasterId;
    }

    public AssignShiftSlotDto setStationMasterId(Integer stationMasterId)
    {
        this.stationMasterId = stationMasterId;
        return this;
    }

    public AssignShiftSlotDto()
    {

    }
}
