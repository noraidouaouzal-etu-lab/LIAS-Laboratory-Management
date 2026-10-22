package com.lias.manager.repository;

import com.lias.manager.entity.PVMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PVMeetingRepository extends JpaRepository<PVMeeting, Long> {
}
