package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "date_upload", nullable = false)
    @Builder.Default
    private LocalDateTime dateUpload = LocalDateTime.now();

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "upload_date")
    @Builder.Default
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

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
