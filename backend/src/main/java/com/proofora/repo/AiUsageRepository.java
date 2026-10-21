package com.proofora.repo;

import com.proofora.domain.AiUsage;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {
    Optional<AiUsage> findByUserAndUsageDate(User user, LocalDate usageDate);
}
