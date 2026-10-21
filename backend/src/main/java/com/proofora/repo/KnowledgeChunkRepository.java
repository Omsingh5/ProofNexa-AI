package com.proofora.repo;

import com.proofora.domain.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
}
