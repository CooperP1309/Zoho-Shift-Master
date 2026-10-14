package com.anticipate.listr.scheduler.repositories;

import com.anticipate.listr.scheduler.entities.StationMaster;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationMasterRepository extends CrudRepository<StationMaster, Integer>
{
}
