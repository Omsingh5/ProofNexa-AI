package com.proofora.repo;

import com.proofora.domain.ApplicationEvent;
import com.proofora.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {
    List<ApplicationEvent> findByApplicationOrderByCreatedAtAsc(JobApplication application);
}
