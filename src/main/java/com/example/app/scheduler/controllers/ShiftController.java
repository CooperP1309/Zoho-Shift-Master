package com.example.app.scheduler.controllers;

import com.example.app.scheduler.configs.ShiftTimes;
import com.example.app.scheduler.services.ShiftService;
import com.example.app.scheduler.services.ShiftService.SlotKey;
import com.example.app.scheduler.services.StaffService;

import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.util.Map;
import java.util.NoSuchElementException;

@RequestMapping("/shift/")
@Controller
@Slf4j
public class ShiftController 
{
    private final StaffService staffService;

    private final ShiftService shiftService;
    
    public ShiftController(StaffService staffService, ShiftService shiftService)
    {
        this.staffService = staffService;
        this.shiftService = shiftService;
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
     
        return "shift-page";
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
