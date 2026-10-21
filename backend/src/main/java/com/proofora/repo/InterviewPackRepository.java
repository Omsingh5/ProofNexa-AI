package com.proofora.repo;

import com.proofora.domain.InterviewPack;
import com.proofora.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewPackRepository extends JpaRepository<InterviewPack, Long> {
    List<InterviewPack> findByJobOrderByCreatedAtDesc(Job job);
}
