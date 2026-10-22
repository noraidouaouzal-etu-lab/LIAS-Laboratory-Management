package com.lias.manager.repository;

import com.lias.manager.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {
}
