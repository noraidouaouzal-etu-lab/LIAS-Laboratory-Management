package com.lias.manager.repository;

import com.lias.manager.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    java.util.List<Notification> findByUserAndType(String user, String type);
}
