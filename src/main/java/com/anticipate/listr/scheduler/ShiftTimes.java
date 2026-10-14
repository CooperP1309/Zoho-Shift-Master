package com.anticipate.listr.scheduler;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/*  The fixed shape of the station master roster
 *
 *  Shifts are 2 hours long, starting at 7am and ending at 7pm,
 *  Monday to Friday. Keep this in sync with scheduler.cron in
 *  application.properties (CronScheduleTest checks the two agree).
 */
public final class ShiftTimes
{
    public static final List<DayOfWeek> DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    public static final List<LocalTime> START_TIMES = List.of(
            LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(11, 0),
            LocalTime.of(13, 0), LocalTime.of(15, 0), LocalTime.of(17, 0));

    public static final Duration SHIFT_LENGTH = Duration.ofHours(2);

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private ShiftTimes()
    {
    }

    /*  Finds the shift a time falls in
     *
     *  Returns the start time of the shift containing the given
     *  time (e.g. 15:00:03 and 16:30 both map to 15:00), or empty
     *  if the time is outside rostered hours.
     */
    public static Optional<LocalTime> slotStartFor(LocalTime time)
    {
        return START_TIMES.stream()
                .filter(start -> !time.isBefore(start) && time.isBefore(start.plus(SHIFT_LENGTH)))
                .findFirst();
    }

    // e.g. "7:00 AM - 9:00 AM", matching the old excel sheet
    public static String label(LocalTime startTime)
    {
        return LABEL_FORMAT.format(startTime) + " - " + LABEL_FORMAT.format(startTime.plus(SHIFT_LENGTH));
    }
}
