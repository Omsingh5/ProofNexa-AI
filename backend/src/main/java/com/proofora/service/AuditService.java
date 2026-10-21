package com.proofora.service;

import com.proofora.domain.AuditLog;
import com.proofora.domain.User;
import com.proofora.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    public void record(User actor, String action, String entityType, Long entityId, String detail) {
        AuditLog log = new AuditLog();
        log.setActorId(actor == null ? null : actor.getId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetail(detail);
        logs.save(log);
    }
}
