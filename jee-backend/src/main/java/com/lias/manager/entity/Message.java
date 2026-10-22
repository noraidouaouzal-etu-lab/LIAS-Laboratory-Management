package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String sender; // email or name of the sender

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, name = "message_date")
    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();

    @Column(nullable = false, length = 50, name = "channel_type")
    private String channelType; // GLOBAL, TEAM, EVENT

    @Column(length = 150, name = "channel_name")
    private String channelName; // Name of the team or event title

    @Transient
    private String authorFullName;

    @Transient
    private String authorRole;
}
