package com.example.app.scheduler.services;

import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftSlotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftSchedulerServiceTest
{
    private static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

    // Monday 2026-11-30, 09:00 Sydney -> the MONDAY 09:00 slot
    private static final Clock MONDAY_9AM = Clock.fixed(
            ZonedDateTime.of(2026, 11, 30, 9, 0, 0, 0, ZONE).toInstant(), ZONE);

    private static final LocalTime NINE = LocalTime.of(9, 0);

    @Mock
    private ShiftSlotRepository shiftSlotRepository;

    @Mock
    private ShiftCoverService shiftCoverService;

    private ShiftSchedulerService service;

    private final Staff rostered = new Staff().setId(1).setDisplayName("Bob").setCliqUserId("bob-cliq");
    private final Staff cover = new Staff().setId(2).setDisplayName("Alice").setCliqUserId("alice-cliq");

    @BeforeEach
    void setUp()
    {
        service = new ShiftSchedulerService(shiftSlotRepository, shiftCoverService, MONDAY_9AM, Duration.ZERO);
    }

    private void rosterBob()
    {
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE))
                .thenReturn(Optional.of(new ShiftSlot().setDayOfWeek(DayOfWeek.MONDAY).setStartTime(NINE).setStaff(rostered)));
    }

    @Test
    void queuedCover_isNotifiedInsteadOfRosteredStaff_andConsumed()
    {
        rosterBob();
        when(shiftCoverService.takeCover(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.of(cover));

        Optional<String> message = service.resolveCurrentShiftMessage();

        assertEquals(Optional.of("Hi {@alice-cliq}, time to start your station master shift (covering for Bob)!"), message);
        verify(shiftCoverService).takeCover(DayOfWeek.MONDAY, NINE);
    }

    @Test
    void noCover_notifiesRosteredStaff()
    {
        rosterBob();
        when(shiftCoverService.takeCover(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.empty());

        Optional<String> message = service.resolveCurrentShiftMessage();

        assertEquals(Optional.of("Hi {@bob-cliq}, time to start your station master shift!"), message);
    }

    @Test
    void coverOnUnassignedSlot_notifiesCover()
    {
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.empty());
        when(shiftCoverService.takeCover(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.of(cover));

        Optional<String> message = service.resolveCurrentShiftMessage();

        assertEquals(Optional.of("Hi {@alice-cliq}, time to start your station master shift!"), message);
    }

    @Test
    void outsideRosteredHours_returnsEmpty_andDoesNotTouchCovers()
    {
        Clock evening = Clock.fixed(ZonedDateTime.of(2026, 11, 30, 20, 0, 0, 0, ZONE).toInstant(), ZONE);
        service = new ShiftSchedulerService(shiftSlotRepository, shiftCoverService, evening, Duration.ZERO);

        assertTrue(service.resolveCurrentShiftMessage().isEmpty());
        verifyNoInteractions(shiftCoverService);
    }
}
