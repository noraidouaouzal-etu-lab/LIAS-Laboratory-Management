package com.lias.manager.controller;

import com.lias.manager.dto.RegisterDto;
import com.lias.manager.entity.Member;
import com.lias.manager.entity.Role;
import com.lias.manager.entity.User;
import com.lias.manager.repository.RoleRepository;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.repository.AdhesionRequestRepository;
import com.lias.manager.entity.AdhesionRequest;
import com.lias.manager.entity.Message;
import com.lias.manager.repository.MessageRepository;
import com.lias.manager.service.EventService;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.HashSet;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final TeamRepository teamRepository;
    private final EventService eventService;
    private final PublicationService publicationService;
    private final UserService userService;
    private final MemberService memberService;
    private final RoleRepository roleRepository;
    private final AdhesionRequestRepository adhesionRequestRepository;
    private final com.lias.manager.repository.MemberRepository memberRepository;
    private final com.lias.manager.repository.UserRepository userRepository;
    private final com.lias.manager.repository.MandateRepository mandateRepository;
    private final com.lias.manager.repository.NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final com.lias.manager.repository.MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final com.lias.manager.repository.RoleHistoryRepository roleHistoryRepository;
    private final com.lias.manager.repository.MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;
    private final com.lias.manager.service.FileStorageService fileStorageService;

    @GetMapping({"/adhesions/apply", "/adhesion-apply"})
    public String showAdhesionApply() {
        return "adhesion-apply";
    }

    @PostMapping({"/adhesions/apply", "/adhesion-apply"})
    @org.springframework.transaction.annotation.Transactional
    public String submitAdhesionApply(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam("cvFile") org.springframework.web.multipart.MultipartFile cvFile,
            @RequestParam String motivation,
            @RequestParam String institution,
            @RequestParam String originLab,
            @RequestParam(required = false) String requestedType,
            Model model) {
        
        if (cvFile == null || cvFile.isEmpty()) {
            model.addAttribute("error", "Le fichier CV est obligatoire.");
            return "adhesion-apply";
        }
        String filename = cvFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            model.addAttribute("error", "Seuls les fichiers PDF sont autorisés pour le CV.");
            return "adhesion-apply";
        }

        java.util.Optional<AdhesionRequest> existingOpt = adhesionRequestRepository.findByEmailIgnoreCase(email.trim());
        if (existingOpt.isPresent()) {
            AdhesionRequest existing = existingOpt.get();
            if ("ACCEPTED".equalsIgnoreCase(existing.getStatus())) {
                model.addAttribute("error", "Une demande d'adhésion a déjà été acceptée pour cet e-mail. Votre compte est actif.");
                return "adhesion-apply";
            } else if ("PENDING".equalsIgnoreCase(existing.getStatus())) {
                model.addAttribute("error", "Une demande d'adhésion en attente existe déjà pour cet e-mail.");
                return "adhesion-apply";
            } else {
                adhesionRequestRepository.delete(existing);
                adhesionRequestRepository.flush();
            }
        }

        String storedCvPath;
        try {
            storedCvPath = fileStorageService.storeFile(cvFile);
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'enregistrement du CV: " + e.getMessage());
            return "adhesion-apply";
        }

        String fullName = firstName.trim() + " " + lastName.trim();

        String finalMotivation = motivation.trim();
        if (requestedType != null && !requestedType.isBlank()) {
            finalMotivation = "[Statut demandé: " + requestedType + "] " + finalMotivation;
        }

        AdhesionRequest req = AdhesionRequest.builder()
                .name(fullName)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(email.trim().toLowerCase())
                .cv(storedCvPath)
                .motivation(finalMotivation)
                .institution(institution.trim())
                .originLab(originLab.trim())
                .requestedRole(requestedType != null ? requestedType.trim() : "DOCTORANT")
                .status("PENDING")
                .createdDate(LocalDateTime.now())
                .build();

        System.out.println("BEFORE SAVE New AdhesionRequest = " + req);
        AdhesionRequest saved = adhesionRequestRepository.saveAndFlush(req);
        System.out.println("SAVED = " + saved);

        try {
            com.lias.manager.entity.Notification notification = com.lias.manager.entity.Notification.builder()
                .message("Nouvelle demande d'adhésion soumise par " + fullName + " (" + email.trim() + ")")
                .type("NEW_ADHESION")
                .read(false)
                .date(LocalDateTime.now())
                .user("director@lias-lab.ma")
                .build();
            notificationRepository.saveAndFlush(notification);
        } catch (Exception e) {
            System.err.println("Failed to write notification: " + e.getMessage());
        }

        return "redirect:/adhesions/apply?success";
    }

    @GetMapping({"/adhesion-status", "/adhesion/status"})
    public String showAdhesionStatus(@RequestParam(required = false) String email, Model model) {
        if (email != null && !email.isBlank()) {
            java.util.Optional<AdhesionRequest> reqOpt = adhesionRequestRepository.findByEmailIgnoreCase(email.trim());
            if (reqOpt.isPresent()) {
                AdhesionRequest adhesionRequest = reqOpt.get();
                model.addAttribute("adhesionRequest", adhesionRequest);
                if ("ACCEPTED".equalsIgnoreCase(adhesionRequest.getStatus())) {
                    java.util.List<com.lias.manager.entity.Notification> notifications = notificationRepository.findByUserAndType(adhesionRequest.getEmail(), "ADHESION_ACCEPTED");
                    if (!notifications.isEmpty()) {
                        model.addAttribute("adhesionNotification", notifications.get(notifications.size() - 1));
                    }
                }
                model.addAttribute("searched", true);
            } else {
                model.addAttribute("error", "Aucune demande d'adhésion trouvée pour cette adresse e-mail.");
                model.addAttribute("searched", true);
            }
            model.addAttribute("email", email);
        }
        return "adhesion-status";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        return "redirect:/adhesion-apply";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registerDto") RegisterDto registerDto, Model model) {
        return "redirect:/adhesion-apply";
    }

    @GetMapping({"/", "/home"})
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String home(Model model) {
        model.addAttribute("teams", teamRepository.findAll());
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("publications", publicationService.findAll());

        long teamCount = teamRepository.count();
        long publicationCount = publicationService.findAll().size();
        long memberCount = memberRepository.countByStatusIn(java.util.List.of("PERMANENT", "ASSOCIATE", "DOCTORANT"));

        model.addAttribute("teamCount", teamCount);
        model.addAttribute("publicationCount", publicationCount);
        model.addAttribute("memberCount", memberCount);

        return "home";
    }

    @GetMapping("/teams")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String teams(Model model) {
        java.util.List<com.lias.manager.entity.Team> teams = teamRepository.findAll();
        java.util.List<com.lias.manager.entity.Member> allMembers = memberRepository.findAll();
        java.util.List<com.lias.manager.entity.Mandate> allMandates = mandateRepository.findAll();
        
        java.util.Map<Long, Long> memberCounts = new java.util.HashMap<>();
        java.util.Map<Long, String> responsibles = new java.util.HashMap<>();
        
        for (com.lias.manager.entity.Team team : teams) {
            long count = allMembers.stream()
                .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(team.getId()))
                .count();
            memberCounts.put(team.getId(), count);
            
            String respName = allMandates.stream()
                .filter(m -> m.isActive() && m.getRoleTitle() != null && 
                    ("TEAM_LEADER".equalsIgnoreCase(m.getRoleTitle()) || 
                     "Team Leader".equalsIgnoreCase(m.getRoleTitle()) || 
                     "TEAM_LEADER".equalsIgnoreCase(m.getRoleTitle().replace(" ", "_").replace("-", "_"))))
                .filter(m -> {
                    if (team.getName().equalsIgnoreCase(m.getTeamAssigned())) {
                        return true;
                    }
                    if (m.getMember() != null && m.getMember().getTeam() != null && m.getMember().getTeam().getId().equals(team.getId())) {
                        return true;
                    }
                    return false;
                })
                .map(m -> "Pr. " + m.getMember().getFirstName() + " " + m.getMember().getLastName())
                .findFirst()
                .orElse(null);
                
            if (respName == null) {
                com.lias.manager.entity.Member perm = allMembers.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(team.getId()))
                    .filter(m -> "PERMANENT".equalsIgnoreCase(m.getStatus()))
                    .findFirst()
                    .orElse(null);
                if (perm != null) {
                    respName = "Pr. " + perm.getFirstName() + " " + perm.getLastName();
                } else {
                    respName = "Non renseigné";
                }
            }
            responsibles.put(team.getId(), respName);
        }
        
        model.addAttribute("teams", teams);
        model.addAttribute("memberCounts", memberCounts);
        model.addAttribute("responsibles", responsibles);
        return "teams";
    }

    @GetMapping("/members")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String publicMembers(Model model) {
        java.util.List<com.lias.manager.entity.Member> allMembers = memberRepository.findAll();
        java.util.List<com.lias.manager.entity.Mandate> allMandates = mandateRepository.findAll();
        
        com.lias.manager.entity.Member director = allMandates.stream()
            .filter(m -> m.isActive() && m.getRoleTitle() != null && 
                ("DIRECTOR".equalsIgnoreCase(m.getRoleTitle()) || 
                 "Director".equalsIgnoreCase(m.getRoleTitle()) || 
                 "DIRECTOR".equalsIgnoreCase(m.getRoleTitle().replace(" ", "_").replace("-", "_"))))
            .map(m -> m.getMember())
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
        
        if (director == null) {
            com.lias.manager.entity.User directorUser = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_DIRECTOR".equals(r.getName())))
                .findFirst()
                .orElse(null);
            if (directorUser != null) {
                final String dEmail = directorUser.getEmail();
                director = allMembers.stream()
                    .filter(m -> dEmail.equalsIgnoreCase(m.getEmail()))
                    .findFirst()
                    .orElse(null);
            }
        }
        
        final com.lias.manager.entity.Member finalDirector = director;
        
        java.util.List<com.lias.manager.entity.Member> permanents = allMembers.stream()
            .filter(m -> "PERMANENT".equalsIgnoreCase(m.getStatus()))
            .filter(m -> finalDirector == null || !m.getId().equals(finalDirector.getId()))
            .collect(java.util.stream.Collectors.toList());
            
        java.util.List<com.lias.manager.entity.Member> associates = allMembers.stream()
            .filter(m -> "ASSOCIATE".equalsIgnoreCase(m.getStatus()))
            .collect(java.util.stream.Collectors.toList());
            
        java.util.List<com.lias.manager.entity.Member> doctorants = allMembers.stream()
            .filter(m -> "DOCTORANT".equalsIgnoreCase(m.getStatus()))
            .collect(java.util.stream.Collectors.toList());
            
        model.addAttribute("director", director);
        model.addAttribute("permanents", permanents);
        model.addAttribute("associates", associates);
        model.addAttribute("doctorants", doctorants);
        model.addAttribute("allMembers", allMembers);
        
        return "public-members";
    }

    @GetMapping({"/events", "/public-events"})
    public String publicEvents(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "public-events";
    }

    @GetMapping({"/publications", "/public-publications"})
    public String publicPublications(Model model) {
        model.addAttribute("publications", publicationService.findAll());
        model.addAttribute("teams", teamRepository.findAll());
        return "public-publications";
    }



    private void enrichMessage(Message msg) {
        if (msg == null) return;
        String senderEmailOrUser = msg.getSender();
        if (senderEmailOrUser == null || senderEmailOrUser.trim().isEmpty()) {
            msg.setAuthorFullName("Anonyme");
            msg.setAuthorRole("Visiteur");
            return;
        }

        // Try to find User to get the correct email
        String email = senderEmailOrUser;
        var userOpt = userRepository.findByEmail(senderEmailOrUser);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(senderEmailOrUser);
        }
        
        String resolvedRole = "Membre";
        if (userOpt.isPresent()) {
            email = userOpt.get().getEmail();
            var roles = userOpt.get().getRoles();
            boolean isDir = roles.stream().anyMatch(r -> "ROLE_DIRECTOR".equalsIgnoreCase(r.getName()));
            boolean isAdmin = roles.stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
            if (isDir) {
                resolvedRole = "Directeur";
            } else if (isAdmin) {
                resolvedRole = "Administrateur";
            }
        }

        // Try to find Member profile
        var memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) {
            Member m = memberOpt.get();
            msg.setAuthorFullName(m.getFirstName() + " " + m.getLastName());
            if ("Membre".equals(resolvedRole)) {
                if ("PERMANENT".equalsIgnoreCase(m.getStatus())) {
                    resolvedRole = "Membre Permanent";
                } else if ("DOCTORANT".equalsIgnoreCase(m.getStatus())) {
                    resolvedRole = "Doctorant";
                } else if ("ASSOCIATE".equalsIgnoreCase(m.getStatus())) {
                    resolvedRole = "Membre Associé";
                } else if ("RETIRED".equalsIgnoreCase(m.getStatus())) {
                    resolvedRole = "Membre Retraité";
                } else if ("FORMER_MEMBER".equalsIgnoreCase(m.getStatus())) {
                    resolvedRole = "Ancien Membre";
                }
            }
        } else {
            // No member profile found, use username / email parts
            if (userOpt.isPresent()) {
                String uname = userOpt.get().getUsername();
                msg.setAuthorFullName(uname.substring(0, 1).toUpperCase() + uname.substring(1));
            } else {
                int atIdx = senderEmailOrUser.indexOf('@');
                String namePart = atIdx > 0 ? senderEmailOrUser.substring(0, atIdx) : senderEmailOrUser;
                msg.setAuthorFullName(namePart.substring(0, 1).toUpperCase() + namePart.substring(1));
            }
        }
        msg.setAuthorRole(resolvedRole);
    }

    @GetMapping("/communication")
    public String communication(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        List<Message> allMessages = messageRepository.findAll();
        
        // Enrich
        for (Message msg : allMessages) {
            enrichMessage(msg);
        }

        // Order: newest messages first (Newest -> Oldest)
        List<Message> filtered = allMessages.stream()
            .sorted((m1, m2) -> {
                LocalDateTime d1 = m1.getDate() != null ? m1.getDate() : LocalDateTime.MIN;
                LocalDateTime d2 = m2.getDate() != null ? m2.getDate() : LocalDateTime.MIN;
                return d2.compareTo(d1);
            })
            .collect(java.util.stream.Collectors.toList());

        // Apply filters
        if (author != null && !author.trim().isEmpty()) {
            String authorLower = author.trim().toLowerCase();
            filtered = filtered.stream()
                .filter(m -> (m.getAuthorFullName() != null && m.getAuthorFullName().toLowerCase().contains(authorLower)) ||
                             (m.getSender() != null && m.getSender().toLowerCase().contains(authorLower)))
                .collect(java.util.stream.Collectors.toList());
        }

        if (channel != null && !channel.trim().isEmpty()) {
            String channelLower = channel.trim().toLowerCase();
            filtered = filtered.stream()
                .filter(m -> (m.getChannelType() != null && m.getChannelType().toLowerCase().contains(channelLower)) ||
                             (m.getChannelName() != null && m.getChannelName().toLowerCase().contains(channelLower)))
                .collect(java.util.stream.Collectors.toList());
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                .filter(m -> m.getContent() != null && m.getContent().toLowerCase().contains(keywordLower))
                .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("messages", filtered);
        return "communication";
    }

    @PostMapping("/communication/send")
    @org.springframework.transaction.annotation.Transactional
    public String sendCommunication(
            @RequestParam String channelType,
            @RequestParam String channelName,
            @RequestParam String content) {
        String loginName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var userOpt = userRepository.findByEmail(loginName);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(loginName);
        }
        String senderEmail = userOpt.isPresent() ? userOpt.get().getEmail() : loginName;
        
        Message msg = Message.builder()
                .sender(senderEmail)
                .content(content.trim())
                .channelType(channelType.trim().toUpperCase())
                .channelName(channelName.trim())
                .date(LocalDateTime.now())
                .build();
        messageRepository.saveAndFlush(msg);
        return "redirect:/communication?success";
    }
}
