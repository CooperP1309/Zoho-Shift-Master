package com.example.app.scheduler.services;

import com.example.app.scheduler.ShiftTimes;
import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ShiftService
{
    public record ShiftRow(LocalTime startTime, String label, List<RosterCell> cells) 
    {
    }

    // StaffId is null when the slot is unassigned
    public record ShiftCell(DayOfWeek day, Integer staffId) 
    {
    }

    private final ShiftSlotRepository shiftSlotRepository;

    public ShiftService(ShiftSlotRepository shiftSlotRepository)
    {
        this.shiftSlotRepository = shiftSlotRepository;
    }

    /*  Assigns a staff to a shift slot
     *
     *  Creates the slot if it doesn't exist yet, otherwise
     *  updates it. A null stationMasterId unassigns the slot.
     */
    @Transactional
    public void assignSlot(DayOfWeek day, LocalTime startTime, Integer stationMasterId)
    {
        if (!ShiftTimes.DAYS.contains(day) || !ShiftTimes.START_TIMES.contains(startTime)) 
        {
            throw new IllegalArgumentException("Not a rostered slot: " + day + " " + startTime);
        }

        Staff staff = staffId == null ? null : findStationMaster(staffId);

        ShiftSlot slot = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                .orElseGet(() -> new ShiftSlot().setDayOfWeek(day).setStartTime(startTime));

        shiftSlotRepository.save(slot.setStationMaster(staff));
    }

    /*  Builds the weekly roster grid
     *
     *  One row per shift start time, one cell per weekday,
     *  in the same layout as the old excel sheet.
     */
    public List<RosterRow> getWeekGrid()
    {
        List<RosterRow> rows = new ArrayList<>();

        for (LocalTime startTime : ShiftTimes.START_TIMES)
        {
            List<RosterCell> cells = new ArrayList<>();

            for (DayOfWeek day : ShiftTimes.DAYS) 
            {
                Integer staffId = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                        .map(ShiftSlot::getStaff)
                        .map(StationMaster::getId)
                        .orElse(null);

                cells.add(new RosterCell(day, staffId));
            }

            rows.add(new RosterRow(startTime, ShiftTimes.label(startTime), cells));
        }

        return rows;
    }
}