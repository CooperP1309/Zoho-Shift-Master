package com.example.app.scheduler.services;

import com.example.app.scheduler.configs.ShiftTimes;
import com.example.app.scheduler.entities.ShiftCover;
import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftCoverRepository;
import com.example.app.scheduler.repositories.ShiftSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ShiftCoverService
{
    // rosteredName is null when the covered slot is unassigned
    public record CoverView(Integer id, DayOfWeek day, LocalTime startTime, String label,
                            String staffName, String rosteredName)
    {
    }

    private final ShiftCoverRepository shiftCoverRepository;

    private final ShiftSlotRepository shiftSlotRepository;

    private final StaffService staffService;

    public ShiftCoverService(ShiftCoverRepository shiftCoverRepository,
                             ShiftSlotRepository shiftSlotRepository,
                             StaffService staffService)
    {
        this.shiftCoverRepository = shiftCoverRepository;
        this.shiftSlotRepository = shiftSlotRepository;
        this.staffService = staffService;
    }

    /*  Queues a staff member to cover a slot
     *
     *  Replaces any cover already queued for that slot.
     */
    @Transactional
    public void queueCover(DayOfWeek day, LocalTime startTime, Integer staffId)
    {
        if (day == null || startTime == null
                || !ShiftTimes.DAYS.contains(day) || !ShiftTimes.START_TIMES.contains(startTime))
        {
            throw new IllegalArgumentException("Not a rostered slot: " + day + " " + startTime);
        }

        if (staffId == null)
        {
            throw new IllegalArgumentException("A staff member must be selected");
        }

        Staff staff = staffService.findStaffById(staffId);

        ShiftCover cover = shiftCoverRepository.findByDayOfWeekAndStartTime(day, startTime)
                .orElseGet(() -> new ShiftCover().setDayOfWeek(day).setStartTime(startTime));

        shiftCoverRepository.save(cover.setStaff(staff));
    }

    @Transactional
    public void cancelCover(Integer coverId)
    {
        ShiftCover cover = shiftCoverRepository.findById(coverId)
                .orElseThrow(() -> new NoSuchElementException("No queued cover with ID: " + coverId));

        shiftCoverRepository.delete(cover);
    }

    /*  Removes and returns the cover queued for a slot, if any
     *
     *  Called by the scheduler when the slot fires, so each
     *  cover is only ever used once.
     */
    @Transactional
    public Optional<Staff> takeCover(DayOfWeek day, LocalTime startTime)
    {
        Optional<ShiftCover> cover = shiftCoverRepository.findByDayOfWeekAndStartTime(day, startTime);

        cover.ifPresent(shiftCoverRepository::delete);

        return cover.map(ShiftCover::getStaff);
    }

    /*  All queued covers, in roster order (day, then start time) */
    public List<CoverView> allCovers()
    {
        List<CoverView> covers = new ArrayList<>();

        for (ShiftCover cover : shiftCoverRepository.findAll())
        {
            String rosteredName = shiftSlotRepository
                    .findByDayOfWeekAndStartTime(cover.getDayOfWeek(), cover.getStartTime())
                    .map(ShiftSlot::getStaff)
                    .map(Staff::getDisplayName)
                    .orElse(null);

            covers.add(new CoverView(cover.getId(), cover.getDayOfWeek(), cover.getStartTime(),
                    ShiftTimes.label(cover.getStartTime()), cover.getStaff().getDisplayName(), rosteredName));
        }

        covers.sort(Comparator.comparing(CoverView::day).thenComparing(CoverView::startTime));

        return covers;
    }
}
