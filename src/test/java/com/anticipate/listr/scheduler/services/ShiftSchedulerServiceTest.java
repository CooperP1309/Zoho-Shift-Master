package com.anticipate.listr.scheduler.services;

import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.entities.StationMaster;
import com.anticipate.listr.scheduler.repositories.ShiftSlotRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftSchedulerServiceTest
{
    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    // Monday 12 October 2026
    private static final LocalDateTime MONDAY_3PM = LocalDateTime.of(2026, 10, 12, 15, 0);

    @Mock
    private ShiftSlotRepository shiftSlotRepository;

    private ShiftSchedulerService serviceAt(LocalDateTime sydneyTime)
    {
        Clock clock = Clock.fixed(ZonedDateTime.of(sydneyTime, SYDNEY).toInstant(), SYDNEY);
        return new ShiftSchedulerService(shiftSlotRepository, clock);
    }

    private ShiftSlot slotWith(StationMaster stationMaster)
    {
        return new ShiftSlot()
                .setDayOfWeek(DayOfWeek.MONDAY)
                .setStartTime(LocalTime.of(15, 0))
                .setStationMaster(stationMaster);
    }

    @Test
    void buildCurrentShiftMessage_mentionsAssignedStationMaster()
    {
        StationMaster cooper = new StationMaster().setDisplayName("Cooper").setCliqUserId("12345678");
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, LocalTime.of(15, 0)))
                .thenReturn(Optional.of(slotWith(cooper)));

        assertEquals(Optional.of("Hi {@12345678}, time to start your station master shift!"),
                serviceAt(MONDAY_3PM).buildCurrentShiftMessage());
    }

    @Test
    void buildCurrentShiftMessage_matchesSlotWhenTriggerFiresLate()
    {
        // the scheduler thread may run a few seconds after the cron instant
        StationMaster cooper = new StationMaster().setDisplayName("Cooper").setCliqUserId("12345678");
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, LocalTime.of(15, 0)))
                .thenReturn(Optional.of(slotWith(cooper)));

        assertTrue(serviceAt(MONDAY_3PM.plusSeconds(3)).buildCurrentShiftMessage().isPresent());
    }

    @Test
    void buildCurrentShiftMessage_mapsMidShiftTimeToSlotStart()
    {
        // manual "trigger now" from the admin portal at 4:30pm is still the 3pm shift
        StationMaster cooper = new StationMaster().setDisplayName("Cooper").setCliqUserId("12345678");
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, LocalTime.of(15, 0)))
                .thenReturn(Optional.of(slotWith(cooper)));

        assertTrue(serviceAt(MONDAY_3PM.plusMinutes(90)).buildCurrentShiftMessage().isPresent());
    }

    @Test
    void buildCurrentShiftMessage_emptyWhenSlotUnassigned()
    {
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, LocalTime.of(15, 0)))
                .thenReturn(Optional.of(slotWith(null)));

        assertTrue(serviceAt(MONDAY_3PM).buildCurrentShiftMessage().isEmpty());
    }

    @Test
    void buildCurrentShiftMessage_emptyWhenSlotMissing()
    {
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, LocalTime.of(15, 0)))
                .thenReturn(Optional.empty());

        assertTrue(serviceAt(MONDAY_3PM).buildCurrentShiftMessage().isEmpty());
    }

    @Test
    void buildCurrentShiftMessage_emptyOutsideRosteredHours()
    {
        assertTrue(serviceAt(MONDAY_3PM.withHour(20)).buildCurrentShiftMessage().isEmpty());

        verify(shiftSlotRepository, never()).findByDayOfWeekAndStartTime(any(), any());
    }
}
