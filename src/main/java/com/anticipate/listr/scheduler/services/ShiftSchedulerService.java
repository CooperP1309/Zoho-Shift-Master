package com.anticipate.listr.scheduler.services;

/* spring specific modules */
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/* local modules */
import com.anticipate.listr.scheduler.ShiftTimes;
import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.repositories.ShiftSlotRepository;

/* java modules */
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShiftSchedulerService
{
    private final ShiftSlotRepository shiftSlotRepository;

    private final Clock clock;

    public ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository, Clock clock)
    {
        this.shiftSlotRepository = shiftSlotRepository;
        this.clock = clock;
    }

    /*  Announces the station master at each shift change
     *
     *  Spring calls this whenever scheduler.cron matches, in the
     *  scheduler.zone timezone (see application.properties).
     *  DRY RUN: only logs what would be sent to Cliq.
     */
    @Scheduled(cron = "${scheduler.cron}", zone = "${scheduler.zone}")
    public void announceCurrentShift()
    {
        Optional<String> message = buildCurrentShiftMessage();

        if (message.isPresent()) {
            log.info("[DRY RUN] Would send to Cliq: {}", message.get());
        } else {
            log.warn("No station master assigned for the shift at {}", ZonedDateTime.now(clock));
        }
    }

    /*  Builds the message for the current shift
     *
     *  Looks up the roster slot containing the current time and
     *  returns the mention message for whoever is on it. Empty if
     *  outside rostered hours or the slot is unassigned.
     */
    public Optional<String> buildCurrentShiftMessage()
    {
        ZonedDateTime now = ZonedDateTime.now(clock);

        return ShiftTimes.slotStartFor(now.toLocalTime())
                .flatMap(startTime -> shiftSlotRepository.findByDayOfWeekAndStartTime(now.getDayOfWeek(), startTime))
                .map(ShiftSlot::getStationMaster)
                .map(stationMaster -> "Hi {@" + stationMaster.getCliqUserId() + "}, time to start your station master shift!");
    }
}
