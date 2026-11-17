package com.example.app.scheduler.repositories;

import com.example.app.scheduler.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<Staff, Integer> 
{
}