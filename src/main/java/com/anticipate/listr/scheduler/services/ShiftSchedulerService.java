package com.anticipate.listr.scheduler.services;

/* spring specific modules */
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/* local modules */
import com.anticipate.listr.scheduler.ShiftTimes;
import com.anticipate.listr.scheduler.entities.ShiftSlot;
import com.anticipate.listr.scheduler.repositories.ShiftSlotRepository;
import com.anticipate.listr.zoho_client.services.ZohoClientService;

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

    private final ZohoClientService zohoClientService;

    private final Clock clock;

    public ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository, ZohoClientService zohoClientService,
                                 Clock clock)
    {
        this.shiftSlotRepository = shiftSlotRepository;
        this.zohoClientService = zohoClientService;
        this.clock = clock;
    }

    /*  Announces the station master at each shift change
     *
     *  Spring calls this whenever scheduler.cron matches, in the
     *  scheduler.zone timezone (see application.properties), and
     *  posts the mention message to the configured Cliq chat.
     */
    @Scheduled(cron = "${scheduler.cron}", zone = "${scheduler.zone}")
    public void announceCurrentShift()
    {
        Optional<String> message = buildCurrentShiftMessage();

        if (message.isEmpty()) {
            log.warn("No station master assigned for the shift at {}", ZonedDateTime.now(clock));
            return;
        }

        try
        {
            log.info("Sending shift announcement to Cliq: {}", message.get());
            zohoClientService.postMessage(message.get());
        }
        catch (RestClientException | ResponseStatusException e)
        {
            // logged rather than thrown so the admin "trigger now" button doesn't error page
            log.error("Failed to send shift announcement to Cliq: {}", e.getMessage(), e);
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
