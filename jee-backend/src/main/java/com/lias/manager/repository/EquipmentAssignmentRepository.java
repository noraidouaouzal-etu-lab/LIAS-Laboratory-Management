package com.lias.manager.repository;

import com.lias.manager.entity.EquipmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipmentAssignmentRepository extends JpaRepository<EquipmentAssignment, Long> {
    List<EquipmentAssignment> findByMemberId(Long memberId);
    List<EquipmentAssignment> findByEquipmentId(Long equipmentId);
}
