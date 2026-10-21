package com.proofora.repo;

import com.proofora.domain.TechnicalChallenge;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicalChallengeRepository extends JpaRepository<TechnicalChallenge, Long> {
    List<TechnicalChallenge> findByCandidateOrderByCreatedAtDesc(User candidate);
}
