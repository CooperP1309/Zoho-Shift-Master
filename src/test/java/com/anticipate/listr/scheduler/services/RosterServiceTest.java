package com.anticipate.listr.scheduler.services;

import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.entities.StationMaster;
import com.anticipate.listr.scheduler.repositories.ShiftSlotRepository;
import com.anticipate.listr.scheduler.repositories.StationMasterRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest
{
    private static final LocalTime NINE_AM = LocalTime.of(9, 0);

    @Mock
    private StationMasterRepository stationMasterRepository;

    @Mock
    private ShiftSlotRepository shiftSlotRepository;

    private RosterService rosterService;

    private StationMaster cooper;

    @BeforeEach
    void setUp()
    {
        rosterService = new RosterService(stationMasterRepository, shiftSlotRepository);
        cooper = new StationMaster().setId(1).setDisplayName("Cooper").setCliqUserId("12345678");
    }

    @Test
    void assignSlot_updatesExistingSlotInsteadOfCreatingDuplicate()
    {
        ShiftSlot existing = new ShiftSlot().setDayOfWeek(DayOfWeek.TUESDAY).setStartTime(NINE_AM);
        when(stationMasterRepository.findById(1)).thenReturn(Optional.of(cooper));
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.TUESDAY, NINE_AM))
                .thenReturn(Optional.of(existing));

        rosterService.assignSlot(DayOfWeek.TUESDAY, NINE_AM, 1);

        ArgumentCaptor<ShiftSlot> saved = ArgumentCaptor.forClass(ShiftSlot.class);
        verify(shiftSlotRepository).save(saved.capture());
        assertSame(existing, saved.getValue());
        assertSame(cooper, saved.getValue().getStationMaster());
    }

    @Test
    void assignSlot_createsSlotWhenMissing()
    {
        when(stationMasterRepository.findById(1)).thenReturn(Optional.of(cooper));
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.TUESDAY, NINE_AM))
                .thenReturn(Optional.empty());

        rosterService.assignSlot(DayOfWeek.TUESDAY, NINE_AM, 1);

        ArgumentCaptor<ShiftSlot> saved = ArgumentCaptor.forClass(ShiftSlot.class);
        verify(shiftSlotRepository).save(saved.capture());
        assertEquals(DayOfWeek.TUESDAY, saved.getValue().getDayOfWeek());
        assertEquals(NINE_AM, saved.getValue().getStartTime());
        assertSame(cooper, saved.getValue().getStationMaster());
    }

    @Test
    void assignSlot_nullStationMasterUnassignsSlot()
    {
        ShiftSlot existing = new ShiftSlot().setDayOfWeek(DayOfWeek.TUESDAY).setStartTime(NINE_AM).setStationMaster(cooper);
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.TUESDAY, NINE_AM))
                .thenReturn(Optional.of(existing));

        rosterService.assignSlot(DayOfWeek.TUESDAY, NINE_AM, null);

        assertNull(existing.getStationMaster());
        verify(shiftSlotRepository).save(existing);
    }

    @Test
    void assignSlot_rejectsNonRosteredSlot()
    {
        assertThrows(IllegalArgumentException.class,
                () -> rosterService.assignSlot(DayOfWeek.SATURDAY, NINE_AM, 1));
        assertThrows(IllegalArgumentException.class,
                () -> rosterService.assignSlot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), 1));

        verify(shiftSlotRepository, never()).save(any());
    }

    @Test
    void assignSlot_throwsForUnknownStationMaster()
    {
        when(stationMasterRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> rosterService.assignSlot(DayOfWeek.TUESDAY, NINE_AM, 99));
    }

    @Test
    void deleteStationMaster_unassignsTheirSlotsFirst()
    {
        ShiftSlot slot = new ShiftSlot().setDayOfWeek(DayOfWeek.MONDAY).setStartTime(NINE_AM).setStationMaster(cooper);
        when(stationMasterRepository.findById(1)).thenReturn(Optional.of(cooper));
        when(shiftSlotRepository.findAllByStationMaster(cooper)).thenReturn(List.of(slot));

        rosterService.deleteStationMaster(1);

        assertNull(slot.getStationMaster());
        verify(shiftSlotRepository).saveAll(List.of(slot));
        verify(stationMasterRepository).delete(cooper);
    }

    @Test
    void getWeekGrid_hasRowPerShiftAndCellPerWeekday()
    {
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(any(), any())).thenReturn(Optional.empty());
        when(shiftSlotRepository.findByDayOfWeekAndStartTime(DayOfWeek.FRIDAY, LocalTime.of(11, 0)))
                .thenReturn(Optional.of(new ShiftSlot().setStationMaster(cooper)));

        List<RosterService.RosterRow> grid = rosterService.getWeekGrid();

        assertEquals(6, grid.size());
        assertEquals("11:00 AM - 1:00 PM", grid.get(2).label());
        assertEquals(5, grid.get(2).cells().size());
        assertEquals(1, grid.get(2).cells().get(4).stationMasterId());
        assertNull(grid.get(2).cells().get(0).stationMasterId());
    }
}
