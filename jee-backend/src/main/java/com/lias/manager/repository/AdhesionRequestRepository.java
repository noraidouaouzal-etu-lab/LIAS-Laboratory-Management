package com.lias.manager.repository;

import com.lias.manager.entity.AdhesionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdhesionRequestRepository extends JpaRepository<AdhesionRequest, Long> {
    java.util.List<AdhesionRequest> findByStatus(String status);
    java.util.Optional<AdhesionRequest> findByEmailIgnoreCase(String email);
    long countByStatus(String status);
}
