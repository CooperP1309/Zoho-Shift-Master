package com.example.app.scheduler.services;

import com.example.app.scheduler.entities.ShiftCover;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftCoverRepository;
import com.example.app.scheduler.repositories.ShiftSlotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftCoverServiceTest
{
    private static final LocalTime NINE = LocalTime.of(9, 0);

    @Mock
    private ShiftCoverRepository shiftCoverRepository;

    @Mock
    private ShiftSlotRepository shiftSlotRepository;

    @Mock
    private StaffService staffService;

    private ShiftCoverService service;

    private final Staff alice = new Staff().setId(2).setDisplayName("Alice").setCliqUserId("alice-cliq");

    @BeforeEach
    void setUp()
    {
        service = new ShiftCoverService(shiftCoverRepository, shiftSlotRepository, staffService);
    }

    @Test
    void queueCover_rejectsWeekend()
    {
        assertThrows(IllegalArgumentException.class, () -> service.queueCover(DayOfWeek.SATURDAY, NINE, 2));
        verify(shiftCoverRepository, never()).save(any());
    }

    @Test
    void queueCover_rejectsNonRosteredStartTime()
    {
        assertThrows(IllegalArgumentException.class, () -> service.queueCover(DayOfWeek.MONDAY, LocalTime.of(8, 0), 2));
        verify(shiftCoverRepository, never()).save(any());
    }

    @Test
    void queueCover_rejectsUnknownStaff()
    {
        when(staffService.findStaffById(99)).thenThrow(new NoSuchElementException("No staff found with ID: 99"));

        assertThrows(NoSuchElementException.class, () -> service.queueCover(DayOfWeek.MONDAY, NINE, 99));
        verify(shiftCoverRepository, never()).save(any());
    }

    @Test
    void queueCover_createsNewCover()
    {
        when(staffService.findStaffById(2)).thenReturn(alice);
        when(shiftCoverRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.empty());

        service.queueCover(DayOfWeek.MONDAY, NINE, 2);

        ArgumentCaptor<ShiftCover> saved = ArgumentCaptor.forClass(ShiftCover.class);
        verify(shiftCoverRepository).save(saved.capture());
        assertEquals(DayOfWeek.MONDAY, saved.getValue().getDayOfWeek());
        assertEquals(NINE, saved.getValue().getStartTime());
        assertSame(alice, saved.getValue().getStaff());
    }

    @Test
    void queueCover_replacesExistingCoverForSlot()
    {
        Staff bob = new Staff().setId(1).setDisplayName("Bob").setCliqUserId("bob-cliq");
        ShiftCover existing = new ShiftCover().setId(5).setDayOfWeek(DayOfWeek.MONDAY).setStartTime(NINE).setStaff(bob);

        when(staffService.findStaffById(2)).thenReturn(alice);
        when(shiftCoverRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.of(existing));

        service.queueCover(DayOfWeek.MONDAY, NINE, 2);

        verify(shiftCoverRepository).save(existing);
        assertSame(alice, existing.getStaff());
    }

    @Test
    void takeCover_returnsAndDeletesQueuedCover()
    {
        ShiftCover queued = new ShiftCover().setId(5).setDayOfWeek(DayOfWeek.MONDAY).setStartTime(NINE).setStaff(alice);
        when(shiftCoverRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.of(queued));

        assertEquals(Optional.of(alice), service.takeCover(DayOfWeek.MONDAY, NINE));
        verify(shiftCoverRepository).delete(queued);
    }

    @Test
    void takeCover_emptyWhenNothingQueued()
    {
        when(shiftCoverRepository.findByDayOfWeekAndStartTime(DayOfWeek.MONDAY, NINE)).thenReturn(Optional.empty());

        assertTrue(service.takeCover(DayOfWeek.MONDAY, NINE).isEmpty());
        verify(shiftCoverRepository, never()).delete(any());
    }
}
