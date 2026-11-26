package com.example.app.scheduler.controllers;

import com.example.app.scheduler.configs.ShiftTimes;
import com.example.app.scheduler.services.ShiftCoverService;
import com.example.app.scheduler.services.ShiftService;
import com.example.app.scheduler.services.ShiftService.SlotKey;
import com.example.app.scheduler.services.StaffService;

import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RequestMapping("/shift/")
@Controller
@Slf4j
public class ShiftController 
{
    private final StaffService staffService;

    private final ShiftService shiftService;

    private final ShiftCoverService shiftCoverService;

    // a roster start time with its display label, for the cover form's slot dropdown
    public record SlotOption(LocalTime startTime, String label)
    {
    }

    private static final List<SlotOption> SLOT_OPTIONS = ShiftTimes.START_TIMES.stream()
            .map(startTime -> new SlotOption(startTime, ShiftTimes.label(startTime)))
            .toList();

    public ShiftController(StaffService staffService, ShiftService shiftService,
                           ShiftCoverService shiftCoverService)
    {
        this.staffService = staffService;
        this.shiftService = shiftService;
        this.shiftCoverService = shiftCoverService;
    }

    @GetMapping("dashboard")
    /*  Returns the shift schedule page for the system
     *
     *  The shift page is where users can add/remove names from the
     *  notification system. 
     */
    public String showShiftPage(Model model) 
    {
        model.addAttribute("staff", staffService.allStaff());
        model.addAttribute("days", ShiftTimes.DAYS);
        model.addAttribute("rows", shiftService.getWeekGrid());
        model.addAttribute("startTimes", SLOT_OPTIONS);
        model.addAttribute("covers", shiftCoverService.allCovers());

        return "shift-page";
    }

    @PostMapping("cover")
    /*  Queues a one-off cover for a roster slot
     *
     *  The next time the scheduler fires for this day/slot it
     *  notifies the covering staff instead of the rostered one.
     */
    public String queueCover(@RequestParam(required = false) String staffId,
                             @RequestParam(required = false) String day,
                             @RequestParam(required = false) String startTime,
                             RedirectAttributes redirectAttributes)
    {
        try
        {
            if (staffId == null || staffId.isBlank() || day == null || day.isBlank()
                    || startTime == null || startTime.isBlank())
            {
                throw new IllegalArgumentException("staff, day and slot are all required");
            }

            shiftCoverService.queueCover(DayOfWeek.valueOf(day), LocalTime.parse(startTime), Integer.valueOf(staffId));
            redirectAttributes.addFlashAttribute("message", "Cover queued.");
        }
        catch (IllegalArgumentException | NoSuchElementException | DateTimeParseException e)
        {
            log.warn("Rejected cover: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Cover not queued: " + e.getMessage());
        }

        return "redirect:/shift/dashboard";
    }

    @PostMapping("cover/{id}/cancel")
    /*  Removes a queued cover before it is used */
    public String cancelCover(@PathVariable Integer id, RedirectAttributes redirectAttributes)
    {
        try
        {
            shiftCoverService.cancelCover(id);
            redirectAttributes.addFlashAttribute("message", "Cover cancelled.");
        }
        catch (NoSuchElementException e)
        {
            log.warn("Rejected cover cancel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Cover not cancelled: " + e.getMessage());
        }

        return "redirect:/shift/dashboard";
    }

    @PostMapping("save")
    /*  Saves the whole roster grid from the dashboard form
     *
     *  Every select in the grid is named "slot-{DAY}-{HH:mm}"
     *  with the staff ID as its value (blank = unassigned). Any
     *  other params (e.g. _csrf) are ignored. Redirects back to
     *  the dashboard with a flash message either way.
     */
    public String saveShifts(@RequestParam Map<String, String> form, RedirectAttributes redirectAttributes)
    {
        try
        {
            shiftService.saveWeekGrid(parseSlots(form));
            redirectAttributes.addFlashAttribute("message", "Roster saved.");
        }
        catch (IllegalArgumentException | NoSuchElementException e)
        {
            log.warn("Rejected roster save: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Roster not saved: " + e.getMessage());
        }

        return "redirect:/shift/dashboard";
    }

    private static final String SLOT_PREFIX = "slot-";

    /*  Turns "slot-MONDAY-07:00" -> "3" form entries into typed slot keys
     *
     *  Throws IllegalArgumentException on any malformed name or value
     *  so a tampered request can't partially apply.
     */
    private Map<SlotKey, Integer> parseSlots(Map<String, String> form)
    {
        Map<SlotKey, Integer> assignments = new HashMap<>();

        for (Map.Entry<String, String> entry : form.entrySet())
        {
            if (!entry.getKey().startsWith(SLOT_PREFIX))
            {
                continue;
            }

            String[] parts = entry.getKey().substring(SLOT_PREFIX.length()).split("-", 2);

            if (parts.length != 2)
            {
                throw new IllegalArgumentException("Malformed slot field: " + entry.getKey());
            }

            try
            {
                DayOfWeek day = DayOfWeek.valueOf(parts[0]);
                LocalTime startTime = LocalTime.parse(parts[1]);
                String value = entry.getValue();
                Integer staffId = (value == null || value.isBlank()) ? null : Integer.valueOf(value);

                assignments.put(new SlotKey(day, startTime), staffId);
            }
            catch (DateTimeParseException e)
            {
                throw new IllegalArgumentException("Malformed slot field: " + entry.getKey());
            }
        }

        return assignments;
    }
}
