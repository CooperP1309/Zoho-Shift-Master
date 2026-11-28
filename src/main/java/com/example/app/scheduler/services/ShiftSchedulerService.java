package com.example.app.scheduler.services;

/* spring specific modules */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/* local modules */
import com.example.app.scheduler.configs.ShiftTimes;
import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftSlotRepository;
import com.example.app.scheduler.services.ZohoCliqService;

/* java modules */
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShiftSchedulerService
{
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(5);

    private final ShiftSlotRepository shiftSlotRepository;

    private final ShiftCoverService shiftCoverService;

    private final ZohoCliqService zohoCliqService;

    private final Clock clock;

    private final Duration retryDelay;

    @Autowired
    public ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository, ShiftCoverService shiftCoverService,
                                 ZohoCliqService zohoCliqService, Clock clock)
    {
        this(shiftSlotRepository, shiftCoverService, zohoCliqService, clock, DEFAULT_RETRY_DELAY);
    }

    // allows tests to skip the retry delay
    ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository, ShiftCoverService shiftCoverService,
                          ZohoCliqService zohoCliqService, Clock clock, Duration retryDelay)
    {
        this.shiftSlotRepository = shiftSlotRepository;
        this.shiftCoverService = shiftCoverService;
        this.zohoCliqService = zohoCliqService;
        this.clock = clock;
        this.retryDelay = retryDelay;
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
        Optional<String> message = resolveCurrentShiftMessage();

        if (message.isEmpty()) {
            log.warn("No station master assigned for the shift at {}", ZonedDateTime.now(clock));
            return;
        }

        try
        {
            log.info("Sending shift announcement to Cliq: {}", message.get());
            postMessageWithRetry(message.get());
        }
        catch (RestClientException | ResponseStatusException e)
        {
            // logged rather than thrown so the admin "trigger now" button doesn't error page
            log.error("Failed to send shift announcement to Cliq: {}", e.getMessage(), e);
        }
    }

    /*  Posts a message, retrying once on connection errors
     *
     *  Only I/O failures (e.g. a connection reset by the network)
     *  are retried, since the second attempt opens a fresh
     *  connection. HTTP error responses from Zoho are not retried,
     *  as the same request would just be rejected again.
     */
    private void postMessageWithRetry(String message)
    {
        try
        {
            zohoCliqService.postMessage(message);
        }
        catch (ResourceAccessException e)
        {
            log.warn("Connection error sending shift announcement to Cliq ({}), retrying in {}s",
                    e.getMessage(), retryDelay.toSeconds());

            try
            {
                Thread.sleep(retryDelay);
            }
            catch (InterruptedException interrupted)
            {
                Thread.currentThread().interrupt();
                throw e;
            }

            zohoCliqService.postMessage(message);
            log.info("Retry succeeded, shift announcement sent to Cliq");
        }
    }

    /*  Resolves the message for the current shift
     *
     *  Looks up the roster slot containing the current time. If a
     *  cover is queued for that slot, the covering staff is
     *  mentioned instead and the cover is CONSUMED (deleted), so
     *  only call this when the announcement is actually going out.
     *  Otherwise mentions the rostered staff. Empty if outside
     *  rostered hours, or the slot is unassigned with no cover.
     */
    public Optional<String> resolveCurrentShiftMessage()
    {
        ZonedDateTime now = ZonedDateTime.now(clock);
        Optional<LocalTime> slotStart = ShiftTimes.slotStartFor(now.toLocalTime());

        if (slotStart.isEmpty())
        {
            return Optional.empty();
        }

        Optional<Staff> rostered = shiftSlotRepository
                .findByDayOfWeekAndStartTime(now.getDayOfWeek(), slotStart.get())
                .map(ShiftSlot::getStaff);

        Optional<Staff> cover = shiftCoverService.takeCover(now.getDayOfWeek(), slotStart.get());

        if (cover.isPresent())
        {
            String coveringFor = rostered
                    .map(staff -> " (covering for " + staff.getDisplayName() + ")")
                    .orElse("");

            log.info("Using queued cover {} for the {} {} shift", cover.get().getDisplayName(),
                    now.getDayOfWeek(), slotStart.get());

            // someone queued a cover for themselves; call it out
            String remark = "";

            if (rostered.isPresent() && rostered.get().getId().equals(cover.get().getId()))
            {
                List<String> remarks = List.of("What a goose!", 
                                                "How ridiculous!", 
                                                "They can't be serious!",
                                                "Grow up mate!",
                                                "*crickets*",
                                                "Why do I even bother!",
                                                "How did you pass your birth certificate?!");

                remark = " " + remarks.get(ThreadLocalRandom.current().nextInt(remarks.size()));
            }

            return Optional.of("Hi {@" + cover.get().getCliqUserId()
                    + "}, time to start your station master shift" + coveringFor + "!" + remark);
        }

        return rostered.map(stationMaster ->
                "Hi {@" + stationMaster.getCliqUserId() + "}, time to start your station master shift!");
    }
}