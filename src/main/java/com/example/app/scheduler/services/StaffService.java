package com.example.app.scheduler.services;

import com.example.app.scheduler.entities.ShiftSlot;
import com.example.app.scheduler.entities.Staff;
import com.example.app.scheduler.repositories.ShiftSlotRepository;
import com.example.app.scheduler.repositories.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StaffService 
{
    private final StaffRepository staffRepository;
    private final ShiftSlotRepository shiftSlotRepository;

    public StaffService(StaffRepository staffRepository, ShiftSlotRepository shiftSlotRepository)
    {
        this.staffRepository = staffRepository;
        this.shiftSlotRepository = shiftSlotRepository;
    }

    public List<Staff> allStaff() 
    {
        List<Staff> staff = new ArrayList<>();

        staffRepository.findAll().forEach(staff::add);

        return staff;
    }

    public Staff findStaffById(Integer staffID)
    {
        return staffRepository.findById(staffID)
                .orElseThrow(() -> new NoSuchElementException("No staff found with ID: " + staffID));
    }

    @Transactional
    public Staff createStaff(Staff staff) 
    {
        return staffRepository.save(staff);
    }

    @Transactional
    public void deleteStaff(Integer staffID) 
    {
        Staff staff = staffRepository.findById(staffID)
                .orElseThrow(() -> new NoSuchElementException("No staff found with ID: " + staffID));

        // Unassign any slots this staff member holds first, otherwise
        // the shift_slots.staff_id foreign key blocks the delete
        List<ShiftSlot> slots = shiftSlotRepository.findAllByStaff(staff);
        slots.forEach(slot -> slot.setStaff(null));
        shiftSlotRepository.saveAll(slots);

        staffRepository.delete(staff);
    }
}