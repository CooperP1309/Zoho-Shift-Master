package com.example.app.scheduler.controllers;

import com.example.app.scheduler.services.StaffService;

import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/shift/")
@Controller
@Slf4j
public class ShiftController 
{
    private final StaffService staffService;
    
    public ShiftController(StaffService staffService)
    {
        this.staffService = staffService;
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
     
        return "shift-page";
    }
}