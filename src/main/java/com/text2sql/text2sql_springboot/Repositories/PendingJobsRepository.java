package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.PendingJobs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PendingJobsRepository extends JpaRepository<PendingJobs, UUID> {
}
