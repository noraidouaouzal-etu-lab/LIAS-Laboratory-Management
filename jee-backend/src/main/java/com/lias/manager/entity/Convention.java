package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "conventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String partner;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "convention_date")
    private LocalDate date;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "document_path", length = 255)
    private String documentPath;

    @Column(name = "activities_tracked", columnDefinition = "TEXT")
    private String activitiesTracked;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    public String getComputedStatus() {
        if (this.status == null || "PENDING".equalsIgnoreCase(this.status)) {
            return "PENDING";
        }
        if ("REJECTED".equalsIgnoreCase(this.status)) {
            return "REJECTED";
        }
        // "validated" means status was set to APPROVED
        if ("APPROVED".equalsIgnoreCase(this.status)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            if (this.endDate != null && now.isAfter(this.endDate)) {
                return "EXPIRED";
            }
            if ((this.startDate == null || !now.isBefore(this.startDate)) &&
                (this.endDate == null || !now.isAfter(this.endDate))) {
                return "ACTIVE";
            }
            return "ACTIVE";
        }
        return this.status;
    }
}
