package com.proofora.web;

import com.proofora.domain.AuditLog;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.repo.AuditLogRepository;
import com.proofora.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final AuditLogRepository logs;

    public AdminController(UserRepository users, AuditLogRepository logs) {
        this.users = users;
        this.logs = logs;
    }

    @GetMapping("/users")
    public Dtos.PageResponse<Dtos.UserView> users(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Page<User> result = users.findAll(PageRequest.of(page, size));
        return new Dtos.PageResponse<>(
                result.map(Dtos.UserView::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/audit")
    public Dtos.PageResponse<AuditLog> audit(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> result = logs.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new Dtos.PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
