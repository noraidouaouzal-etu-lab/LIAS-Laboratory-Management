package com.lias.manager.controller;

import com.lias.manager.entity.Document;
import com.lias.manager.entity.PVMeeting;
import com.lias.manager.entity.Convention;
import com.lias.manager.repository.DocumentRepository;
import com.lias.manager.repository.PVMeetingRepository;
import com.lias.manager.repository.ConventionRepository;
import com.lias.manager.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;

@Controller
@RequiredArgsConstructor
public class DownloadController {

    private final DocumentRepository documentRepository;
    private final PVMeetingRepository pvMeetingRepository;
    private final FileStorageService fileStorageService;
    private final ConventionRepository conventionRepository;
    private final com.lias.manager.repository.AdhesionRequestRepository adhesionRequestRepository;

    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        String filePathString = doc.getFilePath();
        if (filePathString == null || filePathString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = fileStorageService.loadFileAsPath(filePathString);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    contentType = java.nio.file.Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                } catch (Exception e) {
                    // fall back
                }

                String attachmentName = doc.getFileName();
                if (attachmentName == null || attachmentName.isEmpty()) {
                    attachmentName = doc.getName();
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pv/download/{id}")
    public ResponseEntity<Resource> downloadPV(@PathVariable Long id) {
        PVMeeting pv = pvMeetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PV meeting not found with id: " + id));

        String filePathString = pv.getFilePath();
        if (filePathString == null || filePathString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = fileStorageService.loadFileAsPath(filePathString);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    contentType = java.nio.file.Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                } catch (Exception e) {
                    // fall back
                }

                String attachmentName = pv.getFileName();
                if (attachmentName == null || attachmentName.isEmpty()) {
                    attachmentName = pv.getTitle() + ".pdf";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/conventions/download/{id}")
    public ResponseEntity<Resource> downloadConvention(@PathVariable Long id) {
        Convention cov = conventionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention not found with id: " + id));

        String filePathString = cov.getDocumentPath();
        if (filePathString == null || filePathString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = fileStorageService.loadFileAsPath(filePathString);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    contentType = java.nio.file.Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                } catch (Exception e) {
                    // fall back
                }

                String attachmentName = "convention-" + cov.getPartner().toLowerCase().replaceAll("[^a-z0-9]", "-") + ".pdf";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/director/adhesions/download-cv/{id}")
    public ResponseEntity<Resource> downloadAdhesionCV(@PathVariable Long id) {
        com.lias.manager.entity.AdhesionRequest req = adhesionRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adhesion request not found with id: " + id));

        String filePathString = req.getCv();
        if (filePathString == null || filePathString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = fileStorageService.loadFileAsPath(filePathString);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/pdf";
                String firstNamePart = req.getFirstName() != null ? req.getFirstName() : "first";
                String lastNamePart = req.getLastName() != null ? req.getLastName() : "last";
                String attachmentName = "cv-" + firstNamePart.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + lastNamePart.toLowerCase().replaceAll("[^a-z0-9]", "-") + ".pdf";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
