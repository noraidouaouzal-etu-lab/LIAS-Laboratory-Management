package com.lias.manager.controller;

import com.lias.manager.entity.Member;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.MemberProfileHistoryRepository;
import com.lias.manager.repository.RoleHistoryRepository;
import com.lias.manager.repository.MemberAffiliationHistoryRepository;
import com.lias.manager.service.UserService;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final TeamRepository teamRepository;
    private final UserService userService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final RoleHistoryRepository roleHistoryRepository;
    private final MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;
    private final PublicationService publicationService;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String profile(Model model) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }
        String email = auth.getName();
        var userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            userOpt = userService.findByUsername(email);
        }
        if (userOpt.isPresent()) {
            email = userOpt.get().getEmail();
        }

        String finalEmail = email;
        Member member = memberRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    String first = "";
                    String last = "";
                    if (finalEmail != null && finalEmail.contains("@")) {
                        String prefix = finalEmail.split("@")[0];
                        if (prefix.contains(".")) {
                            String[] parts = prefix.split("\\.");
                            first = parts[0].substring(0, 1).toUpperCase() + (parts[0].length() > 1 ? parts[0].substring(1) : "");
                            last = parts[1].substring(0, 1).toUpperCase() + (parts[1].length() > 1 ? parts[1].substring(1) : "");
                        } else {
                            first = prefix.substring(0, 1).toUpperCase() + (prefix.length() > 1 ? prefix.substring(1) : "");
                            last = "Chercheur";
                        }
                    } else {
                        first = "Membre";
                        last = "LIAS";
                    }

                    Member fallback = Member.builder()
                            .firstName(first)
                            .lastName(last)
                            .email(finalEmail)
                            .status("PERMANENT")
                            .institution(null)
                            .originLab(null)
                            .biography(null)
                            .interests(null)
                            .build();
                    return memberRepository.saveAndFlush(fallback);
                });

        model.addAttribute("member", member);
        if (member != null && member.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(member.getId()));
            model.addAttribute("roleHistory", roleHistoryRepository.findByMemberId(member.getId()));
            model.addAttribute("affiliationHistory", memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(member.getId()));
            
            // Retrieve member's publications
            String ln = member.getLastName() != null ? member.getLastName().toLowerCase() : "";
            String fn = member.getFirstName() != null ? member.getFirstName().toLowerCase() : "";
            java.util.List<com.lias.manager.entity.Publication> myPubs = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .toList();
            model.addAttribute("myPublications", myPubs);
        } else {
            model.addAttribute("myPublications", java.util.List.of());
        }
        model.addAttribute("teams", teamRepository.findAll());
        return "profile";
    }

    @PostMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
    public String updateProfile(
            @ModelAttribute Member edited,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        String email = auth.getName();
        var userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            userOpt = userService.findByUsername(email);
        }
        if (userOpt.isPresent()) {
            email = userOpt.get().getEmail();
        }

        final String finalEmail = email;
        Member existing = memberRepository.findByEmail(finalEmail)
                .orElseThrow(() -> new RuntimeException("Membre introuvable."));

        boolean isAssociate = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ASSOCIATE"));
        if (isAssociate || "ASSOCIATE".equals(existing.getStatus())) {
            if (photoFile != null && !photoFile.isEmpty()) {
                String savedPath = fileStorageService.storeFile(photoFile);
                existing.setPhoto(savedPath);
            }
            existing.setBiography(edited.getBiography());
            existing.setInterests(edited.getInterests());
            memberRepository.saveAndFlush(existing);
            return "redirect:/associate/profile?success";
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            String savedPath = fileStorageService.storeFile(photoFile);
            edited.setPhoto(savedPath);
        } else {
            edited.setPhoto(existing.getPhoto());
        }

        memberService.updateMemberProfile(finalEmail, edited);
        return "redirect:/profile?success";
    }

    @GetMapping("/uploads/{filename:.+}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> serveUpload(@PathVariable String filename) {
        try {
            Path path = Paths.get("/uploads").resolve(filename).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
                String contentType = Files.probeContentType(path);
                if (contentType == null) {
                    contentType = "image/jpeg";
                }
                return org.springframework.http.ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .body(resource);
            }
        } catch (Exception e) {
            // Ignore
        }
        try {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(URI.create("/images/default-avatar.png"))
                    .build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile/photo/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getProfilePhoto(@PathVariable Long id) {
        var memberOpt = memberRepository.findById(id);
        if (memberOpt.isPresent()) {
            Member m = memberOpt.get();
            String diskPath = m.getPhoto();
            if (diskPath != null && !diskPath.trim().isEmpty()) {
                try {
                    Path path = Paths.get(diskPath).toAbsolutePath().normalize();
                    if (Files.exists(path)) {
                        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) {
                            contentType = "image/jpeg";
                        }
                        return org.springframework.http.ResponseEntity.ok()
                                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                                .body(resource);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        try {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(URI.create("/images/default-avatar.png"))
                    .build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }
}
