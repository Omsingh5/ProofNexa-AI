package com.proofora.repo;

import com.proofora.domain.AssistantMessage;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, Long> {
    List<AssistantMessage> findByUserOrderByCreatedAtAsc(User user);
}
