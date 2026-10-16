package com.anticipate.listr.authentication.controllers;

/* ===== local libs ===== */
import com.anticipate.listr.authentication.services.UserService;
import com.anticipate.listr.authentication.services.AuthenticationService;
import com.anticipate.listr.authentication.dtos.SetAccountEnabledDto;
import com.anticipate.listr.scheduler.ShiftTimes;
import com.anticipate.listr.scheduler.dtos.AddStationMasterDto;
import com.anticipate.listr.scheduler.dtos.AssignShiftSlotDto;
import com.anticipate.listr.scheduler.services.RosterService;
import com.anticipate.listr.scheduler.services.ShiftSchedulerService;

/* ===== spring libs ===== */
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.stereotype.Controller;

/* ===== java libs =====*/
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

/*  Defense-in-depth: every method here is also gated by the /admin/**
 *  rules in SecurityConfiguration, but those are URL-pattern checks.
 *  These @PreAuthorize annotations are a second, independent mechanism
 *  (an AOP proxy checking the authenticated principal directly) that
 *  still holds even if the routing-level rules are ever misconfigured -
 *  exactly the class of mistake that publicly exposed setAccountEnabled()
 *  before it was moved onto this controller.
 *
 *  Standard users can use the console (roster, station masters, send
 *  now). User account management is admin only, enforced per method
 *  below (a method-level @PreAuthorize overrides the class-level one).
 */
@RequestMapping("/admin")
@Controller
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class AdminController
{
    private final UserService userService;

    private final AuthenticationService authenticationService;

    private final RosterService rosterService;

    private final ShiftSchedulerService shiftSchedulerService;

    public AdminController(UserService userService, AuthenticationService authenticationService,
                           RosterService rosterService, ShiftSchedulerService shiftSchedulerService)
    {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.rosterService = rosterService;
        this.shiftSchedulerService = shiftSchedulerService;
    }

    @PostMapping("/delete-user/{userID}")
    @PreAuthorize("hasRole('ADMIN')")
    /*  Deletes a user by ID
     *
     *  This endpoint interafaces with the user repository
     *  through the UserService module to delete a user by
     *  their ID. Bound as a urlencoded form post from a
     *  per-row form on the dashboard (plain HTML forms can't
     *  submit DELETE), redirecting back once done.
     */
    public String deleteUser(@PathVariable Integer userID, Authentication authentication)
    {
        try
        {
            userService.deleteUser(userID);
            log.info("User with ID {} deleted by {}", userID, authentication.getName());
        }
        catch (NoSuchElementException e)
        {
            log.warn("{} attempted to delete missing user with ID {}", authentication.getName(), userID);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/set-account-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    /*  Enables or disables a user account
     *
     *  A user account is considered "enabled" once its email is
     *  verified (see User.isEnabled()). This endpoint lets an admin
     *  flip that flag directly from the dashboard, toggling the
     *  target user's ability to log in. Bound as a urlencoded form
     *  post from a per-row form on the dashboard, redirecting back
     *  once done.
     */
    public String setAccountEnabled(@ModelAttribute SetAccountEnabledDto input, Authentication authentication)
    {
        if (input.isEnabled()) {
            authenticationService.setEmailAsVerified(input.getEmail());
        } else {
            authenticationService.setEmailAsNotVerified(input.getEmail());
        }

        log.info("Account {} {} by {}", input.getEmail(), input.isEnabled() ? "enabled" : "disabled", authentication.getName());

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/station-masters")
    /*  Adds a station master
     *
     *  Registers a person who can be put on the roster, along
     *  with the Cliq user ID used to @mention them.
     */
    public String addStationMaster(@ModelAttribute AddStationMasterDto input, Authentication authentication)
    {
        if (input.getDisplayName() == null || input.getDisplayName().isBlank()
                || input.getCliqUserId() == null || input.getCliqUserId().isBlank()) {
            log.warn("{} attempted to add station master with missing name or Cliq user ID", authentication.getName());
            return "redirect:/admin/dashboard";
        }

        rosterService.addStationMaster(input.getDisplayName(), input.getCliqUserId());
        log.info("Station master {} (Cliq user ID {}) added by {}", input.getDisplayName(), input.getCliqUserId(), authentication.getName());

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/station-masters/{stationMasterID}/delete")
    /*  Deletes a station master by ID
     *
     *  Also unassigns them from any roster slots they were on.
     */
    public String deleteStationMaster(@PathVariable Integer stationMasterID, Authentication authentication)
    {
        try
        {
            rosterService.deleteStationMaster(stationMasterID);
            log.info("Station master with ID {} deleted by {}", stationMasterID, authentication.getName());
        }
        catch (NoSuchElementException e)
        {
            log.warn("{} attempted to delete missing station master with ID {}", authentication.getName(), stationMasterID);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/shift-slots")
    /*  Assigns a station master to a roster slot
     *
     *  Bound from a per-cell form on the roster grid. A blank
     *  stationMasterId unassigns the slot.
     */
    public String assignShiftSlot(@ModelAttribute AssignShiftSlotDto input, Authentication authentication)
    {
        try
        {
            rosterService.assignSlot(input.getDayOfWeek(), input.getStartTime(), input.getStationMasterId());
            log.info("Slot {} {} assigned to station master ID {} by {}",
                    input.getDayOfWeek(), input.getStartTime(), input.getStationMasterId(), authentication.getName());
        }
        catch (NoSuchElementException | IllegalArgumentException e)
        {
            log.warn("{} failed to assign slot: {}", authentication.getName(), e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/scheduler/trigger-now")
    /*  Runs the shift announcement immediately
     *
     *  Same as what the scheduler does at each shift change,
     *  for testing without waiting for the cron to fire.
     */
    public String triggerShiftAnnouncement(Authentication authentication)
    {
        log.info("Shift announcement manually triggered by {}", authentication.getName());
        shiftSchedulerService.announceCurrentShift();

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    /*  Returns the admin dashboard
     *
     *  Returns the html/web UI for graphically managing the
     *  system. The user list is only loaded for admins, since
     *  standard users can't manage accounts.
     */
    public String getDashboard(Model model, Authentication authentication)
    {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            model.addAttribute("users", userService.allUsers());
        }

        model.addAttribute("stationMasters", rosterService.allStationMasters());
        model.addAttribute("rosterDays", ShiftTimes.DAYS);
        model.addAttribute("rosterRows", rosterService.getWeekGrid());

        return "admin-page";
    }
}