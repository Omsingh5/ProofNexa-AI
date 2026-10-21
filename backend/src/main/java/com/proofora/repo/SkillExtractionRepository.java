package com.proofora.repo;

import com.proofora.domain.SkillExtraction;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillExtractionRepository extends JpaRepository<SkillExtraction, Long> {
    Optional<SkillExtraction> findFirstByCandidateOrderByUpdatedAtDesc(User candidate);
}
