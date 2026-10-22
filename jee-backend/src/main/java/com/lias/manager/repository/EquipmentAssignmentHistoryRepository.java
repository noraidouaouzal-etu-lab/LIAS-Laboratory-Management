package com.lias.manager.repository;

import com.lias.manager.entity.EquipmentAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentAssignmentHistoryRepository extends JpaRepository<EquipmentAssignmentHistory, Long> {
}
