package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "pv_meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PVMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String writer; // Rédacteur (nom du membre rédigeant)

    @Column(nullable = false, length = 100)
    private String type; // Conseil de Labo, Réunion d'équipe, Réunion thématique

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String agenda;

    @Column(columnDefinition = "TEXT")
    private String decisions; // Décisions prises lors de la réunion

    @Column(nullable = false, name = "meeting_date")
    private LocalDate date;

    @Column(name = "file_path", length = 255)
    private String file;

    @Column(name = "upload_file_name", length = 255)
    private String fileName;

    @Column(name = "upload_file_path", length = 500)
    private String filePath;

    @Column(name = "upload_date")
    private java.time.LocalDateTime uploadDate;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    public boolean isArchived() {
        return this.archived;
    }

    public boolean getArchived() {
        return this.archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
