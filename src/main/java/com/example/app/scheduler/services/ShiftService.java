package com.example.app.scheduler.services;

import com.example.app.scheduler.ShiftTimes;
import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.services.StaffService;
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
    public record ShiftRow(LocalTime startTime, String label, List<ShiftCell> cells) 
    {
    }

    // StaffId is null when the slot is unassigned
    public record ShiftCell(DayOfWeek day, Integer staffId) 
    {
    }

    private final ShiftSlotRepository shiftSlotRepository;

    private final StaffService staffService;

    public ShiftService(ShiftSlotRepository shiftSlotRepository, StaffService staffService)
    {
        this.shiftSlotRepository = shiftSlotRepository;
        this.staffService = staffService;
    }

    /*  Assigns a staff to a shift slot
     *
     *  Creates the slot if it doesn't exist yet, otherwise
     *  updates it. A null staffId unassigns the slot.
     */
    @Transactional
    public void assignSlot(DayOfWeek day, LocalTime startTime, Integer staffId)
    {
        if (!ShiftTimes.DAYS.contains(day) || !ShiftTimes.START_TIMES.contains(startTime)) 
        {
            throw new IllegalArgumentException("Not a rostered slot: " + day + " " + startTime);
        }

        Staff staff = staffId == null ? null : staffService.findStaffById(staffId);

        ShiftSlot slot = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                .orElseGet(() -> new ShiftSlot().setDayOfWeek(day).setStartTime(startTime));

        shiftSlotRepository.save(slot.setStaff(staff));
    }

    /*  Builds the weekly roster grid
     *
     *  One row per shift start time, one cell per weekday,
     *  in the same layout as the old excel sheet.
     */
    public List<ShiftRow> getWeekGrid()
    {
        List<ShiftRow> rows = new ArrayList<>();

        for (LocalTime startTime : ShiftTimes.START_TIMES)
        {
            List<ShiftCell> cells = new ArrayList<>();

            for (DayOfWeek day : ShiftTimes.DAYS) 
            {
                Integer staffId = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                        .map(ShiftSlot::getStaff)
                        .map(Staff::getId)
                        .orElse(null);

                cells.add(new ShiftCell(day, staffId));
            }

            rows.add(new ShiftRow(startTime, ShiftTimes.label(startTime), cells));
        }

        return rows;
    }
}