package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // UPLOADED, DELETED

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;
}
