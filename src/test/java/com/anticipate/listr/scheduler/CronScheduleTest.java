package com.anticipate.listr.scheduler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*  Checks the real scheduler.cron / scheduler.zone from
 *  application.properties fire exactly at the roster's shift
 *  changes, so the cron and ShiftTimes can't drift apart.
 */
class CronScheduleTest
{
    private static CronExpression cron;

    private static ZoneId zone;

    @BeforeAll
    static void loadSchedulerProperties() throws IOException
    {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

        cron = CronExpression.parse(properties.getProperty("scheduler.cron"));
        zone = ZoneId.of(properties.getProperty("scheduler.zone"));
    }

    private List<ZonedDateTime> firesBetween(ZonedDateTime from, ZonedDateTime to)
    {
        List<ZonedDateTime> fires = new ArrayList<>();

        for (ZonedDateTime next = cron.next(from); next != null && next.isBefore(to); next = cron.next(next)) {
            fires.add(next);
        }

        return fires;
    }

    @Test
    void firesAtEveryShiftChangeOnWeekdaysOnly()
    {
        // Sunday 11 October 2026 to Sunday 18 October 2026
        ZonedDateTime weekStart = ZonedDateTime.of(LocalDateTime.of(2026, 10, 11, 0, 0), zone);

        List<ZonedDateTime> fires = firesBetween(weekStart, weekStart.plusWeeks(1));

        List<ZonedDateTime> expected = new ArrayList<>();
        for (DayOfWeek day : ShiftTimes.DAYS) {
            for (LocalTime startTime : ShiftTimes.START_TIMES) {
                expected.add(ZonedDateTime.of(
                        weekStart.toLocalDate().plusDays(day.getValue()), startTime, zone));
            }
        }

        assertEquals(expected, fires);
    }

    @Test
    void keepsLocalShiftTimesAcrossDaylightSavingChange()
    {
        // Sydney daylight saving starts Sunday 4 October 2026
        ZonedDateTime fridayLastShift = ZonedDateTime.of(LocalDateTime.of(2026, 10, 2, 17, 0), zone);

        ZonedDateTime mondayFirstShift = cron.next(fridayLastShift);

        assertEquals(ZoneOffset.ofHours(10), fridayLastShift.getOffset());
        assertEquals(LocalDateTime.of(2026, 10, 5, 7, 0), mondayFirstShift.toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(11), mondayFirstShift.getOffset());
    }
}
