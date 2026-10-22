package com.lias.manager.repository;

import com.lias.manager.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelTypeOrderByDateDesc(String channelType);
    List<Message> findByChannelTypeAndChannelNameOrderByDateDesc(String channelType, String channelName);
}
