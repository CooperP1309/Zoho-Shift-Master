package com.example.app.scheduler.repositories;

import com.example.app.scheduler.entities.ShiftCover;
import com.example.app.scheduler.entities.Staff;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftCoverRepository extends CrudRepository<ShiftCover, Integer>
{
    Optional<ShiftCover> findByDayOfWeekAndStartTime(DayOfWeek dayOfWeek, LocalTime startTime);
    List<ShiftCover> findAllByStaff(Staff staff);
}
