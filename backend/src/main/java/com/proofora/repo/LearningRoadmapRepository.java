package com.proofora.repo;

import com.proofora.domain.LearningRoadmap;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, Long> {
    List<LearningRoadmap> findByCandidateOrderByCreatedAtDesc(User candidate);
}
