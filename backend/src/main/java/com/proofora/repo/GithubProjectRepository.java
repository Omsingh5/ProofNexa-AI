package com.proofora.repo;

import com.proofora.domain.GithubProject;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GithubProjectRepository extends JpaRepository<GithubProject, Long> {
    List<GithubProject> findByCandidateOrderByIdDesc(User candidate);
}
