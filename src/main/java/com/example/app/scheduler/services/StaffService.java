package com.example.app.scheduler.services;

import com.example.app.scheduler.entities.Staff;
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

    public StaffService(StaffRepository staffRepository)
    {
        this.staffRepository = staffRepository;
    }

    public List<Staff> allStaff() 
    {
        List<Staff> staff = new ArrayList<>();

        staffRepository.findAll().forEach(staff::add);

        return staff;
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
        
        staffRepository.delete(staff);
    }
}