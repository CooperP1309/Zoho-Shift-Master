package com.anticipate.listr.scheduler.services;

import com.anticipate.listr.scheduler.ShiftTimes;
import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.entities.StationMaster;
import com.anticipate.listr.scheduler.repositories.ShiftSlotRepository;
import com.anticipate.listr.scheduler.repositories.StationMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RosterService
{
    // one row of the admin roster grid, with a cell per weekday
    public record RosterRow(LocalTime startTime, String label, List<RosterCell> cells) {}

    // stationMasterId is null when the slot is unassigned
    public record RosterCell(DayOfWeek day, Integer stationMasterId) {}

    private final StationMasterRepository stationMasterRepository;

    private final ShiftSlotRepository shiftSlotRepository;

    public RosterService(StationMasterRepository stationMasterRepository, ShiftSlotRepository shiftSlotRepository)
    {
        this.stationMasterRepository = stationMasterRepository;
        this.shiftSlotRepository = shiftSlotRepository;
    }

    public List<StationMaster> allStationMasters()
    {
        List<StationMaster> stationMasters = new ArrayList<>();

        stationMasterRepository.findAll().forEach(stationMasters::add);

        return stationMasters;
    }

    public StationMaster addStationMaster(String displayName, String cliqUserId)
    {
        return stationMasterRepository.save(new StationMaster()
                .setDisplayName(displayName.trim())
                .setCliqUserId(cliqUserId.trim()));
    }

    /*  Deletes a station master
     *
     *  Unassigns them from any roster slots first so the
     *  foreign key doesn't block the delete.
     */
    @Transactional
    public void deleteStationMaster(Integer stationMasterId)
    {
        StationMaster stationMaster = findStationMaster(stationMasterId);

        List<ShiftSlot> slots = shiftSlotRepository.findAllByStationMaster(stationMaster);
        slots.forEach(slot -> slot.setStationMaster(null));
        shiftSlotRepository.saveAll(slots);

        stationMasterRepository.delete(stationMaster);
    }

    /*  Assigns a station master to a roster slot
     *
     *  Creates the slot if it doesn't exist yet, otherwise
     *  updates it. A null stationMasterId unassigns the slot.
     */
    @Transactional
    public void assignSlot(DayOfWeek day, LocalTime startTime, Integer stationMasterId)
    {
        if (!ShiftTimes.DAYS.contains(day) || !ShiftTimes.START_TIMES.contains(startTime)) {
            throw new IllegalArgumentException("Not a rostered slot: " + day + " " + startTime);
        }

        StationMaster stationMaster = stationMasterId == null ? null : findStationMaster(stationMasterId);

        ShiftSlot slot = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                .orElseGet(() -> new ShiftSlot().setDayOfWeek(day).setStartTime(startTime));

        shiftSlotRepository.save(slot.setStationMaster(stationMaster));
    }

    /*  Builds the weekly roster grid
     *
     *  One row per shift start time, one cell per weekday,
     *  in the same layout as the old excel sheet.
     */
    public List<RosterRow> getWeekGrid()
    {
        List<RosterRow> rows = new ArrayList<>();

        for (LocalTime startTime : ShiftTimes.START_TIMES) {
            List<RosterCell> cells = new ArrayList<>();

            for (DayOfWeek day : ShiftTimes.DAYS) {
                Integer stationMasterId = shiftSlotRepository.findByDayOfWeekAndStartTime(day, startTime)
                        .map(ShiftSlot::getStationMaster)
                        .map(StationMaster::getId)
                        .orElse(null);

                cells.add(new RosterCell(day, stationMasterId));
            }

            rows.add(new RosterRow(startTime, ShiftTimes.label(startTime), cells));
        }

        return rows;
    }

    private StationMaster findStationMaster(Integer stationMasterId)
    {
        return stationMasterRepository.findById(stationMasterId)
                .orElseThrow(() -> new NoSuchElementException("No station master found with ID: " + stationMasterId));
    }
}
