package com.example.app.scheduler.services;

/* spring specific modules */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/* local modules */
import com.example.app.scheduler.ShiftTimes;
import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.repositories.ShiftSlotRepository;
//import com.example.app.zoho_client.services.ZohoClientService;

/* java modules */
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShiftSchedulerService
{
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(5);

    private final ShiftSlotRepository shiftSlotRepository;

    //private final ZohoClientService zohoClientService;

    private final Clock clock;

    private final Duration retryDelay;

    @Autowired
    public ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository,// ZohoClientService zohoClientService,
                                 Clock clock)
    {
        this(shiftSlotRepository, /*zohoClientService,*/ clock, DEFAULT_RETRY_DELAY);
    }

    // allows tests to skip the retry delay
    ShiftSchedulerService(ShiftSlotRepository shiftSlotRepository, //ZohoClientService zohoClientService,
                          Clock clock, Duration retryDelay)
    {
        this.shiftSlotRepository = shiftSlotRepository;
        //this.zohoClientService = zohoClientService;
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
        Optional<String> message = buildCurrentShiftMessage();

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
            //zohoClientService.postMessage(message);
            log.info("TEST POSTING OF MESSAGE");
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

            zohoClientService.postMessage(message);
            log.info("Retry succeeded, shift announcement sent to Cliq");
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