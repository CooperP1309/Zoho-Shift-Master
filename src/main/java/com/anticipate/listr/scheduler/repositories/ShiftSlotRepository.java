package com.anticipate.listr.scheduler.repositories;

import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.entities.StationMaster;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftSlotRepository extends CrudRepository<ShiftSlot, Integer>
{
    Optional<ShiftSlot> findByDayOfWeekAndStartTime(DayOfWeek dayOfWeek, LocalTime startTime);
    List<ShiftSlot> findAllByStationMaster(StationMaster stationMaster);
}
